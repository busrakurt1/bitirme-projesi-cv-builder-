/*package com.cvbuilder.controller;

import com.cvbuilder.dto.GeneratedCvResponse;
import com.cvbuilder.service.CvGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cv-generator") // React buraya istek atıyor
@RequiredArgsConstructor
//@CrossOrigin(origins = "*") // Tüm portlara (5173, 3000 vs) izin veriyoruz
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class CvGenerationController {

    private final CvGeneratorService cvGeneratorService;

    // React'taki axios.post isteği burayı tetikleyecek
    @PostMapping("/create")
    public ResponseEntity<GeneratedCvResponse> createCv(
            @RequestParam Long userId,
            @RequestParam Long jobId) {
            
        // Servise işi yaptır ve sonucu dön
        GeneratedCvResponse response = cvGeneratorService.generateCvForJob(userId, jobId);
        return ResponseEntity.ok(response);
    }
}*/