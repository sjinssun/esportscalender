package com.example.esportscalendar.service;

import com.example.esportscalendar.domain.MatchSchedule;
import com.example.esportscalendar.repository.MatchScheduleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.catalina.CredentialHandler;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    // ✅ 네이버 month API 기반 크롤링
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

        // 네이버는 Referer 헤더 요구 가능 → 전용 클라이언트
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
     * 한 달치 불러와 저장
     * 엔드포인트 예: https://esports-api.game.naver.com/service/v2/schedule/month?month=2025-08&topLeagueId=lck&relay=false
     */
    private int fetchMonthAndSaveFromNaver(WebClient wc, YearMonth ym) throws Exception {
        String url = "https://esports-api.game.naver.com/service/v2/schedule/month"
                + "?month=" + ym + "&topLeagueId=lck&relay=false";
        System.out.println("[NAVER FETCH] " + url);

        String json = wc.get().uri(url).retrieve().bodyToMono(String.class).block();
        if (json == null || json.isBlank()) return 0;

        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree(json);

        // 네이버 공통 래핑: { code, message, content: { teams, days } }
        JsonNode content = root.path("content");
        if (content.isMissingNode() || content.isNull()) {
            System.out.println("[NAVER WARN] content 없음");
            return 0;
        }

        // 1) 팀 맵 구성 (teamId -> 이름/로고)
        Map<String, String> teamNameById = new HashMap<>();
        Map<String, String> teamLogoById = new HashMap<>();
        for (JsonNode t : content.path("teams")) {
            String id = t.path("teamId").asText(null);
            if (id == null || id.isBlank()) continue;
            String name = firstNonBlank(t, "name", "nameEng", "nameAcronym", "teamName", "teamNameEng");
            String logo = firstNonBlank(t, "imageUrl", "colorImageUrl", "whiteImageUrl", "blackImageUrl");
            if (name != null) teamNameById.put(id, name);
            if (logo != null) teamLogoById.put(id, logo);
        }

        // 2) days 배열
        JsonNode days = content.path("days");
        if (!days.isArray()) {
            System.out.println("[NAVER WARN] days 배열 없음");
            return 0;
        }

        int saved = 0, dup = 0, fail = 0;
        int dIdx = 0;
        for (JsonNode day : days) {
            dIdx++;
            String dateStr = firstNonBlank(day, "date", "gameDate", "matchDate"); // "yyyy-MM-dd"
            if (dateStr == null) continue;

            // day 안의 경기 리스트 키 탐색
            JsonNode matches = firstArray(day, "matches", "events", "games", "list", "items");
            if (matches == null) continue;

            for (JsonNode m : matches) {
                try {
                    // 시간 / ISO
                    String iso = firstNonBlank(m, "startTime", "startDateTime", "beginAt", "time", "matchTime");
                    LocalDateTime when = parseKst(dateStr, iso);
                    if (when == null) throw new IllegalArgumentException("시간 파싱 실패");

                    // 팀
                    String homeId = firstNonBlank(m, "homeTeamId", "homeId", "teamHomeId", "home");
                    String awayId = firstNonBlank(m, "awayTeamId", "awayId", "teamAwayId", "away");

                    String home = resolveTeam(teamNameById, homeId,
                            firstNonBlank(m, "homeTeam", "homeName", "home"));
                    String away = resolveTeam(teamNameById, awayId,
                            firstNonBlank(m, "awayTeam", "awayName", "away"));

                    if (home == null || away == null) throw new IllegalArgumentException("팀 파싱 실패");

                    String league = firstNonBlank(m, "leagueName", "league", "tournamentName", "competitionName");
                    String status = firstNonBlank(m, "status", "matchStatus", "state");

                    // 중복 방지
                    boolean exists = matchScheduleRepository
                            .existsByTeamAIgnoreCaseAndTeamBIgnoreCaseAndMatchDate(home, away, when);
                    if (exists) { dup++; continue; }

                    MatchSchedule schedule = new MatchSchedule(
                            "LOL",
                            home,
                            away,
                            when,
                            league,
                            status
                    );

                    // (선택) 로고 보관 필드가 있으면 세팅
                    try {
                        var setTeamALogo = MatchSchedule.class.getMethod("setTeamALogo", String.class);
                        var setTeamBLogo = MatchSchedule.class.getMethod("setTeamBLogo", String.class);
                        setTeamALogo.invoke(schedule, teamLogoById.get(homeId));
                        setTeamBLogo.invoke(schedule, teamLogoById.get(awayId));
                    } catch (NoSuchMethodException ignore) {
                        // 엔티티에 로고 필드 없으면 무시
                    }

                    matchScheduleRepository.save(schedule);
                    saved++;
                } catch (Exception ex) {
                    fail++;
                    System.err.println("[NAVER WARN] 저장 실패: " + ex.getMessage());
                }
            }
        }

        System.out.printf("[NAVER %s] saved=%d dup=%d fail=%d%n", ym, saved, dup, fail);
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

    private static JsonNode firstArray(JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode arr = node.path(k);
            if (arr.isArray()) return arr;
        }
        return null;
    }

    private static String resolveTeam(Map<String, String> map, String idCandidate, String nameCandidate) {
        if (idCandidate != null && map.containsKey(idCandidate)) return map.get(idCandidate);
        return nameCandidate;
    }

    /**
     * KST로 LocalDateTime 파싱
     * - ISO(…T…+09:00) 형식이면 그대로 Instant→KST
     * - 아니면 date("yyyy-MM-dd") + time("HH:mm") 결합
     */
    private static LocalDateTime parseKst(String dateOrNull, String isoOrTime) {
        if (isoOrTime != null && isoOrTime.contains("T")) {
            try {
                Instant inst = Instant.parse(isoOrTime.replace(" ", "T"));
                return LocalDateTime.ofInstant(inst, ZoneId.of("Asia/Seoul"));
            } catch (Exception ignore) {}
        }
        if (dateOrNull == null || isoOrTime == null) return null;
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try {
            return LocalDateTime.parse(dateOrNull + " " + isoOrTime, f);
        } catch (Exception e) {
            return null;
        }
    }
}
