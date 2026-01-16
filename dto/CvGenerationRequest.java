package com.cvbuilder.dto;

import lombok.Data;
import java.util.List;

@Data
public class CvGenerationRequest {
    // Kullanıcının Veritabanındaki Bilgileri
    private String fullName;
    private String currentJobTitle; // Örn: Yazılım Mühendisi
    private List<String> userSkills; // Örn: [Java, React, SQL]
    private List<String> experiences; // Örn: ["Trendyol'da backend geliştirdim...", "Freelance iş yaptım..."]

    // Analiz Sonucundan Gelen Veriler (Görsel 2'deki veriler)
    private String targetJobTitle; // Örn: E-Ticaret Website Uzmanı
    private List<String> requiredKeywords; // Örn: [HTML, CSS, Flutter, Android]
}