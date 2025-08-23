package com.example.esportscalendar.controller;

import com.example.esportscalendar.domain.MatchSchedule;
import com.example.esportscalendar.service.MatchScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class MatchScheduleController {

    private final MatchScheduleService matchScheduleService;

    public MatchScheduleController(MatchScheduleService matchScheduleService) {
        this.matchScheduleService = matchScheduleService;
    }

    // =====================
    // 1) 수동 크롤링 (임의 기간)
    //    - 미지정 시: 이번달 1일 ~ 다음달 말
    // =====================
    @PostMapping("/crawl")
    public String crawlLckSchedules(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            LocalDate s = (startDate != null) ? startDate : YearMonth.now().atDay(1);
            LocalDate e = (endDate   != null) ? endDate   : YearMonth.now().plusMonths(1).atEndOfMonth();

            int saved = matchScheduleService.crawlAndSaveLckSchedule(s.toString(), e.toString());
            return "크롤링 완료! saved=" + saved + " (" + s + " ~ " + e + ")";
        } catch (Exception e) {
            e.printStackTrace();
            return "크롤링 실패: " + e.getMessage();
        }
    }

    // =====================
    // 1-1) 연도 전체 시즌 크롤 (1~12월)
    //     - 예) POST /api/schedules/crawl/season?year=2025
    // =====================
    @PostMapping("/crawl/season")
    public String crawlSeason(@RequestParam int year) {
        try {
            int saved = matchScheduleService.crawlSeason(year);
            return "시즌 크롤 완료! year=" + year + ", saved=" + saved;
        } catch (Exception e) {
            e.printStackTrace();
            return "시즌 크롤 실패: " + e.getMessage();
        }
    }

    // =====================
    // 2) 전체 또는 기간 조회
    // =====================
    @GetMapping
    public List<MatchSchedule> list(
            @RequestParam(required = false) String from, // yyyy-MM-dd or yyyy-MM-dd'T'HH:mm
            @RequestParam(required = false) String to
    ) {
        LocalDateTime fromDt = parseDateTime(from);
        LocalDateTime toDt   = parseDateTime(to);
        if (fromDt == null && toDt == null) {
            return matchScheduleService.findAll();
        }
        if (fromDt == null) fromDt = LocalDateTime.now().minusYears(10);
        if (toDt == null)   toDt   = LocalDateTime.now().plusYears(10);
        return matchScheduleService.findByDateRange(fromDt, toDt);
    }

    // =====================
    // 3) 팀명으로 조회
    // =====================
    @GetMapping("/team/{team}")
    public List<MatchSchedule> byTeam(@PathVariable String team) {
        return matchScheduleService.findByTeam(team);
    }

    // =====================
    // 4) 다가오는 경기 N건
    // =====================
    @GetMapping("/upcoming")
    public List<MatchSchedule> upcoming(@RequestParam(defaultValue = "5") int n) {
        return matchScheduleService.findUpcoming(n);
    }

    // =====================
    // 5) 점검용
    // =====================
    @GetMapping("/_count")
    public long count() {
        return matchScheduleService.countAll();
    }

    @GetMapping("/_sample")
    public List<MatchSchedule> sample(@RequestParam(defaultValue = "5") int n) {
        return matchScheduleService.findLatest(n);
    }

    // =====================
    // 내부 유틸
    // =====================
    private static final DateTimeFormatter ISO_M = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter ISO_D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private LocalDateTime parseDateTime(String v) {
        if (v == null || v.isBlank()) return null;
        try { return LocalDateTime.parse(v, ISO_M); } catch (Exception ignore) {}
        try { return LocalDate.parse(v, ISO_D).atStartOfDay(); } catch (Exception ignore) {}
        return null;
    }
}


