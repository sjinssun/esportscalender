package com.example.esportscalendar.repository;

import com.example.esportscalendar.domain.MatchSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MatchScheduleRepository extends JpaRepository<MatchSchedule, Long> {

    // 기간
    Page<MatchSchedule> findByMatchDateBetween(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    // 리그 + 기간
    Page<MatchSchedule> findByLeagueNameAndMatchDateBetween(
            String leagueName, LocalDateTime from, LocalDateTime to, Pageable pageable);

    // 상태 + 기간
    Page<MatchSchedule> findByMatchStatusAndMatchDateBetween(
            String matchStatus, LocalDateTime from, LocalDateTime to, Pageable pageable);

    // 팀명 (대소문자 무시)
    Page<MatchSchedule> findByTeamAIgnoreCaseOrTeamBIgnoreCase(
            String teamA, String teamB, Pageable pageable);

    // 팀명 + 기간
    Page<MatchSchedule> findByTeamAIgnoreCaseOrTeamBIgnoreCaseAndMatchDateBetween(
            String teamA, String teamB, LocalDateTime from, LocalDateTime to, Pageable pageable);

    // 기존 중복체크(보조)
    boolean existsByTeamAIgnoreCaseAndTeamBIgnoreCaseAndMatchDate(
            String teamA, String teamB, LocalDateTime matchDate);

    // 네이버 외부ID 기반 중복 방지/조회 ===
    boolean existsByExternalGameId(String externalGameId);
    Optional<MatchSchedule> findByExternalGameId(String externalGameId);

    // 편의: 최신 경기 1건 (정렬용)
    Optional<MatchSchedule> findFirstByOrderByMatchDateDesc();
}