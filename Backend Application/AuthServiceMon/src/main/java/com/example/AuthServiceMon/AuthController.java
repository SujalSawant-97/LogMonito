package com.example.AuthServiceMon;

import com.example.AuthServiceMon.dto.AuthResponse;
import com.example.AuthServiceMon.dto.LoginRequest;
import com.example.AuthServiceMon.dto.RegisterRequest;
import com.example.AuthServiceMon.model.ClientApp;
import com.example.AuthServiceMon.model.User;
import com.example.AuthServiceMon.repository.ClientAppRepository;
import com.example.AuthServiceMon.repository.UserRepository;
import com.example.AuthServiceMon.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    /**
     * User registration for frontend dashboard users.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank() ||
            req.getPassword() == null || req.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("Email and password are required");
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email is already registered");
        }

        User user = new User();
        user.setEmail(req.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setName(req.getName());
        user.setCreatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser.getId(), savedUser.getEmail());

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .build());
    }

    /**
     * User login for frontend dashboard users.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req.getEmail() == null || req.getPassword() == null) {
            return ResponseEntity.badRequest().body("Email and password are required");
        }

        Optional<User> userOpt = userRepository.findByEmail(req.getEmail().trim().toLowerCase());
        if (userOpt.isEmpty() || !passwordEncoder.matches(req.getPassword(), userOpt.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        User user = userOpt.get();
        String token = jwtService.generateToken(user.getId(), user.getEmail());

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build());
    }

    /**
     * Called EXCLUSIVELY by the API Gateway to validate an incoming telemetry request from the SDK.
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
     * Called by the Frontend (protected by JWT filter at Gateway) to generate a new SDK API key for a project.
     */
    @PostMapping("/generate")
    public ResponseEntity<ClientApp> generateNewApiKey(
            @RequestHeader("X-USER-ID") String userId,
            @RequestParam String appName) {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String secureToken = base64Encoder.encodeToString(randomBytes);

        ClientApp newApp = new ClientApp();
        newApp.setUserId(userId);
        newApp.setAppName(appName);
        newApp.setApiKey("mon_" + secureToken);
        newApp.setCreatedAt(Instant.now());
        newApp.setActive(true);

        ClientApp savedApp = clientAppRepository.save(newApp);
        return ResponseEntity.ok(savedApp);
    }
}
