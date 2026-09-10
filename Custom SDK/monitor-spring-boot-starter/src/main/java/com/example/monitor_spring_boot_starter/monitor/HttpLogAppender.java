package com.example.monitor_spring_boot_starter.monitor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HttpLogAppender extends AppenderBase<ILoggingEvent> {

    private String telemetryUrl;
    private String apiKey;
    private String appName;
    private int thresholdLevelInt = Level.WARN_INT; // Defaults to WARN
    private final RestTemplate restTemplate = new RestTemplate();

    // Setters called by your MonitorAutoConfiguration
    public void setTelemetryUrl(String telemetryUrl) { this.telemetryUrl = telemetryUrl; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public void setAppName(String appName) { this.appName = appName; }

    // Allows developers to configure this via application.yml (e.g., monitor.log-level=ERROR)
    public void setThresholdLevel(String level) {
        this.thresholdLevelInt = Level.toLevel(level, Level.WARN).levelInt;
    }

    @Override
    protected void append(ILoggingEvent event) {
        // 1. Filter: Drop anything below the configured threshold (INFO, DEBUG, TRACE)
        if (event.getLevel().levelInt < thresholdLevelInt) {
            return;
        }

        // 2. Build Payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString());
        payload.put("logLevel", event.getLevel().toString());
        payload.put("message", event.getFormattedMessage());

        Map<String, String> metadata = new HashMap<>();
        metadata.put("serviceName", appName);
        payload.put("metadata", metadata);

        // 3. Security Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", apiKey); // Authorize with Gateway

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        // 4. Send Asynchronously (Fire and forget to protect the client's thread)
        CompletableFuture.runAsync(() -> {
            try {
                restTemplate.postForEntity(telemetryUrl, request, String.class);
            } catch (Exception ignored) {
                // Fail silently. If the monitoring tool is offline, don't crash the client's app.
            }
        });
    }
}
