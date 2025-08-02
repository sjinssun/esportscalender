package com.example.esportscalendar.repository;

import com.example.esportscalendar.domain.MatchSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchScheduleRepository extends JpaRepository<MatchSchedule, Long> {
}