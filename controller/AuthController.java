package com.cvbuilder.controller;

import com.cvbuilder.dto.UserDTO;
import com.cvbuilder.dto.UserRequest;
import com.cvbuilder.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody UserRequest request) {
        try {
            log.info("📍 Register attempt for: {}", request.getEmail());

            if (request.getEmail() == null || request.getPassword() == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "message", "Email ve şifre zorunludur")
                );
            }

            if (userService.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "message", "Bu email zaten kayıtlı")
                );
            }

            UserDTO savedUser = userService.registerUser(request);
            log.info("✅ User registered: {}", savedUser.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Kullanıcı başarıyla kaydedildi",
                    "data", savedUser
            ));

        } catch (Exception e) {
            log.error("❌ Registration failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "Kayıt başarısız: " + e.getMessage())
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody UserRequest loginRequest) {
        try {
            log.info("📍 Login attempt for: {}", loginRequest.getEmail());

            if (loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "message", "Email ve şifre zorunludur")
                );
            }

            UserDTO user = userService.loginUser(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
            );

            log.info("✅ Login successful: {}", loginRequest.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Giriş başarılı",
                    "data", user
            ));

        } catch (Exception e) {
            log.error("❌ Login failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "Giriş başarısız: " + e.getMessage())
            );
        }
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "Auth API çalışıyor",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ✅ Frontend'in çağırdığı health endpoint
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "auth",
                "status", "UP",
                "time", LocalDateTime.now().toString()
        ));
    }
}
