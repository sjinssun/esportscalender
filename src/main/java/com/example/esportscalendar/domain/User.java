package com.example.esportscalendar.domain;

import jakarta.persistence.*;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true) // ✅ username 중복 불가
    private String username;

    private String teamName; // 예: "T1", "Gen.G"

    public User() {
    }

    public User(String username, String teamName) {
        this.username = username;
        this.teamName = teamName;
    }

    // === Getter/Setter ===

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
}
