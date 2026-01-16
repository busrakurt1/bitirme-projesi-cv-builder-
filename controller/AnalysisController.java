package com.cvbuilder.controller;

import com.cvbuilder.dto.AnalysisResult;
import com.cvbuilder.dto.JobMatchRequest;
import com.cvbuilder.dto.MarketAnalysisResponse;
import com.cvbuilder.service.CustomJobAnalysisService;
import com.cvbuilder.service.JobAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor // Constructor yerine Lombok kullanıldı, kod temizliği sağlandı
public class AnalysisController {

    private final CustomJobAnalysisService customJobAnalysisService;
    private final JobAnalysisService jobAnalysisService;

    @PostMapping("/job-match")
    public ResponseEntity<AnalysisResult> analyzeJobMatch(@RequestBody JobMatchRequest request) {
        log.info("📥 İş eşleşme analizi isteği alındı: userId={}", request.getUserId());
        try {
            AnalysisResult result = customJobAnalysisService.analyzeJobMatch(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ İş eşleşme analizi hatası: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/market-analysis/{userId}")
    public ResponseEntity<MarketAnalysisResponse> getMarketAnalysis(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "Software Development") String area) {
        log.info("📊 Pazar analizi isteği alındı: userId={}, area={}", userId, area);
        try {
            MarketAnalysisResponse response = jobAnalysisService.performMarketAnalysis(area, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Pazar analizi hatası: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Analysis API çalışıyor");
    }
}