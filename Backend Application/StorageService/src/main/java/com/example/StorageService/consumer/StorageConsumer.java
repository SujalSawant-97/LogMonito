package com.example.StorageService.consumer;

import com.example.StorageService.dto.LogPayload;
import com.example.StorageService.dto.MetricPayload;
import com.example.StorageService.model.LogEvent;
import com.example.StorageService.model.MetricEvent;
import com.example.StorageService.repository.LogEventRepository;
import com.example.StorageService.repository.MetricEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class StorageConsumer {

    private final LogEventRepository logRepository;
    private final MetricEventRepository metricRepository;

    // 🎧 Listener 1: Process Logs
    @KafkaListener(topics = "raw-logs-topic", groupId = "storage-group")
    public void consumeLog(LogPayload payload) {
        try {
            LogEvent event = new LogEvent();
            // Parse the timestamp string back to an Instant object
            event.setTimestamp(Instant.now());
            if (payload.getTimestamp() != null) {
                event.setTimestamp(Instant.parse(payload.getTimestamp()));
            }

            event.setLogLevel(payload.getLogLevel());
            event.setMessage(payload.getMessage());

            LogEvent.LogMetadata meta = new LogEvent.LogMetadata();
            meta.setUserId(payload.getUserId());
            meta.setServiceName(payload.getMetadata().get("serviceName"));
            event.setMetadata(meta);

            logRepository.save(event);
            System.out.println("💾 [Log Saved] " + meta.getServiceName() + ": " + event.getLogLevel());

        } catch (Exception e) {
            System.err.println("❌ Failed to process log: " + e.getMessage());
        }
    }

    // 🎧 Listener 2: Process Metrics
    @KafkaListener(topics = "raw-metrics-topic", groupId = "storage-group")
    public void consumeMetric(MetricPayload payload) {
        try {
            MetricEvent event = new MetricEvent();
            event.setTimestamp(Instant.now());

            event.setCpuUsage(payload.getCpuUsage());
            event.setMemoryUsed(payload.getMemoryUsed());

            MetricEvent.MetricMetadata meta = new MetricEvent.MetricMetadata();
            meta.setUserId(payload.getUserId());
            meta.setServiceName(payload.getMetadata().get("serviceName"));
            event.setMetadata(meta);

            metricRepository.save(event);
            // System.out.println("📊 [Metrics Saved] " + meta.getServiceName());

        } catch (Exception e) {
            System.err.println("❌ Failed to process metric: " + e.getMessage());
        }
    }
}
