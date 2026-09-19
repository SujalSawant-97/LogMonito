package com.example.monitor_spring_boot_starter.monitor;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "monitor")
public class MonitorProperties {
    private String apiKey;
    private String serverUrl = "http://localhost:8080"; // Default to API Gateway port
    private boolean enabled = true; // Let users easily turn it off in dev mode
    private String logLevel = "WARN"; // Default log threshold (WARN, ERROR, etc.)

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }
}
