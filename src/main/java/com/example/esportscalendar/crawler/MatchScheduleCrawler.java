package com.example.esportscalendar.crawler;

import com.example.esportscalendar.service.MatchScheduleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class MatchScheduleCrawler {

    private final MatchScheduleService matchScheduleService;

    public MatchScheduleCrawler(MatchScheduleService matchScheduleService) {
        this.matchScheduleService = matchScheduleService;
    }

    // 매일 새벽 4시에 자동 실행
    @Scheduled(cron = "0 0 4 * * *")
    public void autoCrawlLckSchedules() {
        try {
            String today = LocalDate.now().toString();
            String weekLater = LocalDate.now().plusDays(7).toString();

            matchScheduleService.crawlAndSaveLckSchedule(today, weekLater);
            System.out.println("✅ 자동 크롤링 완료: " + today + " ~ " + weekLater);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
