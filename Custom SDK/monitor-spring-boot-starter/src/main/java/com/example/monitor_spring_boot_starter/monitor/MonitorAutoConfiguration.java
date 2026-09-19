package com.example.monitor_spring_boot_starter.monitor;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.micrometer.core.instrument.MeterRegistry;

// @AutoConfiguration is the standard for Spring Boot 2.7 and 3.x
@AutoConfiguration
@EnableConfigurationProperties(MonitorProperties.class)
@ConditionalOnProperty(prefix = "monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling // 👈 CRITICAL: This is the "on switch" for the @Scheduled metrics timer!
public class MonitorAutoConfiguration {

    public MonitorAutoConfiguration(MonitorProperties properties, Environment env) {
        // 1. Programmatically inject the custom Logback Appender into the client app
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        HttpLogAppender customAppender = new HttpLogAppender();
        customAppender.setContext(loggerContext);

        // Pass the dynamically loaded properties to the appender
        customAppender.setTelemetryUrl(properties.getServerUrl() + "/api/v1/telemetry/logs");
        customAppender.setApiKey(properties.getApiKey());
        customAppender.setAppName(env.getProperty("spring.application.name", "unknown-app"));

        if (properties.getLogLevel() != null && !properties.getLogLevel().isBlank()) {
            customAppender.setThresholdLevel(properties.getLogLevel());
        }

        customAppender.start();

        // 2. Attach our custom appender to the client's root logger
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(customAppender);
    }

    // 3. Automatically create the Metrics Forwarder bean so Spring manages it
    @Bean
    public MetricsForwarder metricsForwarder(MeterRegistry registry, MonitorProperties properties, Environment env) {
        String appName = env.getProperty("spring.application.name", "unknown-app");

        // Returns the configured object, and Spring takes over running it every 60 seconds
        return new MetricsForwarder(registry, properties.getServerUrl(), properties.getApiKey(), appName);
    }
}
