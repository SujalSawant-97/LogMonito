package com.example.StorageService.dto;

import lombok.Data;

import java.util.Map;

@Data
public class LogPayload {
    private String userId;
    private String timestamp;
    private String logLevel;
    private String message;
    private Map<String, String> metadata;
}
