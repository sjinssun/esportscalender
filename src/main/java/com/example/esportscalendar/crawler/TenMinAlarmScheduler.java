package com.example.esportscalendar.crawler;

import com.example.esportscalendar.domain.MatchSchedule;
import com.example.esportscalendar.domain.UserAlarm;
import com.example.esportscalendar.infra.DiscordWebhookClient;
import com.example.esportscalendar.repository.MatchScheduleRepository;
import com.example.esportscalendar.repository.UserAlarmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class TenMinAlarmScheduler {

    private static final Logger log = LoggerFactory.getLogger(TenMinAlarmScheduler.class);

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserAlarmRepository alarms;
    private final MatchScheduleRepository matches;
    private final DiscordWebhookClient discord;

    public TenMinAlarmScheduler(UserAlarmRepository alarms,
                                MatchScheduleRepository matches,
                                DiscordWebhookClient discord) {
        this.alarms = alarms;
        this.matches = matches;
        this.discord = discord;
    }

    /**
     * 매 분 0초(KST)에 실행.
     * 각 구독(UserAlarm)에 대해: now+advanceMin ~ now+advanceMin+1분 사이 시작 경기를 찾아 해당 웹훅으로 발송.
     */
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void sendTenMinAlarms() {
        ZonedDateTime now = ZonedDateTime.now(KST).withSecond(0).withNano(0);
        List<UserAlarm> subs = alarms.findByActiveTrue();
        if (subs.isEmpty()) return;

        for (UserAlarm sub : subs) {
            int adv = sub.getAdvanceMin() == null ? 10 : Math.max(1, sub.getAdvanceMin());
            LocalDateTime from = now.plusMinutes(adv).toLocalDateTime();
            LocalDateTime to   = now.plusMinutes(adv + 1).toLocalDateTime();

            // 팀 필터 (teamName="ALL" 이면 전체)
            List<MatchSchedule> target =
                    isAll(sub.getTeamName())
                            ? matches.findByMatchDateBetween(
                            from, to,
                            PageRequest.of(0, 200, Sort.by("matchDate").ascending())
                    ).getContent()
                            : matches.findByTeamAIgnoreCaseOrTeamBIgnoreCaseAndMatchDateBetween(
                            sub.getTeamName(), sub.getTeamName(), from, to,
                            PageRequest.of(0, 200, Sort.by("matchDate").ascending())
                    ).getContent();

            for (MatchSchedule m : target) {
                // 간단 중복 방지: match_schedule.notified 사용
                // 여러 구독자가 같은 경기 받는 서비스면 alarm_sent_log 로 바꾸는 걸 권장
                if (getNotified(m)) continue;

                String desc = """
                        **%s vs %s**
                        리그: %s
                        일정: %s (KST)
                        """.formatted(nvl(m.getTeamA()), nvl(m.getTeamB()),
                        nvl(m.getLeagueName()),
                        m.getMatchDate().format(FMT));
                try {
                    discord.sendEmbedTo(sub.getWebhookUrl(),
                            "eSports 알림봇", "경기 10분 전 알림", desc, 3447003);
                    setNotifiedTrue(m);
                    matches.save(m);
                    // 디스코드 레이트리밋 완충
                    try { Thread.sleep(250); } catch (InterruptedException ignored) {}
                } catch (Exception e) {
                    log.error("웹훅 발송 실패 (subId={}, matchId={})", sub.getId(), m.getId(), e);
                }
            }
        }
    }

    private boolean isAll(String s) {
        return s == null || s.isBlank() || "ALL".equalsIgnoreCase(s);
    }
    private static String nvl(String s){ return s == null ? "-" : s; }

    // 네 엔티티에 맞춰 접근 (isNotified()/setNotified(boolean) 있다고 가정)
    private boolean getNotified(MatchSchedule m) {
        try { return (Boolean) MatchSchedule.class.getMethod("isNotified").invoke(m); }
        catch (Exception e) { return false; }
    }
    private void setNotifiedTrue(MatchSchedule m) {
        try { MatchSchedule.class.getMethod("setNotified", boolean.class).invoke(m, true); }
        catch (Exception ignored) {}
    }
}
