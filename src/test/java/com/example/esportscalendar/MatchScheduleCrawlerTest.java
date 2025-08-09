package com.example.esportscalendar;

import com.example.esportscalendar.domain.MatchSchedule;
import com.example.esportscalendar.repository.MatchScheduleRepository;
import com.example.esportscalendar.service.MatchScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MatchScheduleCrawlerTest {

    @Autowired
    private MatchScheduleService matchScheduleService;

    @Autowired
    private MatchScheduleRepository matchScheduleRepository;

    private int thisYear;
    private String startOfYear;
    private String endOfYear;

    @BeforeEach
    void setUp() {
        thisYear = LocalDate.now().getYear();
        startOfYear = LocalDate.of(thisYear, 1, 1).toString();          // yyyy-01-01
        endOfYear = LocalDate.of(thisYear, 12, 31).toString();          // yyyy-12-31
        matchScheduleRepository.deleteAll();
    }

    @Test
    @DisplayName("크롤링: 올해 LCK 메인/RTMSI만 저장 + 연도/중복/리그 검증")
    void crawl_and_verify_only_this_year_lck_main() throws Exception {
        // when
        int saved = matchScheduleService.crawlAndSaveLckSchedule(startOfYear, endOfYear);

        // then
        List<MatchSchedule> all = matchScheduleRepository.findAll();

        // 0) 최소 저장 확인 (시즌 기간 중엔 적어도 일부 저장)
        assertThat(all)
                .as("크롤링 후 일정이 최소 1건 이상 저장돼야 함")
                .isNotEmpty();

        // 1) 전부 올해(thisYear)만 있는지
        boolean allThisYear = all.stream()
                .map(MatchSchedule::getMatchDate)
                .allMatch(dt -> dt != null && dt.getYear() == thisYear);
        assertThat(allThisYear)
                .as("모든 일정의 연도가 %s 이어야 함", thisYear)
                .isTrue();

        // 2) 리그명: LCK 메인/RTMSI만 (CL/Academy 등 제외) — 이름 기반 2차 검증
        //    (서비스 레벨에서 토너먼트 ID 화이트리스트로 1차 컷이 있기 때문에 '이름'은 보조 검증)
        boolean anyExcludedLeague = all.stream()
                .map(MatchSchedule::getLeagueName)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .anyMatch(name ->
                        name.contains("LCK CL") ||
                                name.contains("ACADEMY") ||
                                name.contains("AS ") || name.endsWith(" AS") || name.contains("LCK AS")
                );
        assertThat(anyExcludedLeague)
                .as("LCK CL/Academy 시리즈가 저장되면 안 됨")
                .isFalse();

        // 3) 중복(같은 시각+같은 양팀) 없는지
        Set<String> keys = new HashSet<>();
        List<MatchSchedule> dups = all.stream().filter(ms -> {
            LocalDateTime t = ms.getMatchDate();
            String key = (t == null ? "null" : t.toString()) + "|" +
                    (ms.getTeamA() == null ? "null" : ms.getTeamA().toUpperCase()) + "|" +
                    (ms.getTeamB() == null ? "null" : ms.getTeamB().toUpperCase());
            if (keys.contains(key)) return true;
            keys.add(key);
            return false;
        }).collect(Collectors.toList());
        assertThat(dups)
                .as("같은 시각+동일 양팀 중복 저장이 없어야 함")
                .isEmpty();

        // 4) 콘솔로 요약 출력
        System.out.println("✅ 저장된 개수: " + all.size() + " (service 리턴: " + saved + ")");
        System.out.println("예시 5건:");
        all.stream()
                .sorted(Comparator.comparing(MatchSchedule::getMatchDate))
                .limit(5)
                .forEach(s -> System.out.println(
                        s.getMatchDate() + " | " + s.getLeagueName() + " | " + s.getTeamA() + " vs " + s.getTeamB()
                ));
    }
}

