package com.example.esportscalendar.controller;

import com.example.esportscalendar.domain.UserAlarm;
import com.example.esportscalendar.service.AlarmService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/alerts/discord")
public class AlarmController {

    private final AlarmService service;

    public AlarmController(AlarmService service) {
        this.service = service;
    }

    /** 0) 웹훅 연결 테스트 (프론트에서 '테스트' 버튼용) */
    @PostMapping("/test")
    public Map<String, Object> test(@RequestBody TestReq req) {
        service.sendTest(req.webhookUrl);
        return Map.of("ok", true);
    }

    /** 1) 구독 생성: 팀/웹훅/알림분 저장 */
    @PostMapping("/subscribe")
    public Map<String, Object> subscribe(@RequestBody SubscribeReq req) {
        UserAlarm ua = service.subscribe(req.teamName, req.webhookUrl, req.advanceMin);
        return Map.of(
                "ok", true,
                "subscriptionId", ua.getId(),
                "teamName", ua.getTeamName(),
                "advanceMin", ua.getAdvanceMin(),
                "active", ua.getActive()
        );
    }

    /** 2) 구독 목록(간단 버전: 전체) */
    @GetMapping("/subscriptions")
    public List<UserAlarm> list() {
        return service.listAll();
    }

    /** 3) 구독 비활성화 (알림 일시중지) */
    @PatchMapping("/subscriptions/{id}/deactivate")
    public Map<String, Object> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return Map.of("ok", true);
    }

    /** 4) 구독 삭제 */
    @DeleteMapping("/subscriptions/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        service.delete(id);
        return Map.of("ok", true);
    }

    // ====== DTO ======
    public static class TestReq {
        @NotBlank public String webhookUrl;
    }
    public static class SubscribeReq {
        /** "ALL" 또는 팀명 (예: "T1") */
        public String teamName;
        @NotBlank public String webhookUrl;
        @Min(1)  public Integer advanceMin; // null이면 10으로 기본 처리
    }
}
