package com.example.esportscalendar.service;

import com.example.esportscalendar.domain.MatchSchedule;
import com.example.esportscalendar.repository.MatchScheduleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class MatchScheduleService { // ✅ 클래스명 수정

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

    // ✅ SSL 우회 WebClient 생성
    private WebClient createWebClient() throws SSLException {
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(t -> t.sslContext(sslContext));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("User-Agent", "Mozilla/5.0") // 차단 방지
                .build();
    }

    public int crawlAndSaveLckSchedule(String startDate, String endDate) throws Exception {
        String baseUrl = "https://www.epromatch.com/api/v1/lol/matches";
        String region = "Korea";
        String utcHours = "9";
        String calenderMode = "false";

        String apiUrl = String.format(
                "%s?regions=%s&startDate=%s&endDate=%s&utcHours=%s&calenderMode=%s",
                baseUrl, region, startDate, endDate, utcHours, calenderMode
        );

        // API 호출
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

        DateTimeFormatter formatterWithSeconds = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter formatterWithoutSeconds = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        int savedCount = 0;

        for (JsonNode leagueNode : root) {
            String tournamentName = leagueNode.path("tournament").path("name").asText();

            for (JsonNode match : leagueNode.path("matches")) {
                String dateStr = match.path("date").asText();
                String timeStr = match.path("time").asText();

                LocalDateTime dateTime;
                try {
                    dateTime = LocalDateTime.parse(dateStr + " " + timeStr, formatterWithSeconds);
                } catch (Exception e) {
                    dateTime = LocalDateTime.parse(dateStr + " " + timeStr, formatterWithoutSeconds);
                }

                String team1 = match.path("team1").asText();
                String team2 = match.path("team2").asText();
                String status = match.path("winner").asText();

                MatchSchedule schedule = new MatchSchedule(
                        "LOL",
                        team1,
                        team2,
                        dateTime,
                        tournamentName,
                        status
                );

                matchScheduleRepository.save(schedule);
                savedCount++;
            }
        }

        System.out.println("✅ LCK 경기 일정 저장 완료 (" + savedCount + "건)");
        return savedCount; // ✅ 반환 추가
    }
}
