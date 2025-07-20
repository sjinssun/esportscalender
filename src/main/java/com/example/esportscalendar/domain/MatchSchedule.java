package com.example.esportscalendar.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity//데이터베이스 테이블과 1:1로 매핑되는 자바 클래스
public class MatchSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String gameName;
    private String teamA;
    private String teamB;
    private LocalDateTime matchDate;

    private boolean notified = false; //알람여부

    private String matchStatus; // scheduled, live, finished
    private String leagueName; // lck, worlds

    public MatchSchedule(String gameName, String teamA, String teamB,
                         LocalDateTime matchDate, String leagueName, String matchStatus) {
        this.gameName = gameName;
        this.teamA = teamA;
        this.teamB = teamB;
        this.matchDate = matchDate;
        this.leagueName = leagueName;
        this.matchStatus = matchStatus;
    }
    // 입력된 값 사용되게 해줌 ex)) lol T1 vs gen 11 11 2025 , id는 자동생성이라 빼도댐

    protected MatchSchedule(){

    }//JPA는 객체 생성시 new 사용하지않고 리플렉션이라는 기술로 생성. 따라서 protected로 외부에서는 사용하지 못하는 기본생성자 요구.

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String getName) {
        this.gameName = getName;
    }

    public String getTeamA() {
        return teamA;
    }

    public void setTeamA(String teamA) {
        this.teamA = teamA;
    }

    public String getTeamB() {
        return teamB;
    }

    public void setTeamB(String teamB) {
        this.teamB = teamB;
    }

    public LocalDateTime getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(LocalDateTime matchDate) {
        this.matchDate = matchDate;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public String getLeagueName() {
        return leagueName;
    }

    public void setLeagueName(String leagueName) {
        this.leagueName = leagueName;
    }
}
