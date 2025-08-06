package com.example.esportscalendar.controller;

import com.example.esportscalendar.service.MatchScheduleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedules")
public class MatchScheduleController {

    private final MatchScheduleService matchScheduleService;

    // Lombok 없이 직접 생성자 작성
    public MatchScheduleController(MatchScheduleService matchScheduleService) {
        this.matchScheduleService = matchScheduleService;
    }

    @GetMapping("/crawl")
    public String crawlLckSchedules() {
        try {
            // 원하는 날짜 범위 설정
            matchScheduleService.crawlAndSaveLckSchedule("2025-07-28", "2025-08-03");
            return "크롤링 완료!";
        } catch (Exception e) {
            e.printStackTrace();
            return "크롤링 실패: " + e.getMessage();
        }
    }
}
