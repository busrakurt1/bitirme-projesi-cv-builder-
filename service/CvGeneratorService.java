package com.cvbuilder.service;

import com.cvbuilder.dto.GeneratedCvResponse;

public interface CvGeneratorService {

    // Mevcut metodunuz (aynı kalıyor)
    GeneratedCvResponse generateCvForJob(Long userId, Long jobPostingId);

    // [YENİ EKLENDİ - Opsiyonel] Eğer ID vermeden direkt son ilana göre üretmek isterseniz
    // GeneratedCvResponse generateCvForLatestJob(Long userId); 
    // (Şimdilik mevcut yapıyı bozmamak için sadece service implementasyonunda logic değişikliği yapacağım)
}