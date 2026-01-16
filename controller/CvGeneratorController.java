package com.cvbuilder.controller;

import com.cvbuilder.dto.GeneratedCvResponse;
import com.cvbuilder.external.AiClient; // EKLENDİ
import com.cvbuilder.service.CvGeneratorService;
import com.cvbuilder.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cv-generator")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class CvGeneratorController {

    private final TranslationService translationService;
    private final CvGeneratorService cvGeneratorService;
    private final AiClient aiClient; // EKLENDİ: Direkt AI servisine erişim için

    // 1) CV OLUŞTURMA ENDPOINT'İ
    @PostMapping("/create")
    public ResponseEntity<?> createCv(
            @RequestHeader(value = "X-USER-ID", required = false) String userIdHeader,
            @RequestParam(required = false) Long jobId
    ) {
        try {
            // X-USER-ID header'ını kontrol et
            if (userIdHeader == null || userIdHeader.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "X-USER-ID header eksik",
                    "message", "Kullanıcı ID'si gönderilmedi"
                ));
            }
            
            Long userId;
            try {
                userId = Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Geçersiz kullanıcı ID formatı",
                    "message", "X-USER-ID sayısal bir değer olmalı"
                ));
            }
            
            log.info("CV oluşturma isteği - UserId: {}, JobId: {}", userId, jobId);
            GeneratedCvResponse response = cvGeneratorService.generateCvForJob(userId, jobId);
            log.info("CV başarıyla oluşturuldu - CV ID: {}", response.getCvId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // User not found, Profile not found gibi hatalar
            log.error("CV oluşturma hatası (RuntimeException): {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", "CV oluşturma hatası",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            // Diğer beklenmeyen hatalar
            log.error("CV oluşturma hatası (Exception): {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Sunucu hatası",
                "message", "CV oluşturulurken bir hata oluştu: " + e.getMessage()
            ));
        }
    }

    // 2) ÇEVİRİ ENDPOINT'İ
    @PostMapping("/translate")
    public ResponseEntity<?> translateCv(@RequestBody Object cvData, @RequestParam String lang) {
        try {
            Object translatedCv = translationService.translateCV(cvData, lang);
            
            if (translatedCv != null) {
                return ResponseEntity.ok(translatedCv);
            } else {
                return ResponseEntity.badRequest().body("Çeviri yapılamadı (Boş yanıt).");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Sunucu Hatası: " + e.getMessage());
        }
    }

    // 3) YENİ: KARİYER TAVSİYESİ (MARKET ANALYSIS) ENDPOINT'İ
    // Frontend'den gelen isteği karşılar: GET /api/cv-generator/career-advice?title=Java Developer
    @GetMapping("/career-advice")
    public ResponseEntity<String> getCareerAdvice(@RequestParam String title) {
        // AiClient içindeki metodu çağırıyoruz
        String advice = aiClient.getCareerAdvice(title);
        return ResponseEntity.ok(advice);
    }
}