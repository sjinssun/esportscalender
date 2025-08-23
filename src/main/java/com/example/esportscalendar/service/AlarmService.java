package com.example.esportscalendar.service;

import com.example.esportscalendar.domain.UserAlarm;
import com.example.esportscalendar.infra.DiscordWebhookClient;
import com.example.esportscalendar.repository.UserAlarmRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class AlarmService {

    private final UserAlarmRepository repo;
    private final DiscordWebhookClient discord;

    // ← 정규식은 이렇게: 백슬래시 두 개로 이스케이프!
    private static final Pattern WEBHOOK_PATTERN =
            Pattern.compile("^https://discord\\.com/api/webhooks/.+");

    public AlarmService(UserAlarmRepository repo, DiscordWebhookClient discord) {
        this.repo = repo;
        this.discord = discord;
    }

    private void validateWebhook(String url) {
        if (url == null || !WEBHOOK_PATTERN.matcher(url).matches()) {
            throw new IllegalArgumentException("잘못된 디스코드 Webhook URL 입니다.");
        }
    }
    private String nvl(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    // 구독 생성
    @Transactional
    public UserAlarm subscribe(String teamName, String webhookUrl, Integer advanceMin) {
        validateWebhook(webhookUrl);
        UserAlarm ua = new UserAlarm();
        ua.setUser(null); // 로그인 붙이면 세팅
        ua.setTeamName((teamName == null || teamName.isBlank()) ? "ALL" : teamName);
        ua.setWebhookUrl(webhookUrl);
        ua.setAdvanceMin(advanceMin == null ? 10 : Math.max(1, advanceMin));
        ua.setActive(true);
        return repo.save(ua);
    }

    @Transactional(readOnly = true)
    public List<UserAlarm> listAll() { return repo.findAll(); }

    @Transactional
    public void deactivate(Long id) {
        var ua = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("구독이 없습니다."));
        ua.setActive(false);
        repo.save(ua);
    }

    @Transactional
    public void delete(Long id) { repo.deleteById(id); }

    public void sendTest(String webhookUrl) {
        validateWebhook(webhookUrl);
        discord.sendContentTo(webhookUrl, "✅ 웹훅 연결 테스트 성공!");
    }

    public void sendTenMinAlarm(String webhookUrl, String title, String desc) {
        validateWebhook(webhookUrl);
        discord.sendEmbedTo(webhookUrl, "eSports 알림봇", nvl(title), nvl(desc), 3447003);
    }
}
