package com.example.esportscalendar.repository;

import com.example.esportscalendar.domain.MatchSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchScheduleRepository extends JpaRepository<MatchSchedule, Long> {
}//save(entity)	저장 (insert or update) findAll()	전체 조회 findById(id)	id로 조회 delete(entity)	삭제
