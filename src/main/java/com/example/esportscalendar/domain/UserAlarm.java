package com.example.esportscalendar.domain;

import jakarta.persistence.*;

@Entity
public class UserAlarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private MatchSchedule match;

    private boolean active = true; // 알람 설정 여부 (true면 알림 예정)

    public UserAlarm() {}

    public UserAlarm(User user, MatchSchedule match) {
        this.user = user;
        this.match = match;
        this.active = true;
    }

    // Getter / Setter
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public MatchSchedule getMatch() {
        return match;
    }

    public boolean isActive() {
        return active;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setMatch(MatchSchedule match) {
        this.match = match;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
