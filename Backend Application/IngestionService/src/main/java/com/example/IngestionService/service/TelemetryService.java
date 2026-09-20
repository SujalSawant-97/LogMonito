package com.example.IngestionService.service;

import com.example.IngestionService.dto.LogPayload;
import com.example.IngestionService.dto.MetricPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String LOGS_TOPIC = "raw-logs-topic";
    private static final String METRICS_TOPIC = "raw-metrics-topic";

    public void publishLog(String userId, LogPayload payload) {
        payload.setUserId(userId);
        kafkaTemplate.send(LOGS_TOPIC, userId, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish log for user [{}]: {}", userId, ex.getMessage());
                    }
                });
    }

    public void publishMetric(String userId, MetricPayload payload) {
        payload.setUserId(userId);
        kafkaTemplate.send(METRICS_TOPIC, userId, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish metric for user [{}]: {}", userId, ex.getMessage());
                    }
                });
    }
}
