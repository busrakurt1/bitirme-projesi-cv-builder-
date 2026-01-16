package com.cvbuilder.service;

import com.cvbuilder.dto.*;
import com.cvbuilder.entity.*;
import com.cvbuilder.external.AiClient;
import com.cvbuilder.repository.GeneratedCvRepository;
import com.cvbuilder.repository.JobPostingRepository;
import com.cvbuilder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CvGeneratorServiceImpl implements CvGeneratorService {

    private final UserRepository userRepository;
    private final JobPostingRepository jobPostingRepository;
    private final GeneratedCvRepository generatedCvRepository;
    private final AiClient aiClient;

    @Override
    @Transactional
    public GeneratedCvResponse generateCvForJob(Long userId, Long jobPostingId) {
        log.info("🎯 CV Generation started - User: {}, Job ID: {}", userId, jobPostingId);

        // 1) Kullanıcı ve Profil Kontrolü
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        UserProfile profile = user.getProfile();
        if (profile == null) {
            throw new RuntimeException("User profile not found for user id: " + userId);
        }

        // 2) İş İlanı Çözümleme (JobPosting)
        JobPosting job = resolveJobPosting(user, jobPostingId);
        String jobRequiredSkills = job != null ? safe(job.getRequiredSkills()) : "";
        String jobContext = buildJobContext(job);

        log.info("📊 Profile found - Name: {}, Job Title: {}", user.getFullName(), job != null ? job.getPosition() : "N/A");

        // 3) Yeteneklerin Önceliklendirilmesi (İlanla Eşleşenler Başa)
        List<String> userSkills = extractUserSkillNames(profile);
        List<String> prioritizedSkills = prioritizeSkills(userSkills, jobRequiredSkills);

        // 4) AI Optimizasyon Süreçleri
        // Summary (Özet)
        List<String> tailoredSummaries = Collections.emptyList();
        String selectedSummary = "";
        try {
            tailoredSummaries = aiClient.generateTailoredSummaries(profile, jobContext);
            selectedSummary = (tailoredSummaries != null && !tailoredSummaries.isEmpty()) 
                                    ? tailoredSummaries.get(0) : "";
        } catch (Exception e) {
            log.error("Özet oluşturma hatası: {}", e.getMessage());
            // Fallback: Basit özet oluştur
            String skills = profile.getSkills() != null && !profile.getSkills().isEmpty()
                    ? profile.getSkills().stream().limit(3).map(s -> s.getSkillName()).collect(java.util.stream.Collectors.joining(", "))
                    : "Mesleki Yetkinlikler";
            int years = (profile.getTotalExperienceYear() != null) ? profile.getTotalExperienceYear() : 0;
            selectedSummary = String.format("%s deneyime sahip bir %s olarak, %s alanlarındaki yetkinliğimle değer katmayı hedefliyorum.",
                    years > 0 ? years + " yıl" : "Yeni mezun", safe(profile.getTitle()), skills);
            tailoredSummaries = Collections.singletonList(selectedSummary);
        }

        // Deneyim, Proje, Eğitim, Dil ve Sertifikalar
        List<OptimizedCvItem> optExperiences = Collections.emptyList();
        List<OptimizedCvItem> optProjects = Collections.emptyList();
        try {
            optExperiences = aiClient.optimizeExperiences(profile, job);
        } catch (Exception e) {
            log.error("Deneyim optimizasyonu hatası: {}", e.getMessage());
            // Fallback: Orijinal deneyimleri kullan
            if (profile.getExperiences() != null) {
                optExperiences = profile.getExperiences().stream().map(exp -> 
                    new com.cvbuilder.dto.OptimizedCvItem(
                        safe(exp.getPosition()),
                        safe(exp.getCompany()),
                        formatDateRange(exp.getStartDate(), exp.getEndDate()),
                        Collections.singletonList(safe(exp.getDescription()))
                    )
                ).collect(java.util.stream.Collectors.toList());
            }
        }
        
        try {
            optProjects = aiClient.optimizeProjects(profile, job);
        } catch (Exception e) {
            log.error("Proje optimizasyonu hatası: {}", e.getMessage());
            // Fallback: Orijinal projeleri kullan
            if (profile.getProjects() != null) {
                optProjects = profile.getProjects().stream().map(p -> 
                    new com.cvbuilder.dto.OptimizedCvItem(
                        safe(p.getProjectName()),
                        "Proje",
                        formatDateRange(p.getStartDate(), (p.getIsOngoing() != null && p.getIsOngoing()) ? null : p.getEndDate()),
                        Collections.singletonList(safe(p.getDescription()))
                    )
                ).collect(java.util.stream.Collectors.toList());
            }
        }
        
        List<UserEducationDTO> optEducation = aiClient.optimizeEducation(profile, job);
        List<UserLanguageDTO> optLanguages = aiClient.optimizeLanguages(profile, job);
        List<UserCertificateDTO> optCertificates = aiClient.optimizeCertificates(profile, job);

        // 5) AI Kariyer Tavsiyesi
        String careerAdvice = "";
        try {
            String targetTitle = (job != null && job.getPosition() != null) ? job.getPosition() : profile.getTitle();
            careerAdvice = aiClient.getCareerAdvice(targetTitle);
        } catch (Exception e) {
            log.warn("⚠️ AI Career advice failed: {}", e.getMessage());
            careerAdvice = "Kariyer analizi şu an oluşturulamadı.";
        }

        // 6) ATS Dostu İçerik Oluşturma (Veritabanı İçin)
        String fullContentToSave = buildAtsFriendlyContent(
                user, profile, selectedSummary, prioritizedSkills,
                optExperiences, optProjects, optLanguages, optCertificates, optEducation
        );

        // 7) Veritabanına Kayıt
        JobPosting jobToSave = (job != null && job.getId() != null) ? job : null;
        GeneratedCv generatedCv = GeneratedCv.builder()
                .user(user)
                .jobPosting(jobToSave)
                .templateName("ATS_SMART_FULL_V3")
                .content(fullContentToSave)
                .aiCareerAdvice(careerAdvice)
                .build();

        generatedCvRepository.save(generatedCv);
        log.info("💾 CV saved to database - ID: {}", generatedCv.getId());

        // 8) Response DTO Hazırlama
        return mapToResponse(generatedCv.getId(), selectedSummary, tailoredSummaries, 
                             careerAdvice, prioritizedSkills, optExperiences, 
                             optProjects, optLanguages, optCertificates, optEducation);
    }

    // --- YARDIMCI METOTLAR ---

    private JobPosting resolveJobPosting(User user, Long jobPostingId) {
        if (jobPostingId != null && jobPostingId > 0) {
            return jobPostingRepository.findById(jobPostingId).orElseGet(() -> createDummyJobForUser(user));
        }
        // jobPostingId null ise, kullanıcının en son eklediği ilanı kullan
        List<JobPosting> userJobs = jobPostingRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (userJobs != null && !userJobs.isEmpty()) {
            return userJobs.get(0); // En son eklenen ilan (ilk eleman)
        }
        return null; // Hiç ilan yoksa null döndür
    }

    private JobPosting createDummyJobForUser(User user) {
        JobPosting dummy = new JobPosting();
        dummy.setUser(user);
        dummy.setPosition("Genel Başvuru");
        dummy.setRequiredSkills("");
        return dummy;
    }

    private String buildAtsFriendlyContent(User user, UserProfile profile, String summary, List<String> skills,
                                           List<OptimizedCvItem> exps, List<OptimizedCvItem> projs,
                                           List<UserLanguageDTO> langs, List<UserCertificateDTO> certs,
                                           List<UserEducationDTO> edus) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(50)).append("\n").append(user.getFullName().toUpperCase()).append("\n");
        sb.append(safe(profile.getTitle())).append("\n").append(user.getEmail()).append(" | ").append(user.getPhone()).append("\n\n");

        sb.append("🎯 PROFESSIONAL SUMMARY\n").append("-".repeat(25)).append("\n").append(summary).append("\n\n");

        sb.append("🛠️ TECHNICAL SKILLS\n").append("-".repeat(25)).append("\n").append(String.join(", ", skills)).append("\n\n");

        if (!exps.isEmpty()) {
            sb.append("💼 WORK EXPERIENCE\n").append("-".repeat(25)).append("\n");
            for (OptimizedCvItem exp : exps) {
                sb.append("📌 ").append(exp.getTitle()).append(" @ ").append(exp.getSubtitle())
                  .append(" (").append(exp.getDate()).append(")\n");
                exp.getDescription().forEach(d -> sb.append("  • ").append(d).append("\n"));
                sb.append("\n");
            }
        }

        if (!edus.isEmpty()) {
            sb.append("🎓 EDUCATION\n").append("-".repeat(25)).append("\n");
            for (UserEducationDTO edu : edus) {
                sb.append("🏫 ").append(edu.getSchoolName()).append("\n")
                  .append("   ").append(edu.getDepartment()).append(" | ").append(edu.getStartYear())
                  .append(" - ").append(edu.getGraduationYear() != null ? edu.getGraduationYear() : "Devam")
                  .append("\n\n");
            }
        }

        return sb.toString();
    }

    private List<String> extractUserSkillNames(UserProfile profile) {
        if (profile.getSkills() == null) return Collections.emptyList();
        return profile.getSkills().stream()
                .map(s -> s.getSkillName().trim())
                .filter(name -> !name.isEmpty())
                .toList();
    }

    private List<String> prioritizeSkills(List<String> userSkills, String jobKeywordsString) {
        if (userSkills == null || userSkills.isEmpty()) return Collections.emptyList();
        String[] keywords = jobKeywordsString.toLowerCase().split("[,;]");
        
        List<String> prioritized = new ArrayList<>();
        List<String> others = new ArrayList<>();

        for (String skill : userSkills) {
            boolean match = Arrays.stream(keywords).anyMatch(k -> skill.toLowerCase().contains(k.trim()));
            if (match) prioritized.add(skill);
            else others.add(skill);
        }
        prioritized.addAll(others);
        return prioritized;
    }

    private String buildJobContext(JobPosting job) {
        if (job == null) {
            return "Pozisyon: Genel Başvuru\nBeceriler: \nSorumluluklar: ";
        }
        return String.format("Pozisyon: %s\nBeceriler: %s\nSorumluluklar: %s", 
                safe(job.getPosition()), safe(job.getRequiredSkills()), safe(job.getResponsibilities()));
    }

    private GeneratedCvResponse mapToResponse(Long cvId, String summary, List<String> summaries, String advice,
                                              List<String> skills, List<OptimizedCvItem> exps, List<OptimizedCvItem> projs,
                                              List<UserLanguageDTO> langs, List<UserCertificateDTO> certs, List<UserEducationDTO> edus) {
        GeneratedCvResponse resp = new GeneratedCvResponse();
        resp.setCvId(cvId);
        resp.setTemplateName("ATS_SMART_FULL_V3");
        resp.setTailoredSummary(summary);
        resp.setTailoredSummaries(summaries);
        resp.setAiCareerAdvice(advice);
        resp.setPrioritizedSkills(skills);
        resp.setOptimizedExperiences(exps);
        resp.setOptimizedProjects(projs);
        resp.setOptimizedLanguages(langs);
        resp.setOptimizedCertificates(certs);
        resp.setOptimizedEducation(edus);
        return resp;
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
    
    private String formatDateRange(String start, String end) {
        String s = (start != null && !start.isBlank()) ? start : "Belirtilmemiş";
        String e = (end != null && !end.isBlank()) ? end : "Devam Ediyor";
        return s + " - " + e;
    }
}