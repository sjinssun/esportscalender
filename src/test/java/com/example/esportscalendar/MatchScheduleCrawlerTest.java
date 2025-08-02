package com.example.esportscalendar;

import com.example.esportscalendar.domain.MatchSchedule;
import com.example.esportscalendar.repository.MatchScheduleRepository;
import com.example.esportscalendar.service.MatchScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class MatchScheduleCrawlerTest {

    @Autowired
    private MatchScheduleService matchScheduleService;

    @Autowired
    private MatchScheduleRepository matchScheduleRepository;

    @Test
    public void 크롤링_실행_및_DB저장_확인() throws Exception {
        // ✅ WebClient 버전에서는 SSL 우회가 서비스 내부에서 처리되므로 따로 설정 필요 없음

        // given: 기존 데이터 삭제
        matchScheduleRepository.deleteAll();

        // when: 오늘 ~ 7일 뒤 일정 크롤링 후 저장
        String today = java.time.LocalDate.now().toString();
        String weekLater = java.time.LocalDate.now().plusDays(7).toString();
        matchScheduleService.crawlAndSaveLckSchedule(today, weekLater);

        // then: DB 저장 확인
        List<MatchSchedule> schedules = matchScheduleRepository.findAll();
        assertThat(schedules)
                .as("크롤링 후 일정이 최소 1건 이상 저장돼야 함")
                .isNotEmpty();

        // 콘솔 출력
        System.out.println("✅ 크롤링으로 저장된 일정 개수: " + schedules.size());
        schedules.forEach(s -> {
            System.out.println(s.getMatchDate() + " | " + s.getTeamA() + " vs " + s.getTeamB());
        });
    }
}
