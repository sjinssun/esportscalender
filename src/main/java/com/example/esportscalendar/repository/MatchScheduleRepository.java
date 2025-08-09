package com.example.esportscalendar.repository;

import com.example.esportscalendar.domain.MatchSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface MatchScheduleRepository extends JpaRepository<MatchSchedule, Long> {

    Page<MatchSchedule> findByMatchDateBetween(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<MatchSchedule> findByLeagueNameAndMatchDateBetween(
            String leagueName, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<MatchSchedule> findByMatchStatusAndMatchDateBetween(
            String matchStatus, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<MatchSchedule> findByTeamAIgnoreCaseOrTeamBIgnoreCase(
            String teamA, String teamB, Pageable pageable);

    Page<MatchSchedule> findByTeamAIgnoreCaseOrTeamBIgnoreCaseAndMatchDateBetween(
            String teamA, String teamB, LocalDateTime from, LocalDateTime to, Pageable pageable);

    boolean existsByTeamAIgnoreCaseAndTeamBIgnoreCaseAndMatchDate(String teamA, String teamB, LocalDateTime matchDate);
}

