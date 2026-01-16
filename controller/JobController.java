package com.cvbuilder.controller;

import com.cvbuilder.dto.JobAnalysisRequest;
import com.cvbuilder.dto.JobAnalysisResponse;
import com.cvbuilder.entity.JobPosting;
import com.cvbuilder.service.JobAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class JobController {

    private final JobAnalysisService jobAnalysisService;

    // ✅ SADECE: TEKİL İLAN ANALİZİ (URL)
    @PostMapping("/analyze-by-url")
    public ResponseEntity<?> analyzeByUrl(@RequestBody JobAnalysisRequest request) {
        Long userId = request.getUserId();
        String url = request.getUrl();

        log.info("Job analyze-by-url request - URL: {}, UserID: {}", url, userId);

        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "url boş olamaz"));
        }

        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "userId boş olamaz"));
        }

        try {
            JobAnalysisResponse response = jobAnalysisService.analyzeJobPosting(userId, url);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Job analyze-by-url error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "İlan analizi sırasında hata oluştu",
                    "detail", e.getMessage()
            ));
        }
    }

    // ✅ KULLANICI İLANLARINI GETİR
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserJobs(@PathVariable Long userId) {
        log.info("Get user jobs request - UserID: {}", userId);

        try {
            List<JobPosting> userJobs = jobAnalysisService.getJobsByUserId(userId);
            if (userJobs == null || userJobs.isEmpty()) return ResponseEntity.noContent().build();
            return ResponseEntity.ok(userJobs);
        } catch (Exception e) {
            log.error("Get user jobs error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Kullanıcı ilanları alınırken hata oluştu",
                    "detail", e.getMessage()
            ));
        }
    }
}
