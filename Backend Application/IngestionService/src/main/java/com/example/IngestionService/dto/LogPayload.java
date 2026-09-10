package com.example.IngestionService.dto;

import lombok.Data;

import java.util.Map;

@Data
public class LogPayload {
    private String userId; // Injected by the Controller
    private String timestamp;
    private String logLevel;
    private String message;
    private Map<String, String> metadata; // Contains 'serviceName'
}
