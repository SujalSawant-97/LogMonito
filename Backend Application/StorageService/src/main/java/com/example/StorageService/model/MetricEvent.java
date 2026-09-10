package com.example.StorageService.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Document(collection = "telemetry_metrics")
public class MetricEvent {

    @Id
    private String id;

    @Indexed(expireAfter = "4h")
    private Instant timestamp;

    private Double cpuUsage;
    private Double memoryUsed;
    private MetricMetadata metadata;

    @Data
    public static class MetricMetadata {
        private String userId;
        private String serviceName;
    }
}
