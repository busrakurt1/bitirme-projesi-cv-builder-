package com.cvbuilder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketAnalysisResponse {
    private String area;                    // Analiz edilen alan
    private String userDepartment;         // Kullanıcının bölümü
    private String userTitle;              // Kullanıcının unvanı
    private Map<String, Integer> topSkillsInMarket; // Pazardaki yetenekler
    private List<String> userMissingSkills; // Kullanıcıda eksik olan yetenekler
    private String aiRecommendation;       // AI tavsiyesi
    private boolean isAutoAnalyzed;        // Otomatik analiz mi?
    
    
}