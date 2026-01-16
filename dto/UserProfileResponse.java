package com.cvbuilder.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private Long userId;

    // Kişisel bilgiler
    private String fullName;
    private String email;   // User'dan gelecek
    private String phone;
    private String location;
    private String linkedinUrl;
    private String githubUrl;
    private String websiteUrl;

    // Özet
    private String title;
    private Integer totalExperienceYear;
    private String summary;


    // Listeler
    private List<UserSkillDTO> skills;
    private List<UserExperienceDTO> experiences;
    
    private List<UserEducationDTO> educations;
    
        // --- YENİ EKLENENLER ---
        private List<UserLanguageDTO> languages;       // YENİ
        private List<UserCertificateDTO> certificates; // YENİ
        private List<UserProjectDTO> projects;
		// public void setEducations(List<UserEducationDTO> collect) {
			// TODO Auto-generated method stub
			
		
    }

