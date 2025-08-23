package com.example.esportscalendar.infra;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class DiscordWebhookClient {

    private final WebClient client = WebClient.builder().build();

    public void sendContentTo(String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        String payload = "{\"content\":" + json(content) + "}";
        client.post().uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public void sendEmbedTo(String webhookUrl, String username, String title, String description, int color) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        String payload = """
        { "username": %s, "embeds":[{ "title":%s, "description":%s, "color":%d }] }
        """.formatted(json(username), json(title), json(description), color);
        client.post().uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private String json(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n") + "\"";
    }
}
