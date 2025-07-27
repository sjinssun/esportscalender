package com.example.esportscalendar.repository;

import com.example.esportscalendar.domain.MatchSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface MatchScheduleRepository extends JpaRepository<MatchSchedule, Long> {
    List<MatchSchedule> findByTeamAOrTeamB(String teamA, String teamB);
}
