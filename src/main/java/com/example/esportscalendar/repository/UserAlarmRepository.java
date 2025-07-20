package com.example.esportscalendar.repository;

import com.example.esportscalendar.domain.UserAlarm;
import com.example.esportscalendar.domain.User;
import com.example.esportscalendar.domain.MatchSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAlarmRepository extends JpaRepository<UserAlarm, Long> {

    // 특정 유저가 특정 경기 알람 설정했는지 확인
    Optional<UserAlarm> findByUserAndMatch(User user, MatchSchedule match);

    // 유저가 설정한 전체 알람 목록
    List<UserAlarm> findByUser(User user);

    // 특정 경기 알람 설정한 유저들
    List<UserAlarm> findByMatch(MatchSchedule match);
}
