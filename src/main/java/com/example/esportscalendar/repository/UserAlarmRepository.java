package com.example.esportscalendar.repository;

import com.example.esportscalendar.domain.UserAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAlarmRepository extends JpaRepository<UserAlarm, Long> {

    // 활성화된 구독만 가져오기
    List<UserAlarm> findByActiveTrue();

    // 특정 유저 + 활성화된 구독 조회 (로그인 기능 붙였을 때 활용)
    List<UserAlarm> findByUser_IdAndActiveTrue(Long userId);
}
