package com.example.ApiGatewayMonitor;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ApiKeyFilter extends AbstractGatewayFilterFactory<ApiKeyFilter.Config> {

    private final WebClient.Builder webClientBuilder;

    @org.springframework.beans.factory.annotation.Value("${auth.service.url:http://localhost:8082}")
    private String authServiceUrl;

    public ApiKeyFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 1. Check if the client application provided an API Key
            if (!exchange.getRequest().getHeaders().containsKey("X-API-KEY")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-KEY");

            // 2. Ask the Auth Service if this key belongs to a real user
            return webClientBuilder.build()
                    .get()
                    .uri(authServiceUrl + "/api/v1/auth/validate")
                    .header("X-API-KEY", apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(userId -> {
                        // 3. SUCCESS: The Auth Service returned the User ID.
                        // We mutate (modify) the request to attach this secure User ID to the headers.
                        exchange.getRequest().mutate()
                                .header("X-USER-ID", userId)
                                .build();

                        // 4. Pass the modified request forward to the Ingestion Service
                        return chain.filter(exchange);
                    })
                    .onErrorResume(error -> {
                        // 5. FAIL: The Auth Service rejected the key, or is offline. Drop the request.
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    });
        };
    }

    // Required by Spring Cloud Gateway for custom configuration
    public static class Config {
    }
}
