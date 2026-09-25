package com.example.democlient.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.net.SocketTimeoutException;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    // 0. Status & Instructions
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "status", "UP",
                "app", "demo-order-service",
                "endpoints", Map.of(
                        "GET /api/test/null-pointer", "Triggers NullPointerException",
                        "GET /api/test/arithmetic", "Triggers ArithmeticException (/ by zero)",
                        "GET /api/test/database-failure", "Simulates MongoDB socket connection failure",
                        "POST /api/test/checkout", "Simulates DTO validation constraint failure",
                        "GET /api/test/payment-timeout", "Simulates caught exception with log.error(...)"
                )
        );
    }

    // 1. Classic Null Pointer Exception (Unhandled runtime crash)
    @GetMapping("/null-pointer")
    public String triggerNpe() {
        String customerName = null;
        // Triggers java.lang.NullPointerException
        return "Customer: " + customerName.toUpperCase();
    }

    // 2. Arithmetic / Divide-by-Zero Exception
    @GetMapping("/arithmetic")
    public int triggerArithmetic() {
        int totalAmount = 500;
        int discountDivisor = 0;
        // Triggers java.lang.ArithmeticException: / by zero
        return totalAmount / discountDivisor;
    }

    // 3. Simulated Database Connectivity Failure
    @GetMapping("/database-failure")
    public String triggerDbFailure() {
        // Simulates MongoSocketOpenException failure
        throw new RuntimeException(
                "com.mongodb.MongoSocketOpenException: Exception opening socket to localhost:27017"
        );
    }

    // 4. Simulated Validation Failure
    @PostMapping("/checkout")
    public Map<String, Object> triggerValidation(@RequestBody(required = false) Map<String, Object> order) {
        if (order == null || !order.containsKey("creditCard")) {
            throw new IllegalArgumentException(
                    "Validation Failure: 'creditCard' field is mandatory for checkout processing."
            );
        }
        return Map.of("status", "SUCCESS", "message", "Order processed");
    }

    // 5. Caught Exception with Explicit log.error(...)
    @GetMapping("/payment-timeout")
    public Map<String, String> triggerLoggedError() {
        try {
            throw new SocketTimeoutException("Stripe payment gateway read timed out after 5000ms");
        } catch (Exception e) {
            log.error("Payment processing transaction failed for order #ORD-98421", e);
            return Map.of(
                    "status", "FAILED",
                    "reason", "Payment processor read timeout. Error logged and forwarded to LogMonito."
            );
        }
    }
}
