package com.example.StorageService.dto;

import lombok.Data;

import java.util.Map;

@Data
public class MetricPayload {
    private String userId;
    private String timestamp; // Coming as String from JSON
    private Double cpuUsage;
    private Double memoryUsed;
    private Map<String, String> metadata;
}
