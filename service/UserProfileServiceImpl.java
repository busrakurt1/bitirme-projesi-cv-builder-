package com.cvbuilder.service;

import com.cvbuilder.dto.*;
import com.cvbuilder.entity.*;
import com.cvbuilder.repository.UserProfileRepository;
import com.cvbuilder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    // =========================================================
    //  SAVE OR UPDATE
    // =========================================================
    @Override
    @Transactional
    public UserProfileResponse saveOrUpdate(Long userId, UserProfileRequest request) {
        log.info("📌 saveOrUpdate called, userId={}", userId);

     // --- DEBUG: Frontend'den eğitim verisi geliyor mu? ---
        if (request.getEducations() != null) {
            log.info("✅ Gelen Eğitim Sayısı: {}", request.getEducations().size());
            request.getEducations().forEach(e -> 
                log.info("   -> Okul: {}, Derece: {}", e.getSchoolName(), e.getDegree())
            );
        } else {
            log.error("❌ HATA: Frontend 'educations' listesini NULL gönderiyor veya DTO ismi uyuşmuyor!");
        }
        
        // 1. Kullanıcıyı Bul
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // 2. Kullanıcının profilini userId üzerinden bul
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        if (profile == null) {
            log.info("🆕 Creating NEW profile for user: {}", userId);
            profile = new UserProfile();
            profile.setUser(user);

            // Listeleri başlat
            profile.setSkills(new ArrayList<>());
            profile.setExperiences(new ArrayList<>());
            profile.setLanguages(new ArrayList<>());
            profile.setCertificates(new ArrayList<>());
            profile.setProjects(new ArrayList<>());
            profile.setEducations(new ArrayList<>()); // YENİ: Eğitim Listesi
        } else {
            log.info("🔄 Updating EXISTING profile for user: {}", userId);
            deleteExistingCollections(profile);
        }

        // === USER BİLGİLERİNİ GÜNCELLE ===
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getLocation() != null) user.setLocation(request.getLocation());
        // experienceYear request'te varsa user'a da yazalım (senkronizasyon için)
        if (request.getTotalExperienceYear() != null) user.setExperienceYears(request.getTotalExperienceYear());
        
        userRepository.save(user);

        // === PROFİL TEMEL BİLGİLER ===
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setGithubUrl(request.getGithubUrl());
        profile.setWebsiteUrl(request.getWebsiteUrl());
        profile.setTitle(request.getTitle());
        profile.setSummary(request.getSummary());
        profile.setTotalExperienceYear(request.getTotalExperienceYear());

        // NOT: Eski educationSchool, educationStartYear alanlarını kaldırdık.
        // Artık aşağıda updateEducations metodu ile liste olarak kaydedeceğiz.

        // === KOLEKSİYONLARI GÜNCELLE ===
        try {
            if (request.getSkills() != null && !request.getSkills().isEmpty()) {
                updateSkills(profile, request.getSkills());
            }

            if (request.getExperiences() != null && !request.getExperiences().isEmpty()) {
                updateExperiences(profile, request.getExperiences());
            }

            if (request.getLanguages() != null && !request.getLanguages().isEmpty()) {
                updateLanguages(profile, request.getLanguages());
            }

            if (request.getCertificates() != null && !request.getCertificates().isEmpty()) {
                updateCertificates(profile, request.getCertificates());
            }

            if (request.getProjects() != null && !request.getProjects().isEmpty()) {
                updateProjects(profile, request.getProjects());
            }
            
            // YENİ: Eğitim Listesini Güncelle
            if (request.getEducations() != null && !request.getEducations().isEmpty()) {
                updateEducations(profile, request.getEducations());
            }

            // Kaydet
            UserProfile saved = userProfileRepository.save(profile);
            log.info("✅ Profile saved successfully. ID: {}", saved.getId());

            return mapToResponse(user, saved);

        } catch (Exception e) {
            log.error("❌ Error saving profile: ", e);
            throw new RuntimeException("Profil kaydedilemedi: " + e.getMessage());
        }
    }

    // =========================================================
    //  ESKİ KOLEKSİYONLARI TEMİZLE (orphanRemoval için)
    // =========================================================
    private void deleteExistingCollections(UserProfile profile) {
        log.debug("🗑️ Deleting existing collections for profile: {}", profile.getId());

        profile.getSkills().clear();
        profile.getExperiences().clear();
        profile.getLanguages().clear();
        profile.getCertificates().clear();
        profile.getProjects().clear();
        
        if (profile.getEducations() != null) {
            profile.getEducations().clear(); // YENİ
        }

        log.debug("🗑️ Collections cleared. Orphan removal will handle deletions.");
    }

    // =========================================================
    //  YARDIMCI METOTLAR - DTO → Entity UPDATE
    // =========================================================
    
    // --- YENİ: Eğitim Update Metodu ---
    private void updateEducations(UserProfile profile, List<UserEducationDTO> dtos) {
        log.debug("➕ Adding {} educations", dtos.size());
        for (UserEducationDTO dto : dtos) {
            if (isValid(dto.getSchoolName())) {
                UserEducation entity = new UserEducation();
                entity.setSchoolName(dto.getSchoolName());
                entity.setDepartment(dto.getDepartment());
                entity.setDegree(dto.getDegree());
                entity.setStartYear(dto.getStartYear());
                entity.setEndYear(dto.getGraduationYear()); // DTO'da graduationYear, Entity'de endYear olabilir
                entity.setGpa(dto.getGpa());
                
                entity.setUserProfile(profile);
                profile.getEducations().add(entity);
            }
        }
    }

    private void updateSkills(UserProfile profile, List<UserSkillDTO> dtos) {
        for (UserSkillDTO dto : dtos) {
            if (isValid(dto.getSkillName())) {
                UserSkill entity = new UserSkill();
                entity.setSkillName(dto.getSkillName());
                entity.setLevel(dto.getLevel());
                entity.setYears(dto.getYears() != null ? dto.getYears() : 0);
                entity.setUserProfile(profile);
                profile.getSkills().add(entity);
            }
        }
    }

    private void updateExperiences(UserProfile profile, List<UserExperienceDTO> dtos) {
        for (UserExperienceDTO dto : dtos) {
            if (!isValid(dto.getCompany()) && !isValid(dto.getPosition())) continue;

            UserExperience entity = new UserExperience();
            entity.setPosition(dto.getPosition());
            entity.setCompany(dto.getCompany());
            entity.setCity(dto.getCity());
            entity.setStartDate(dto.getStartDate());
            entity.setEndDate(dto.getEndDate());
            entity.setDescription(dto.getDescription());
            entity.setEmploymentType(dto.getEmploymentType());

            if (dto.getTechnologies() != null) {
                String techString = convertTechnologiesToString(dto.getTechnologies());
                entity.setTechnologies(techString);
            }

            entity.setUserProfile(profile);
            profile.getExperiences().add(entity);
        }
    }

    private void updateLanguages(UserProfile profile, List<UserLanguageDTO> dtos) {
        for (UserLanguageDTO dto : dtos) {
            if (isValid(dto.getLanguage())) {
                UserLanguage entity = new UserLanguage();
                entity.setLanguage(dto.getLanguage());
                entity.setLevel(dto.getLevel());
                entity.setUserProfile(profile);
                profile.getLanguages().add(entity);
            }
        }
    }

    private void updateCertificates(UserProfile profile, List<UserCertificateDTO> dtos) {
        for (UserCertificateDTO dto : dtos) {
            if (isValid(dto.getName())) {
                UserCertificate entity = new UserCertificate();
                entity.setName(dto.getName());
                entity.setIssuer(dto.getIssuer());
                entity.setDate(dto.getDate());
                entity.setUrl(dto.getUrl());
                entity.setUserProfile(profile);
                profile.getCertificates().add(entity);
            }
        }
    }

    private void updateProjects(UserProfile profile, List<UserProjectDTO> dtos) {
        for (UserProjectDTO dto : dtos) {
            if (isValid(dto.getProjectName())) {
                UserProject entity = new UserProject();
                entity.setProjectName(dto.getProjectName());
                entity.setStartDate(dto.getStartDate());

                if (Boolean.TRUE.equals(dto.getIsOngoing())) {
                    entity.setEndDate(null);
                    entity.setIsOngoing(true);
                } else {
                    entity.setEndDate(dto.getEndDate());
                    entity.setIsOngoing(false);
                }

                if (dto.getTechnologies() != null) {
                    String techString = convertTechnologiesToString(dto.getTechnologies());
                    entity.setTechnologies(techString);
                }

                entity.setUrl(dto.getUrl());

                // Açıklama kontrolü
                String descriptionFromDto = "";
                if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
                    descriptionFromDto = dto.getDescription();
                } else if (dto.getGeneratedDescription() != null && !dto.getGeneratedDescription().isBlank()) {
                    descriptionFromDto = dto.getGeneratedDescription();
                }
                entity.setDescription(descriptionFromDto);

                entity.setUserProfile(profile);
                profile.getProjects().add(entity);
            }
        }
    }

    private String convertTechnologiesToString(Object technologies) {
        if (technologies == null) return "";
        if (technologies instanceof String) {
            return (String) technologies;
        } else if (technologies instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<Object> techList = (List<Object>) technologies;
                return techList.stream()
                        .map(Object::toString)
                        .filter(s -> !s.trim().isEmpty())
                        .collect(Collectors.joining(", "));
            } catch (Exception e) {
                log.warn("⚠️ Could not convert technologies list: {}", e.getMessage());
                return technologies.toString();
            }
        } else {
            return technologies.toString();
        }
    }

    // =========================================================
    //  GET PROFILE
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        log.info("📥 Getting profile for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        UserProfileResponse response = mapToResponse(user, profile);
        log.info("📤 Profile retrieved successfully for user: {}", userId);

        return response;
    }

    // =========================================================
    //  MAP → RESPONSE
    // =========================================================
    private UserProfileResponse mapToResponse(User user, UserProfile profile) {
        UserProfileResponse resp = new UserProfileResponse();
        resp.setUserId(user.getId());
        resp.setFullName(user.getFullName());
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getPhone());
        resp.setLocation(user.getLocation());

        if (profile == null) {
            resp.setTotalExperienceYear(user.getExperienceYears() != null ? user.getExperienceYears() : 0);
            return resp;
        }

        resp.setId(profile.getId());
        resp.setLinkedinUrl(profile.getLinkedinUrl());
        resp.setGithubUrl(profile.getGithubUrl());
        resp.setWebsiteUrl(profile.getWebsiteUrl());
        resp.setTitle(profile.getTitle());
        resp.setSummary(profile.getSummary());
        resp.setTotalExperienceYear(profile.getTotalExperienceYear());

        // YENİ: EĞİTİM LİSTESİ MAPPING
        if (profile.getEducations() != null) {
            resp.setEducations(profile.getEducations().stream()
                .map(e -> UserEducationDTO.builder()
                        .id(e.getId())
                        .schoolName(e.getSchoolName())
                        .department(e.getDepartment())
                        .degree(e.getDegree())
                        .startYear(e.getStartYear())
                        .graduationYear(e.getEndYear())
                        .gpa(e.getGpa())
                        .build())
                .collect(Collectors.toList()));
        }

        if (profile.getSkills() != null) {
            resp.setSkills(profile.getSkills().stream()
                    .filter(s -> s.getSkillName() != null)
                    .map(s -> UserSkillDTO.builder()
                            .id(s.getId())
                            .skillName(s.getSkillName())
                            .level(s.getLevel())
                            .years(s.getYears())
                            .build())
                    .collect(Collectors.toList()));
        }

        if (profile.getExperiences() != null) {
            resp.setExperiences(profile.getExperiences().stream()
                    .filter(e -> e.getCompany() != null)
                    .map(e -> UserExperienceDTO.builder()
                            .id(e.getId())
                            .position(e.getPosition())
                            .company(e.getCompany())
                            .city(e.getCity())
                            .startDate(e.getStartDate())
                            .endDate(e.getEndDate())
                            .description(e.getDescription())
                            .employmentType(e.getEmploymentType())
                            .technologies(e.getTechnologies())
                            .build())
                    .collect(Collectors.toList()));
        }

        if (profile.getLanguages() != null) {
            resp.setLanguages(profile.getLanguages().stream()
                    .filter(l -> l.getLanguage() != null)
                    .map(l -> UserLanguageDTO.builder()
                            .id(l.getId())
                            .language(l.getLanguage())
                            .level(l.getLevel())
                            .build())
                    .collect(Collectors.toList()));
        }

        if (profile.getCertificates() != null) {
            resp.setCertificates(profile.getCertificates().stream()
                    .filter(c -> c.getName() != null)
                    .map(c -> UserCertificateDTO.builder()
                            .id(c.getId())
                            .name(c.getName())
                            .issuer(c.getIssuer())
                            .date(c.getDate())
                            .url(c.getUrl())
                            .build())
                    .collect(Collectors.toList()));
        }

        if (profile.getProjects() != null) {
            resp.setProjects(profile.getProjects().stream()
                    .filter(p -> p.getProjectName() != null)
                    .map(p -> UserProjectDTO.builder()
                            .id(p.getId())
                            .projectName(p.getProjectName())
                            .startDate(p.getStartDate())
                            .endDate(p.getEndDate())
                            .isOngoing(p.getIsOngoing())
                            .url(p.getUrl())
                            .description(p.getDescription())
                            .generatedDescription(p.getDescription())
                            .technologies(p.getTechnologies())
                            .technologyDetails(null)
                            .build())
                    .collect(Collectors.toList()));
        }

        return resp;
    }

    private boolean isValid(String s) {
        return s != null && !s.trim().isEmpty();
    }
}