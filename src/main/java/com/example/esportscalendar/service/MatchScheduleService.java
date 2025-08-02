package com.example.esportscalendar.service;

import com.example.esportscalendar.domain.MatchSchedule;
import com.example.esportscalendar.repository.MatchScheduleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class MatchScheduleService {

    private final MatchScheduleRepository matchScheduleRepository;

    // Lombok 없이 직접 생성자 작성
    public MatchScheduleService(MatchScheduleRepository matchScheduleRepository) {
        this.matchScheduleRepository = matchScheduleRepository;
    }

    public void crawlAndSaveLckSchedule(String startDate, String endDate) throws Exception {
        String baseUrl = "https://www.epromatch.com/api/v1/lol/matches";
        String region = "KR";
        String utcHours = "9";
        String calenderMode = "false";

        String apiUrl = String.format(
                "%s?regions=%s&startDate=%s&endDate=%s&utcHours=%s&calenderMode=%s",
                baseUrl, region, startDate, endDate, utcHours, calenderMode
        );

        // RestTemplate 사용
        RestTemplate restTemplate = new RestTemplate();

        // API 호출
        String json = restTemplate.getForObject(apiUrl, String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (JsonNode match : root) {
            String dateStr = match.get("date").asText(); // yyyy-MM-dd
            String timeStr = match.get("time").asText(); // HH:mm
            LocalDateTime dateTime = LocalDateTime.parse(dateStr + " " + timeStr, formatter);

            MatchSchedule schedule = new MatchSchedule(
                    "LOL",
                    match.get("team1").asText(),
                    match.get("team2").asText(),
                    dateTime,
                    "LCK",
                    match.get("status").asText()
            );

            matchScheduleRepository.save(schedule);
        }

        System.out.println("✅ LCK 경기 일정 저장 완료 (" + root.size() + "건)");
    }
}
