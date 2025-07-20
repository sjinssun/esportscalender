package com.example.esportscalendar.repository;

import com.example.esportscalendar.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ username으로 유저 찾기 (중복 방지용)
    Optional<User> findByUsername(String username);
}
