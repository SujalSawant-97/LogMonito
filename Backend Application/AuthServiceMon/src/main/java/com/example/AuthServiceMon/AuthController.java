package com.example.AuthServiceMon;





import com.example.AuthServiceMon.model.ClientApp;
import com.example.AuthServiceMon.repository.ClientAppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final ClientAppRepository clientAppRepository;

    // SecureRandom is cryptographically strong and much safer than standard UUIDs for API keys
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    /**
     * Called EXCLUSIVELY by the API Gateway to validate an incoming telemetry request.
     */
    @GetMapping("/validate")
    public ResponseEntity<String> validateApiKey(@RequestHeader("X-API-KEY") String apiKey) {

        Optional<ClientApp> clientApp = clientAppRepository.findByApiKeyAndIsActiveTrue(apiKey);

        if (clientApp.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or revoked API Key");
        }

        return ResponseEntity.ok(clientApp.get().getUserId());
    }

    /**
     * Called by your React Frontend when a user creates a new project.
     * Generates a cryptographically secure, URL-safe API key.
     */
    @PostMapping("/generate")
    public ResponseEntity<ClientApp> generateNewApiKey(
            @RequestHeader("X-USER-ID") String userId,
            @RequestParam String appName) {

        // 1. Generate 32 bytes of secure random data
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        // 2. Encode to a URL-safe Base64 string (no special characters that break headers)
        String secureToken = base64Encoder.encodeToString(randomBytes);

        // 3. Build the application record
        ClientApp newApp = new ClientApp();
        newApp.setUserId(userId);
        newApp.setAppName(appName);
        newApp.setApiKey("mon_" + secureToken); // Prefix helps users identify what the key is for
        newApp.setCreatedAt(Instant.now());
        newApp.setActive(true);

        // 4. Save to MongoDB and return to the React frontend
        ClientApp savedApp = clientAppRepository.save(newApp);
        return ResponseEntity.ok(savedApp);
    }
}
