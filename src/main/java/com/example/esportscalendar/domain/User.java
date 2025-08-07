package com.example.esportscalendar.domain;

import jakarta.persistence.*;


@Entity
@Table(name = "\"user\"")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true) // 중복 로그인 ID 방지
    private String loginId;

    private String password;

    private String username; // 닉네임 (T1 팬 등 표시용)

    private String teamName;

    public User() {}

    public User(String loginId, String password, String username, String teamName) {
        this.loginId = loginId;
        this.password = password;
        this.username = loginId;
        this.teamName = teamName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

