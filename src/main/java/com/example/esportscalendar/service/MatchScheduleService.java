package com.example.esportscalendar.service;

import com.example.esportscalendar.domain.MatchSchedule;
import com.example.esportscalendar.repository.MatchScheduleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MatchScheduleService {

    private final MatchScheduleRepository matchScheduleRepository;
    private final WebClient webClient;

    public MatchScheduleService(MatchScheduleRepository matchScheduleRepository) {
        this.matchScheduleRepository = matchScheduleRepository;
        try {
            this.webClient = createWebClient();
        } catch (SSLException e) {
            throw new RuntimeException("WebClient 생성 실패", e);
        }
    }

    // SSL 우회 WebClient 생성 (외부 API 차단 회피용)
    private WebClient createWebClient() throws SSLException {
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(t -> t.sslContext(sslContext));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .build();
    }

    /**
     * Epromatch API에서 LCK 일정 크롤링 후 DB 저장
     * 파싱을 방어적으로 처리(키 후보, ISO/분리형 시간 모두 허용) + DEBUG 로그 제공
     */
    public int crawlAndSaveLckSchedule(String startDate, String endDate) throws Exception {
        String baseUrl = "https://www.epromatch.com/api/v1/lol/matches";
        String region = "Korea";
        String utcHours = "9";
        String calenderMode = "false";

        String apiUrl = String.format(
                "%s?regions=%s&startDate=%s&endDate=%s&utcHours=%s&calenderMode=%s",
                baseUrl, region, startDate, endDate, utcHours, calenderMode
        );

        String json = webClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (json == null || json.isBlank()) {
            throw new IllegalStateException("⚠️ API 응답이 비어있습니다.");
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        // --- 진단 로그: 첫 노드/첫 매치 키 구조 출력 ---
        if (root.isArray() && root.size() > 0) {
            JsonNode league0 = root.get(0);
            System.out.println("[DEBUG] league[0] keys:");
            league0.fieldNames().forEachRemaining(k -> System.out.print(k + " "));
            System.out.println();
            if (league0.has("matches") && league0.get("matches").isArray() && league0.get("matches").size() > 0) {
                JsonNode m0 = league0.get("matches").get(0);
                System.out.println("[DEBUG] match[0] raw: " + m0.toString());
                System.out.println("[DEBUG] match[0] keys:");
                m0.fieldNames().forEachRemaining(k -> System.out.print(k + " "));
                System.out.println();
            }
        } else {
            System.out.println("[DEBUG] root is not array. root: " + root.toString());
        }

        DateTimeFormatter fmtSec = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter fmtMin = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        int savedCount = 0;
        int failCount  = 0;

        for (JsonNode leagueNode : root) {
            String tournamentName = text(leagueNode.path("tournament"), "name");

            JsonNode matches = leagueNode.path("matches");
            if (!matches.isArray()) continue;

            for (JsonNode match : matches) {
                try {
                    // 1) 날짜/시간 파싱: (date+time) 또는 ISO( startTime / beginAt 등 )
                    LocalDateTime dateTime = null;

                    String dateStr = text(match, "date");
                    String timeStr = text(match, "time");

                    if (!isBlank(dateStr) && !isBlank(timeStr)) {
                        try {
                            dateTime = LocalDateTime.parse(dateStr + " " + timeStr, fmtSec);
                        } catch (Exception ex1) {
                            dateTime = LocalDateTime.parse(dateStr + " " + timeStr, fmtMin);
                        }
                    } else {
                        String iso = firstNonBlank(match,
                                "startTime", "start_time",
                                "startDateTime", "beginAt", "start_at");
                        if (!isBlank(iso)) {
                            Instant inst = Instant.parse(normalizeIso(iso)); // Z 또는 offset 필요
                            dateTime = LocalDateTime.ofInstant(inst, ZoneId.of("Asia/Seoul"));
                        }
                    }

                    if (dateTime == null) throw new IllegalArgumentException("match datetime 파싱 실패");

                    // 2) 팀/상태 키 후보 적용
                    String team1 = firstNonBlank(match,
                            "team1", "teamA", "homeTeam", "blueTeam", "team1Name", "home_team");
                    String team2 = firstNonBlank(match,
                            "team2", "teamB", "awayTeam", "redTeam", "team2Name", "away_team");
                    String status = firstNonBlank(match,
                            "winner", "status", "matchStatus");

                    if (isBlank(team1) || isBlank(team2)) {
                        throw new IllegalArgumentException("team 키 불일치");
                    }

                    MatchSchedule schedule = new MatchSchedule(
                            "LOL",          // gameName
                            team1,
                            team2,
                            dateTime,
                            tournamentName, // leagueName
                            status          // matchStatus
                    );

                    matchScheduleRepository.save(schedule);
                    savedCount++;
                } catch (Exception ex) {
                    failCount++;
                    System.err.println("[WARN] match 저장 실패: " + ex.getMessage());
                }
            }
        }

        System.out.printf("✅ LCK 일정 저장 완료: saved=%d, failed=%d%n", savedCount, failCount);
        return savedCount;
    }

    // ===== 조회 보조 메서드들 =====

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

    // ===== 내부 유틸 =====

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String text(JsonNode parent, String key) {
        return parent.path(key).asText(null);
    }

    private static String firstNonBlank(JsonNode node, String... keys) {
        for (String k : keys) {
            String v = node.path(k).asText(null);
            if (!isBlank(v)) return v;
        }
        return null;
    }

    /**
     * Instant.parse가 읽을 수 있도록 ISO 문자열 정규화
     * - "YYYY-MM-DD HH:mm[:ss]" → "YYYY-MM-DDTHH:mm[:ss]Z"
     * - 이미 Z 또는 +09:00 같은 오프셋이 있으면 그대로 둠
     */
    private static String normalizeIso(String iso) {
        String s = iso.trim();
        if (s.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}(:\\d{2})?")) {
            s = s.replace(' ', 'T') + "Z";
        }
        return s;
    }
}
