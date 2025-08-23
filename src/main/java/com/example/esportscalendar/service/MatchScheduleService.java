package com.example.esportscalendar.service;

import com.example.esportscalendar.domain.MatchSchedule;
import com.example.esportscalendar.repository.MatchScheduleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.lang.reflect.Method;
import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MatchScheduleService {

    private final MatchScheduleRepository matchScheduleRepository;
    private final WebClient defaultClient;

    public MatchScheduleService(MatchScheduleRepository matchScheduleRepository) {
        this.matchScheduleRepository = matchScheduleRepository;
        try {
            this.defaultClient = createDefaultClient();
        } catch (SSLException e) {
            throw new RuntimeException("WebClient 생성 실패", e);
        }
    }

    private WebClient createDefaultClient() throws SSLException {
        HttpClient httpClient = HttpClient.create()
                .secure(t -> {
                    try {
                        t.sslContext(
                                SslContextBuilder.forClient()
                                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                        .build()
                        );
                    } catch (SSLException e) {
                        throw new RuntimeException(e);
                    }
                });

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .build();
    }

    // ===========================
    // 네이버 month API 기반 크롤링 (matches만)
    // ===========================
    /**
     * NAVER month API로 LCK 일정 크롤링 & 저장
     * - 입력: yyyy-MM-dd ~ yyyy-MM-dd
     * - 처리: 월(YearMonth) 단위로 슬라이딩 호출
     * - 중복 방지: (teamA, teamB, matchDate) exists 체크
     */
    public int crawlAndSaveLckSchedule(String startDate, String endDate) throws Exception {
        LocalDate s = LocalDate.parse(startDate);
        LocalDate e = LocalDate.parse(endDate);
        if (e.isBefore(s)) throw new IllegalArgumentException("endDate가 startDate보다 빠릅니다.");

        WebClient naverClient = this.defaultClient.mutate()
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .defaultHeader("Referer", "https://sports.naver.com/esports/schedule/League_of_Legends")
                .build();

        YearMonth cur = YearMonth.from(s);
        YearMonth last = YearMonth.from(e);

        int totalSaved = 0;
        while (!cur.isAfter(last)) {
            totalSaved += fetchMonthAndSaveFromNaver(naverClient, cur);
            try { Thread.sleep(150L); } catch (InterruptedException ignore) {}
            cur = cur.plusMonths(1);
        }
        System.out.println("[NAVER DONE] saved=" + totalSaved + " (" + s + " ~ " + e + ")");
        return totalSaved;
    }

    /**
     * 한 달치 불러와 저장 (content.matches 전용)
     */
    private int fetchMonthAndSaveFromNaver(WebClient wc, YearMonth ym) throws Exception {
        String url = "https://esports-api.game.naver.com/service/v2/schedule/month"
                + "?month=" + ym + "&topLeagueId=lck&relay=false";
        System.out.println("[NAVER FETCH] " + url);

        String json = wc.get().uri(url).retrieve().bodyToMono(String.class).block();
        if (json == null || json.isBlank()) return 0;

        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree(json);

        JsonNode content = root.path("content");
        if (content.isMissingNode() || content.isNull()) {
            System.out.println("[NAVER WARN] content 없음");
            return 0;
        }

        // 팀 맵 구성 (teamId -> 이름)
        Map<String, String> teamNameById = new HashMap<>();
        for (JsonNode t : content.path("teams")) {
            String id = t.path("teamId").asText(null);
            if (id == null || id.isBlank()) continue;
            String name = firstNonBlank(t, "name", "nameEng", "nameAcronym", "teamName", "teamNameEng");
            if (name != null) teamNameById.put(id, name);
        }

        // 최신 응답: content.matches
        JsonNode matchesTop = content.path("matches");
        if (!matchesTop.isArray() || matchesTop.size() == 0) {
            System.out.println("[NAVER WARN] matches 배열 없음");
            return 0;
        }

        int saved = 0, dup = 0, fail = 0;

        for (JsonNode m : matchesTop) {
            try {
                // 시간: startDate(epoch ms) 우선
                LocalDateTime when = null;
                long epochMs = m.path("startDate").asLong(0L);
                if (epochMs > 0) {
                    when = Instant.ofEpochMilli(epochMs)
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .toLocalDateTime();
                }
                if (when == null) throw new IllegalArgumentException("시간 파싱 실패");

                // 팀ID/이름 (중첩객체 보조)
                String homeId = firstNonBlank(m, "homeTeamId", "homeId", "teamHomeId", "home");
                String awayId = firstNonBlank(m, "awayTeamId", "awayId", "teamAwayId", "away");
                if (homeId == null) homeId = m.path("homeTeam").path("teamId").asText(null);
                if (awayId == null) awayId = m.path("awayTeam").path("teamId").asText(null);

                String homeName = resolveTeam(teamNameById, homeId, null);
                String awayName = resolveTeam(teamNameById, awayId, null);
                if (homeName == null) homeName = firstNonBlank(m.path("homeTeam"), "name", "nameEng", "nameAcronym");
                if (awayName == null) awayName = firstNonBlank(m.path("awayTeam"), "name", "nameEng", "nameAcronym");

                if (homeName == null || awayName == null) throw new IllegalArgumentException("팀 파싱 실패");

                String league = firstNonBlank(m, "leagueId", "topLeagueId", "leagueName", "league"); // e.g., lck_2025
                String status = firstNonBlank(m, "matchStatus", "status", "state");                   // RESULT, SCHEDULED, ...
                String gameCode = firstNonBlank(m, "gameCode");                                       // lol
                String externalGameId = firstNonBlank(m, "gameId");                                   // 네이버 고유ID

                // 중복 방지
                boolean exists = matchScheduleRepository
                        .existsByTeamAIgnoreCaseAndTeamBIgnoreCaseAndMatchDate(homeName, awayName, when);
                if (exists) { dup++; continue; }

                MatchSchedule schedule = new MatchSchedule(
                        (gameCode != null ? gameCode.toUpperCase() : "LOL"),
                        homeName,
                        awayName,
                        when,
                        league,
                        status
                );

                // 선택 필드: externalGameId (엔티티에 있으면 세팅)
                safeInvoke(schedule, "setExternalGameId", externalGameId);

                matchScheduleRepository.save(schedule);
                saved++;
            } catch (Exception ex) {
                fail++;
                System.err.println("[NAVER WARN] 저장 실패(matches): " + ex.getMessage());
            }
        }

        System.out.printf("[NAVER %s matches] saved=%d dup=%d fail=%d%n", ym, saved, dup, fail);
        return saved;
    }

    // ===========================
    // 조회 메서드 (기존 유지)
    // ===========================
    public long countAll() {
        return matchScheduleRepository.count();
    }

    public List<MatchSchedule> findAll() {
        return matchScheduleRepository.findAll();
    }

    public List<MatchSchedule> findLatest(int n) {
        return matchScheduleRepository
                .findAll(PageRequest.of(0, n, Sort.by("matchDate").descending()))
                .getContent();
    }

    public List<MatchSchedule> findUpcoming(int n) {
        LocalDateTime now = LocalDateTime.now();
        return matchScheduleRepository
                .findByMatchDateBetween(
                        now, now.plusYears(1),
                        PageRequest.of(0, n, Sort.by("matchDate").ascending()))
                .getContent();
    }

    public List<MatchSchedule> findByDateRange(LocalDateTime from, LocalDateTime to) {
        return matchScheduleRepository
                .findByMatchDateBetween(
                        from, to, PageRequest.of(0, Integer.MAX_VALUE, Sort.by("matchDate").ascending()))
                .getContent();
    }

    public List<MatchSchedule> findByTeam(String team) {
        return matchScheduleRepository
                .findByTeamAIgnoreCaseOrTeamBIgnoreCase(
                        team, team, PageRequest.of(0, Integer.MAX_VALUE, Sort.by("matchDate").ascending()))
                .getContent();
    }

    // 연도 전체 시즌 크롤
    public int crawlSeason(int year) throws Exception {
        WebClient naverClient = this.defaultClient.mutate()
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .defaultHeader("Referer", "https://sports.naver.com/esports/schedule/League_of_Legends")
                .build();

        YearMonth cur = YearMonth.of(year, 1);
        YearMonth last = YearMonth.of(year, 12);

        int totalSaved = 0;
        while (!cur.isAfter(last)) {
            totalSaved += fetchMonthAndSaveFromNaver(naverClient, cur);
            try { Thread.sleep(150L); } catch (InterruptedException ignore) {}
            cur = cur.plusMonths(1);
        }
        System.out.println("[NAVER DONE] YEAR=" + year + " saved=" + totalSaved);
        return totalSaved;
    }

    // ===========================
    // 유틸
    // ===========================
    private static String firstNonBlank(JsonNode node, String... keys) {
        for (String k : keys) {
            String v = node.path(k).asText(null);
            if (v != null && !v.isBlank() && !"null".equalsIgnoreCase(v)) return v;
        }
        return null;
    }

    private static String resolveTeam(Map<String, String> map, String idCandidate, String nameCandidate) {
        if (idCandidate != null && map.containsKey(idCandidate)) return map.get(idCandidate);
        return nameCandidate;
    }

    /** 엔티티 선택 세터 안전 호출(없으면 무시) */
    private static void safeInvoke(Object target, String setterName, Object arg) {
        try {
            if (arg == null) return;
            Method m = target.getClass().getMethod(setterName, arg.getClass());
            m.invoke(target, arg);
        } catch (NoSuchMethodException ignore) {
        } catch (Exception e) {
            System.err.println("[WARN] " + setterName + " 세팅 실패: " + e.getMessage());
        }
    }
}
