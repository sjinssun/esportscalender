package com.example.esportscalendar.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 디스코드 웹훅 알림 구독
 * - 유저별로 특정 팀(또는 ALL) 경기를 일정 시간 전(advanceMin)에 알림
 * - 어디로 보낼지: webhookUrl
 */
@Entity
@Table(
        name = "user_alarm",
        uniqueConstraints = {
                // 같은 유저가 같은 팀에 같은 웹훅을 중복 등록하지 못하게
                @UniqueConstraint(name = "uq_user_alarm_user_team_webhook",
                        columnNames = {"user_id", "team_name", "webhook_url"})
        },
        indexes = {
                @Index(name = "idx_user_alarm_active", columnList = "active"),
                @Index(name = "idx_user_alarm_team", columnList = "team_name")
        }
)
public class UserAlarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 안 쓰면 null 허용 */
    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** "T1", "Gen.G" */
    @Column(name = "team_name", length = 100, nullable = false)
    private String teamName;

    /** 디스코드 Webhook URL (길 수 있어 TEXT 권장) */
    @Column(name = "webhook_url", columnDefinition = "text", nullable = false)
    private String webhookUrl;

    /** 몇 분 전 알림 (기본 10분) */
    @Column(name = "advance_min", nullable = false)
    private Integer advanceMin = 10;

    /** 구독 활성 여부 */
    @Column(nullable = false)
    private Boolean active = true;

    /** 생성/수정 시각 (운영 관찰 용) */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserAlarm() {}

    public UserAlarm(User user, String teamName, String webhookUrl, Integer advanceMin) {
        this.user = user;
        this.teamName = teamName;
        this.webhookUrl = webhookUrl;
        this.advanceMin = (advanceMin == null ? 10 : advanceMin);
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.advanceMin == null) this.advanceMin = 10;
        if (this.active == null) this.active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ===== Getter/Setter =====
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getTeamName() { return teamName; }
    public String getWebhookUrl() { return webhookUrl; }
    public Integer getAdvanceMin() { return advanceMin; }
    public Boolean getActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public void setAdvanceMin(Integer advanceMin) { this.advanceMin = advanceMin; }
    public void setActive(Boolean active) { this.active = active; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        // 보안: 웹훅 URL 전체 노출 방지(앞/뒤만)
        String masked = webhookUrl == null ? "null"
                : webhookUrl.substring(0, Math.min(24, webhookUrl.length())) + "...";
        return "UserAlarm{id=%d, team=%s, advance=%d, active=%s, webhook=%s}"
                .formatted(id, teamName, advanceMin, active, masked);
    }
}
