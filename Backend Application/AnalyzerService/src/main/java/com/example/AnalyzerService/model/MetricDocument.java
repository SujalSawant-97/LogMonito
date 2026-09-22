package com.example.AnalyzerService.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "telemetry_metrics")
public class MetricDocument {

    @Id
    private String id;

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
