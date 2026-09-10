package com.example.IngestionService.controller;

import com.example.IngestionService.dto.LogPayload;
import com.example.IngestionService.dto.MetricPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String LOGS_TOPIC = "raw-logs-topic";
    private static final String METRICS_TOPIC = "raw-metrics-topic";

    @PostMapping("/logs")
    public ResponseEntity<Void> ingestLog(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody LogPayload payload) {

        // 1. Securely attach the validated User ID to the payload
        payload.setUserId(userId);

        // 2. Fire and forget to Kafka
        kafkaTemplate.send(LOGS_TOPIC, payload);

        // 3. Instantly return 202 Accepted (Don't keep the client waiting)
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/metrics")
    public ResponseEntity<Void> ingestMetric(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody MetricPayload payload) {

        payload.setUserId(userId);

        kafkaTemplate.send(METRICS_TOPIC, payload);

        return ResponseEntity.accepted().build();
    }
}
