package com.example.IngestionService.dto;

import lombok.Data;

import java.util.Map;

@Data
public class MetricPayload {
    private String userId; // Injected by the Controller
    private Double cpuUsage;
    private Double memoryUsed;
    private Map<String, String> metadata; // Contains 'serviceName'
}
