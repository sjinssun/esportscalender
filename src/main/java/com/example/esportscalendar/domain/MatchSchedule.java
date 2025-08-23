package com.example.esportscalendar.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "match_schedule",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_match_teamda_teamb_when", columnNames = {"teama", "teamb", "match_date"})
        },
        indexes = {
                @Index(name = "ix_match_date", columnList = "match_date"),
                @Index(name = "ix_league_name", columnList = "league_name"),
                @Index(name = "ix_teama", columnList = "teama"),
                @Index(name = "ix_teamb", columnList = "teamb")
        }
)
public class MatchSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // pk
    private Long id;

    // 외부 식별자(네이버 gameId). 선택이지만 강력 추천.
    @Column(name = "external_game_id", length = 64, unique = true)
    private String externalGameId;

    @Column(name = "game_name", nullable = false, length = 16)
    private String gameName; // LOL, VAL 등

    @Column(name = "teama", nullable = false, length = 100)
    private String teamA;

    @Column(name = "teamb", nullable = false, length = 100)
    private String teamB;

    @Column(name = "match_date", nullable = false)
    private LocalDateTime matchDate;

    @Column(name = "notified", nullable = false)
    private boolean notified = false;

    @Column(name = "match_status", nullable = false, length = 40)
    private String matchStatus; // RESULT, SCHEDULED, LIVE...

    @Column(name = "league_name", nullable = false, length = 40)
    private String leagueName; // lck_2025, lck 등


    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected MatchSchedule() { }

    public MatchSchedule(String gameName, String teamA, String teamB,
                         LocalDateTime matchDate, String leagueName, String matchStatus) {
        this.gameName = gameName;
        this.teamA = teamA;
        this.teamB = teamB;
        this.matchDate = matchDate;
        this.leagueName = leagueName;
        this.matchStatus = matchStatus;
    }

    // --- getter/setter ---

    public Long getId() { return id; }

    public String getExternalGameId() { return externalGameId; }
    public void setExternalGameId(String externalGameId) { this.externalGameId = externalGameId; }

    public String getGameName() { return gameName; }
    public void setGameName(String gameName) { this.gameName = gameName; }

    public String getTeamA() { return teamA; }
    public void setTeamA(String teamA) { this.teamA = teamA; }

    public String getTeamB() { return teamB; }
    public void setTeamB(String teamB) { this.teamB = teamB; }

    public LocalDateTime getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDateTime matchDate) { this.matchDate = matchDate; }

    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }

    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }

    public String getLeagueName() { return leagueName; }
    public void setLeagueName(String leagueName) { this.leagueName = leagueName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
