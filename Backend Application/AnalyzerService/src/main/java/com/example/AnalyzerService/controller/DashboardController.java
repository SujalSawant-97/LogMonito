package com.example.AnalyzerService.controller;

import com.example.AnalyzerService.model.LogDocument;
import com.example.AnalyzerService.model.MetricDocument;
import com.example.AnalyzerService.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // Get strictly the last 10 metrics
    @GetMapping("/metrics")
    public ResponseEntity<List<MetricDocument>> getMetrics(
            @RequestHeader("X-USER-ID") String userId,
            @RequestParam String appName) {
        return ResponseEntity.ok(dashboardService.getLast10Metrics(userId, appName));
    }

    // Get strictly the last 10 errors
    @GetMapping("/logs/errors")
    public ResponseEntity<List<LogDocument>> getErrors(
            @RequestHeader("X-USER-ID") String userId,
            @RequestParam String appName) {
        return ResponseEntity.ok(dashboardService.getLast10Errors(userId, appName));
    }

    // AI Analysis 1: Analyze a specific error by its MongoDB ID
    @GetMapping("/ai/analyze/error/{logId}")
    public ResponseEntity<Map<String, Object>> analyzeSpecificError(
            @RequestHeader("X-USER-ID") String userId,
            @RequestParam String appName,
            @PathVariable String logId) {

        Map<String, Object> diagnosis = dashboardService.analyzeSpecificError(userId, appName, logId);
        return ResponseEntity.ok(diagnosis);
    }

    // AI Analysis 2: Analyze a time window (e.g., ?minutes=15)
    @GetMapping("/ai/analyze/window")
    public ResponseEntity<Map<String, String>> analyzeWindow(
            @RequestHeader("X-USER-ID") String userId,
            @RequestParam String appName,
            @RequestParam int minutes) {

        String insight = dashboardService.analyzeTimeWindow(userId, appName, minutes);
        return ResponseEntity.ok(Map.of("insight", insight));
    }
}