package com.example.IngestionService.controller;

import com.example.IngestionService.dto.LogPayload;
import com.example.IngestionService.dto.MetricPayload;
import com.example.IngestionService.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;

    @PostMapping("/logs")
    public ResponseEntity<Void> ingestLog(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody LogPayload payload) {

        telemetryService.publishLog(userId, payload);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/metrics")
    public ResponseEntity<Void> ingestMetric(
            @RequestHeader("X-USER-ID") String userId,
            @RequestBody MetricPayload payload) {

        telemetryService.publishMetric(userId, payload);
        return ResponseEntity.accepted().build();
    }
}
