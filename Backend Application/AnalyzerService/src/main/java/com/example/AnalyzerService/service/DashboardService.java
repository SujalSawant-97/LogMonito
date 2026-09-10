package com.example.AnalyzerService.service;

import lombok.RequiredArgsConstructor;
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

    // Standard Ollama endpoint
    private static final String LOCAL_LLM_URL = "http://localhost:11434/api/generate";
    private static final String LLM_MODEL = "llama3"; // Change to mistral, qwen, etc.

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

    public String analyzeSpecificError(String userId, String appName, String logId) {
        LogDocument errorLog = logRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found"));

        String prompt = "You are an expert DevOps engineer. Analyze this specific error log from the application '"
                + appName + "' and explain what caused it and how to fix it in 3 short bullet points. \n"
                + "Error Message: " + errorLog.getMessage();

        return callLocalLlm(prompt);
    }

    public String analyzeTimeWindow(String userId, String appName, int minutes) {
        // Calculate the cutoff time
        Instant cutoffTime = Instant.now().minus(Duration.ofMinutes(minutes));

        // Fetch logs and metrics from that window
        List<LogDocument> recentLogs = logRepository
                .findByMetadataUserIdAndMetadataServiceNameAndTimestampAfterOrderByTimestampDesc(userId, appName, cutoffTime);
        List<MetricDocument> recentMetrics = metricRepository
                .findByMetadataUserIdAndMetadataServiceNameAndTimestampAfterOrderByTimestampDesc(userId, appName, cutoffTime);

        // Build a prompt injecting the context
        String prompt = "You are a DevOps AI. Analyze the system health for the last " + minutes + " minutes. \n" +
                "Total Logs generated: " + recentLogs.size() + "\n" +
                "Total Metrics recorded: " + recentMetrics.size() + "\n" +
                "Are there any spikes or concerning patterns? Keep your answer under 4 sentences.";

        return callLocalLlm(prompt);
    }

    // --- 3. LOCAL LLM INTEGRATION ---

    private String callLocalLlm(String prompt) {
        try {
            // Build the JSON payload for Ollama
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", LLM_MODEL);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false); // We want the whole response at once

            // Make the POST request to your local AI
            Map<String, Object> response = restTemplate.postForObject(LOCAL_LLM_URL, requestBody, Map.class);

            if (response != null && response.containsKey("response")) {
                return (String) response.get("response");
            }
            return "AI failed to generate an insight.";

        } catch (Exception e) {
            System.err.println("Local LLM Connection Failed: " + e.getMessage());
            return "Error: Could not connect to the Local LLM. Is it running on port 11434?";
        }
    }
}
