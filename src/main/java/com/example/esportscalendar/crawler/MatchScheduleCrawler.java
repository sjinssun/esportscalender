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

    /**
     * 매일 새벽 4시에 자동 실행
     * cron 형식: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void autoCrawlLckSchedules() {
        String today = LocalDate.now().toString();
        String weekLater = LocalDate.now().plusDays(7).toString();

        try {
            int savedCount = matchScheduleService.crawlAndSaveLckSchedule(today, weekLater);
            System.out.printf("✅ 자동 크롤링 완료: %s ~ %s (저장 %d건)%n", today, weekLater, savedCount);
        } catch (Exception e) {
            System.err.printf("❌ 자동 크롤링 실패: %s ~ %s%n", today, weekLater);
            e.printStackTrace();
        }
    }
}
