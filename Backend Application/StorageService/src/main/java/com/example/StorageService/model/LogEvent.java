package com.example.StorageService.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


import java.time.Instant;

@Data
@Document(collection = "telemetry_logs")
public class LogEvent {

    @Id
    private String id;


    @Indexed(expireAfter = "15d")
    private Instant timestamp;

    private String logLevel;
    private String message;
    private LogMetadata metadata;

    @Data
    public static class LogMetadata {
        private String userId;
        private String serviceName;
    }
}
