package com.example.AnalyzerService.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "telemetry_logs")
public class LogDocument {

    @Id
    private String id;

    private Instant timestamp;
    private String logLevel;
    private String message;
    private LogMetadata metadata;

    private java.util.Map<String, Object> diagnosis;
    private Instant diagnosedAt;

    @Data
    public static class LogMetadata {
        private String userId;
        private String serviceName;
    }
}
