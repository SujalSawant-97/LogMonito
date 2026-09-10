package com.example.monitor_spring_boot_starter.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class MetricsForwarder {

    private final MeterRegistry meterRegistry;
    private final String serverUrl;
    private final String apiKey;
    private final String appName;
    private final RestTemplate restTemplate = new RestTemplate();

    public MetricsForwarder(MeterRegistry meterRegistry, String serverUrl, String apiKey, String appName) {
        this.meterRegistry = meterRegistry;
        this.serverUrl = serverUrl;
        this.apiKey = apiKey;
        this.appName = appName;
    }

    // Dynamically reads the interval from application.yml, falls back to 60,000ms (60s)
    @Scheduled(fixedRateString = "${monitor.metrics-interval:60000}")
    public void pushMetrics() {
        try {
            // 1. Extract Metrics from Actuator
            double processCpu = meterRegistry.get("process.cpu.usage").gauge().value();
            double jvmMemoryUsed = meterRegistry.get("jvm.memory.used").gauge().value();

            // 2. Build Payload (Handle NaN just in case the app is booting and metrics aren't ready)
            Map<String, Object> payload = new HashMap<>();
            payload.put("cpuUsage", Double.isNaN(processCpu) ? 0.0 : processCpu);
            payload.put("memoryUsed", Double.isNaN(jvmMemoryUsed) ? 0.0 : jvmMemoryUsed);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("serviceName", appName);
            payload.put("metadata", metadata);

            // 3. Security Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-KEY", apiKey); // Authorize with Gateway

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // 4. Send directly to your Ingestion Service
            restTemplate.postForEntity(serverUrl + "/api/v1/telemetry/metrics", request, String.class);

        } catch (Exception ignored) {
            // Fail silently to protect the client's application loop
        }
    }
}
