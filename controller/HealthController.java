package com.cvbuilder.controller;

import com.cvbuilder.dto.ApiResponse;
import com.cvbuilder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("🔍 Health check endpoint called");
        
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "CV Builder Backend");
        healthInfo.put("timestamp", java.time.LocalDateTime.now().toString());
        healthInfo.put("database", "PostgreSQL");
        
        try {
            long userCount = userRepository.count();
            healthInfo.put("userCount", userCount);
            healthInfo.put("databaseStatus", "CONNECTED");
        } catch (Exception e) {
            healthInfo.put("databaseStatus", "ERROR: " + e.getMessage());
        }
        
        return ResponseEntity.ok(healthInfo);
    }
}