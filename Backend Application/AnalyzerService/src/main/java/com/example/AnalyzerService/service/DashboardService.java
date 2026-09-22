package com.example.AnalyzerService.service;

import com.example.AnalyzerService.model.LogDocument;
import com.example.AnalyzerService.model.MetricDocument;
import com.example.AnalyzerService.repository.LogRepository;
import com.example.AnalyzerService.repository.MetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MetricRepository metricRepository;
    private final LogRepository logRepository;
    private final RestTemplate restTemplate = new RestTemplate(); // For calling the Local LLM

    @org.springframework.beans.factory.annotation.Value("${ai.debugger.url:http://localhost:8000/diagnose}")
    private String aiDebuggerUrl;

    // --- 1. DATA FETCHING METHODS ---

    public List<MetricDocument> getLast10Metrics(String userId, String appName) {
        return metricRepository.findByMetadataUserIdAndMetadataServiceNameOrderByTimestampDesc(
                userId, appName, PageRequest.of(0, 10));
    }

    public List<LogDocument> getLast10Errors(String userId, String appName) {
        return logRepository.findByMetadataUserIdAndMetadataServiceNameAndLogLevelOrderByTimestampDesc(
                userId, appName, "ERROR", PageRequest.of(0, 10));
    }

    // --- 2. AI ANALYSIS METHODS ---

    public Map<String, Object> analyzeSpecificError(String userId, String appName, String logId) {
        LogDocument errorLog = logRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found: " + logId));

        // 1. Check if this error was already diagnosed & stored in MongoDB
        if (errorLog.getDiagnosis() != null && !errorLog.getDiagnosis().isEmpty()) {
            return errorLog.getDiagnosis();
        }

        // 2. Query the Python AI Debugger service
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("logId", logId);
            requestBody.put("appName", appName);
            requestBody.put("message", errorLog.getMessage());
            requestBody.put("logLevel", errorLog.getLogLevel() != null ? errorLog.getLogLevel() : "ERROR");

            Map<String, Object> response = restTemplate.postForObject(aiDebuggerUrl, requestBody, Map.class);

            if (response != null && response.containsKey("summary")) {
                // 3. Store the diagnosis directly in MongoDB under the LogDocument
                errorLog.setDiagnosis(response);
                errorLog.setDiagnosedAt(Instant.now());
                logRepository.save(errorLog);

                return response;
            }
        } catch (Exception e) {
            System.err.println("AI Debugger Connection Failed: " + e.getMessage());
        }

        // Fallback if AI debugger is unavailable
        return Map.of(
                "summary", "Runtime Incident in " + appName,
                "cause", "Exception captured in telemetry. The AI Debugger bridge on port 8000 is unavailable.",
                "solution", "Start the LogMonito AI Debugger service (`run_api.bat`) to enable real-time automated root-cause analysis.",
                "insight", "AI Debugger bridge on port 8000 was unreachable."
        );
    }

    public String analyzeTimeWindow(String userId, String appName, int minutes) {
        Instant cutoffTime = Instant.now().minus(Duration.ofMinutes(minutes));

        List<LogDocument> recentLogs = logRepository
                .findByMetadataUserIdAndMetadataServiceNameAndTimestampAfterOrderByTimestampDesc(userId, appName, cutoffTime);
        List<MetricDocument> recentMetrics = metricRepository
                .findByMetadataUserIdAndMetadataServiceNameAndTimestampAfterOrderByTimestampDesc(userId, appName, cutoffTime);

        long errorCount = recentLogs.stream().filter(l -> "ERROR".equalsIgnoreCase(l.getLogLevel())).count();
        double avgCpu = recentMetrics.stream()
                .mapToDouble(m -> m.getCpuUsage() != null ? m.getCpuUsage() : 0.0)
                .average().orElse(0.0);
        double avgMem = recentMetrics.stream()
                .mapToDouble(m -> m.getMemoryUsed() != null ? m.getMemoryUsed() : 0.0)
                .average().orElse(0.0);

        return String.format("Telemetry Window (%d min): %d logs (%d errors), %d metric ticks. Avg CPU: %.1f%%, Avg Memory: %.1f MB.",
                minutes, recentLogs.size(), errorCount, recentMetrics.size(), avgCpu, avgMem);
    }
}
