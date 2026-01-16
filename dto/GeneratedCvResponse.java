package com.cvbuilder.dto;

import lombok.Data;

import java.util.List;

@Data
public class GeneratedCvResponse {

    private Long cvId;
    private String templateName;

    // Özet
    private String tailoredSummary;
    
    // iş ilanı pazar analizi
    private String aiCareerAdvice;

    // Yetenekler
    private List<String> prioritizedSkills;

    // Deneyimler (AI ile optimize edilmiş)
    private List<OptimizedCvItem> optimizedExperiences;

    // Projeler (AI ile optimize edilmiş, OptimizedCvItem formatında)
    private List<OptimizedCvItem> optimizedProjects;

    // Kullanıcının orijinal projelerinin optimize hali (UserProjectDTO)
    private List<UserProjectDTO> optimizedUserProjects;

    // Diller
    private List<UserLanguageDTO> optimizedLanguages;

    // Sertifikalar
    private List<UserCertificateDTO> optimizedCertificates;

    // Eğitim (UserProfile’dan optimize edilmiş liste)
    private List<UserEducationDTO> optimizedEducation;
    
    private List<String> tailoredSummaries;
}
