package com.cvbuilder.service;

import com.cvbuilder.dto.JobAnalysisResponse;
import com.cvbuilder.dto.MarketAnalysisResponse;
import com.cvbuilder.entity.JobPosting;
import com.cvbuilder.entity.User;
import com.cvbuilder.entity.UserProfile;
import com.cvbuilder.external.AiClient;
import com.cvbuilder.external.JobScraperClient;
import com.cvbuilder.repository.JobPostingRepository;
import com.cvbuilder.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobAnalysisServiceImpl implements JobAnalysisService {

    private final UserRepository userRepository;
    private final JobPostingRepository jobPostingRepository;
    private final JobScraperClient scraper;
    private final AiClient aiClient;

    // Yeni kodun ihtiyacı (universal JSON parse)
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public JobAnalysisResponse analyzeJobPosting(Long userId, String url) {
        User user = userRepository.findById(userId).orElseThrow();
        Map<String, String> scrapedData = scraper.fetchJobData(url);
        String jobContent = scrapedData.getOrDefault("jobContent", scrapedData.getOrDefault("fullText", ""));
        return runComprehensiveAnalysis(user, jobContent, url);
    }

    @Override
    @Transactional
    public JobAnalysisResponse analyzeJobByRawText(Long userId, String jobContent) {
        User user = userRepository.findById(userId).orElseThrow();
        return runComprehensiveAnalysis(user, jobContent, "Manuel Giriş");
    }

    private JobAnalysisResponse runComprehensiveAnalysis(User user, String jobContent, String url) {
        if (jobContent == null || jobContent.length() < 50) {
            return JobAnalysisResponse.builder().formattedAnalysis("İçerik çekilemedi.").build();
        }

        // ==============================
        // 1) ESKİ AKIŞ (DETAILED + REPORT)
        // ==============================
        Map<String, Object> aiDetailed = aiClient.analyzeJobPostingDetailed(jobContent);
        String detailedReport = aiClient.analyzeJobSubmission(user.getProfile(), jobContent);

        // Detailed içinden güvenli listeler (ClassCastException fix)
        List<String> jobSkillsDetailed = safeGetList(aiDetailed, "technicalSkills");
        List<String> responsibilitiesDetailed = safeGetList(aiDetailed, "responsibilities");

        // ==============================
        // 2) YENİ AKIŞ (UNIVERSAL JSON)
        // ==============================
        String jsonResult = aiClient.analyzeJobPostingUniversal(jobContent);
        JobAiResult aiUniversal = parseAiResult(jsonResult);

        // ==============================
        // VERİ BİRLEŞTİRME: Tüm kaynaklardan gelen verileri birleştir
        // ==============================
        // Skills: Universal ve Detailed'dan gelen tüm skills'leri birleştir (duplicate'leri kaldırarak)
        Set<String> allSkillsSet = new LinkedHashSet<>();
        if (!aiUniversal.technicalSkills.isEmpty()) {
            allSkillsSet.addAll(aiUniversal.technicalSkills);
        }
        if (!jobSkillsDetailed.isEmpty()) {
            allSkillsSet.addAll(jobSkillsDetailed);
        }
        List<String> jobSkills = new ArrayList<>(allSkillsSet);

        // Responsibilities: Universal ve Detailed'dan gelen tüm responsibilities'leri birleştir
        Set<String> allResponsibilitiesSet = new LinkedHashSet<>();
        if (!aiUniversal.responsibilities.isEmpty()) {
            allResponsibilitiesSet.addAll(aiUniversal.responsibilities);
        }
        if (!responsibilitiesDetailed.isEmpty()) {
            allResponsibilitiesSet.addAll(responsibilitiesDetailed);
        }
        List<String> responsibilities = new ArrayList<>(allResponsibilitiesSet);

        // ==============================
        // 3) BEcERİ EŞLEŞME (Eski mantık korunur)
        // ==============================
        List<String> userSkills = getUserSkillsNormalized(user);
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String req : jobSkills) {
            if (isUserHasSkill(userSkills, req)) matched.add(req);
            else missing.add(req);
        }

        int score = jobSkills.isEmpty() ? 0 : (int) Math.round((matched.size() * 100.0) / jobSkills.size());

        // ==============================
        // 4) DB KAYDI (İki kod entegre)
        // - hem eski alanlar (cleanedText, analysisReport)
        // - hem yeni alanlar (requiredSkills, responsibilities)
        // ==============================
        JobPosting jp = saveJobPosting(user, url, aiDetailed, aiUniversal, jobContent, detailedReport, matched, responsibilities);

        // ==============================
        // 5) DTO DOLDURMA (Eski + Yeni)
        // - Eski alanlar detailed'dan fallback ile
        // - Yeni alanlar universal'dan
        // ==============================
        return JobAnalysisResponse.builder()
                .jobId(jp.getId())

                // Önce universal doluysa onu kullan, değilse detailed fallback
                .position(firstNonBlank(aiUniversal.position, safeGetString(aiDetailed, "position", "Belirtilmemiş")))
                .company(firstNonBlank(aiUniversal.company, safeGetString(aiDetailed, "company", "Bilinmiyor")))
                .location(firstNonBlank(aiUniversal.location, safeGetString(aiDetailed, "location", "Belirtilmemiş")))
                .workType(firstNonBlank(aiUniversal.workType, safeGetString(aiDetailed, "workType", "Belirtilmemiş")))
                .experienceLevel(firstNonBlank(aiUniversal.experienceLevel, safeGetString(aiDetailed, "experienceLevel", "Belirtilmemiş")))
                .educationLevel(firstNonBlank(aiUniversal.educationLevel, safeGetString(aiDetailed, "educationLevel", "Belirtilmemiş")))
                .summary(firstNonBlank(aiUniversal.summary, safeGetString(aiDetailed, "summary", "")))

                // Yeni alanlar (universal'dan)
                .militaryStatus(emptyToUnspecified(aiUniversal.militaryStatus))
                .salary(emptyToUnspecified(aiUniversal.salary))

                .matchedSkills(matched)
                .missingSkills(missing)
                .matchScore(score)

                // Eski rapor korunur (detaylı recruiter raporu)
                .formattedAnalysis(detailedReport)

                .responsibilities(responsibilities)
                .build();
    }

    // ==============================
    // UNIVERSAL JSON PARSE (Yeni kod)
    // ==============================
    private JobAiResult parseAiResult(String json) {
        JobAiResult result = new JobAiResult();
        try {
            JsonNode root = objectMapper.readTree(json == null ? "{}" : json);

            result.position = getText(root, "position");
            result.company = getText(root, "company");
            result.location = getText(root, "location");
            result.workType = getText(root, "workType");
            result.experienceLevel = getText(root, "experienceLevel");
            result.educationLevel = getText(root, "educationLevel");
            result.militaryStatus = getText(root, "militaryStatus");
            result.salary = getText(root, "salary");
            result.summary = getText(root, "summary");

            if (root.has("technicalSkills") && root.get("technicalSkills").isArray()) {
                result.technicalSkills = StreamSupport.stream(root.get("technicalSkills").spliterator(), false)
                        .map(JsonNode::asText)
                        .filter(s -> s != null && !s.isBlank())
                        .distinct()
                        .collect(Collectors.toList());
            }

            if (root.has("responsibilities") && root.get("responsibilities").isArray()) {
                result.responsibilities = StreamSupport.stream(root.get("responsibilities").spliterator(), false)
                        .map(JsonNode::asText)
                        .filter(s -> s != null && !s.isBlank())
                        .distinct()
                        .collect(Collectors.toList());
            }

            // Languages alanını da parse et
            if (root.has("languages") && root.get("languages").isArray()) {
                List<String> languages = StreamSupport.stream(root.get("languages").spliterator(), false)
                        .map(JsonNode::asText)
                        .filter(s -> s != null && !s.isBlank())
                        .distinct()
                        .collect(Collectors.toList());
                // Languages bilgisini summary'ye ekle (eğer varsa)
                if (!languages.isEmpty() && result.summary != null && !result.summary.isEmpty()) {
                    result.summary += " | Gereken Diller: " + String.join(", ", languages);
                } else if (!languages.isEmpty()) {
                    result.summary = "Gereken Diller: " + String.join(", ", languages);
                }
            }
        } catch (Exception e) {
            log.error("JSON Parse Hatası: ", e);
        }
        return result;
    }

    private String getText(JsonNode node, String field) {
        if (node == null || field == null) return "";
        if (!node.has(field) || node.get(field).isNull()) return "";
        return node.get(field).asText("");
    }

    private static class JobAiResult {
        String position = "";
        String company = "";
        String location = "";
        String workType = "";
        String experienceLevel = "";
        String educationLevel = "";
        String militaryStatus = "";
        String salary = "";
        String summary = "";
        List<String> technicalSkills = new ArrayList<>();
        List<String> responsibilities = new ArrayList<>();
    }

    // ==============================
    // ESKİ SAFE METHODS (Aynen korunur)
    // ==============================
    @SuppressWarnings("unchecked")
    private List<String> safeGetList(Map<String, Object> map, String key) {
        Object obj = map.get(key);
        if (obj instanceof List) return (List<String>) obj;
        if (obj instanceof String) return Arrays.asList(obj.toString().split(","));
        return new ArrayList<>();
    }

    private String safeGetString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return (val != null && !val.toString().isBlank()) ? val.toString().trim() : defaultValue;
    }

    private JobPosting saveJobPosting(
            User user,
            String url,
            Map<String, Object> aiDetailed,
            JobAiResult aiUniversal,
            String rawText,
            String report,
            List<String> matchedSkills,
            List<String> responsibilities
    ) {
        String safeText = safeTruncate(rawText, 4000);

        // DB’ye kaydedilecek skill/responsibility stringleri
        List<String> jobSkills = !aiUniversal.technicalSkills.isEmpty()
                ? aiUniversal.technicalSkills
                : safeGetList(aiDetailed, "technicalSkills");

        String skillsStr = String.join(", ", jobSkills);
        String respStr = String.join("; ", responsibilities);

        return jobPostingRepository.save(JobPosting.builder()
                .user(user)
                .url(url)
                .position(firstNonBlank(aiUniversal.position, safeGetString(aiDetailed, "position", "Belirtilmemiş")))
                .cleanedText(safeText)

                // yeni koddan gelen alanlar (entity’de varsa)
                .requiredSkills(safeTruncate(skillsStr, 2000))
                .responsibilities(safeTruncate(respStr, 2000))

                .analysisReport(safeTruncate(report, 4000))
                .createdAt(new Date())
                .build());
    }

    private List<String> getUserSkillsNormalized(User user) {
        if (user.getProfile() == null || user.getProfile().getSkills() == null) return new ArrayList<>();
        return user.getProfile().getSkills().stream()
                .map(s -> s.getSkillName().toLowerCase(Locale.forLanguageTag("tr")).trim())
                .collect(Collectors.toList());
    }

    private boolean isUserHasSkill(List<String> userSkills, String req) {
        String reqL = req.toLowerCase(Locale.forLanguageTag("tr")).trim();
        return userSkills.stream().anyMatch(us -> us.contains(reqL) || reqL.contains(us));
    }

    // ==============================
    // YENİ yardımcılar (bozmadan eklendi)
    // ==============================
    private String safeTruncate(String value, int length) {
        if (value == null) return "";
        String v = value.trim();
        return v.length() > length ? v.substring(0, Math.max(0, length - 3)) + "..." : v;
    }

    private String emptyToUnspecified(String s) {
        if (s == null) return "Belirtilmemiş";
        String t = s.trim();
        if (t.isEmpty()) return "Belirtilmemiş";
        if (t.equalsIgnoreCase("null")) return "Belirtilmemiş";
        return t;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.trim().isEmpty() && !"null".equalsIgnoreCase(primary.trim())) return primary.trim();
        return fallback;
    }

    // ==============================
    // Interface methods (eski gibi)
    // ==============================
    @Override
    public List<JobPosting> getJobsByUserId(Long userId) {
        return jobPostingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public JobPosting getJobById(Long jobId) {
        return jobPostingRepository.findById(jobId).orElseThrow();
    }

    @Override
    public JobAnalysisResponse analyzeJobPosting(Object o, String url) {
        return analyzeJobPosting((Long) o, url);
    }

    @Override
    @Transactional(readOnly = true)
    public MarketAnalysisResponse performMarketAnalysis(String area, Long userId) {
        // 1. Kullanıcıyı bul
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı: " + userId));
        
        UserProfile profile = user.getProfile();
        if (profile == null) {
            throw new IllegalArgumentException("Kullanıcı profili bulunamadı. Lütfen önce profil oluşturun.");
        }

        // 2. Area'yı belirle: eğer null ise kullanıcının department veya title'ından al
        boolean isAutoAnalyzed = false;
        String analysisArea = area;
        
        if (analysisArea == null || analysisArea.trim().isEmpty()) {
            isAutoAnalyzed = true;
            // Önce department, yoksa title'dan al
            if (profile.getDepartment() != null && !profile.getDepartment().trim().isEmpty()) {
                analysisArea = profile.getDepartment().trim();
            } else if (profile.getTitle() != null && !profile.getTitle().trim().isEmpty()) {
                analysisArea = profile.getTitle().trim();
            } else {
                throw new IllegalArgumentException("Analiz alanı belirlenemedi. Lütfen alan adını belirtin veya profil bilgilerinizi güncelleyin.");
            }
        } else {
            analysisArea = area.trim();
        }

        log.info("Pazar analizi başlatılıyor - Area: {}, UserId: {}, AutoAnalyzed: {}", 
                analysisArea, userId, isAutoAnalyzed);

        // 3. İlgili iş ilanlarını çek (position'a göre arama yap) - En az 100 ilan için
        List<JobPosting> relevantJobs = jobPostingRepository
                .findTop200ByPositionContainingIgnoreCaseOrderByCreatedAtDesc(analysisArea);

        // Eğer yeterli ilan yoksa (100'den az), tüm ilanları çekip filtreleme yapabiliriz
        if (relevantJobs.isEmpty() || relevantJobs.size() < 100) {
            log.warn("Position'a göre yeterli ilan bulunamadı ({}), tüm ilanlar kontrol ediliyor (en az 100 ilan hedefleniyor)...", relevantJobs.size());
            // Tüm ilanları çek ve içerikte area'yı ara
            List<JobPosting> allJobs = jobPostingRepository.findAll();
            relevantJobs = filterJobsByArea(allJobs, analysisArea);
            // En az 100 ilan alabilmek için limit'i 200'e çıkar
            relevantJobs = relevantJobs.stream()
                    .sorted((a, b) -> {
                        Date dateA = a.getCreatedAt() != null ? a.getCreatedAt() : new Date(0);
                        Date dateB = b.getCreatedAt() != null ? b.getCreatedAt() : new Date(0);
                        return dateB.compareTo(dateA);
                    })
                    .limit(200)
                    .collect(Collectors.toList());
        }

        // Kullanıcının mevcut becerilerini al
        List<String> userSkills = getUserSkillsNormalized(user);
        
        Map<String, Integer> skillFrequency;
        List<String> missingSkills;
        String aiRecommendation;

        // Eğer yeterli ilan yoksa (100'den az), AI'dan genel pazar analizi al
        if (relevantJobs.isEmpty() || relevantJobs.size() < 100) {
            log.warn("'{}' alanı için yeterli ilan bulunamadı ({}), AI'dan genel pazar analizi alınıyor (en az 100 ilan gerekiyor)...", 
                    analysisArea, relevantJobs.size());
            
            // Lazy collection'ları initialize et (session açıkken)
            if (profile.getSkills() != null) {
                profile.getSkills().size(); // Initialize
            }
            if (profile.getLanguages() != null) {
                profile.getLanguages().size(); // Initialize
            }
            if (profile.getEducations() != null) {
                profile.getEducations().size(); // Initialize
            }
            
            try {
                // AI'dan genel pazar becerilerini al
                Object skillsJsonObj = aiClient.generateGeneralMarketSkills(analysisArea);
                String skillsJson;
                
                if (skillsJsonObj == null) {
                    skillsJson = "{\"skills\":[]}";
                } else if (skillsJsonObj instanceof String) {
                    skillsJson = (String) skillsJsonObj;
                } else {
                    log.warn("generateGeneralMarketSkills String döndürmedi, tip: {}, değer: {}", 
                        skillsJsonObj.getClass().getName(), skillsJsonObj);
                    skillsJson = String.valueOf(skillsJsonObj);
                }
                
                skillFrequency = parseSkillFrequencyFromAI(skillsJson);
            } catch (Exception e) {
                log.error("AI'dan beceri listesi alınırken hata: ", e);
                skillFrequency = new LinkedHashMap<>();
            }
            
            try {
                // AI'dan genel pazar analizi al
                String aiResult = aiClient.generateGeneralMarketAnalysis(analysisArea, profile);
                if (aiResult != null && !aiResult.trim().isEmpty()) {
                    String lowerResult = aiResult.toLowerCase();
                    // Hata mesajlarını kontrol et
                    if (!lowerResult.contains("şu an gerçekleştirilemiyor") &&
                        !lowerResult.contains("gerçekleştirilemiyor") &&
                        !lowerResult.contains("lütfen daha sonra") &&
                        aiResult.trim().length() > 50) { // En az 50 karakter olmalı
                        aiRecommendation = aiResult;
                        log.info("AI analizi başarıyla alındı, uzunluk: {}", aiResult.length());
                    } else {
                        log.warn("AI'dan hata mesajı veya çok kısa analiz döndü: {}", 
                            aiResult.length() > 100 ? aiResult.substring(0, 100) : aiResult);
                        aiRecommendation = null;
                    }
                } else {
                    log.warn("AI'dan boş analiz döndü");
                    aiRecommendation = null;
                }
            } catch (Exception e) {
                log.error("AI'dan pazar analizi alınırken hata: ", e);
                aiRecommendation = null;
            }
            
            // Eksik becerileri belirle
            missingSkills = findMissingSkills(userSkills, skillFrequency);
        } else {
            log.info("'{}' alanı için {} ilan bulundu (en az 100 ilan gereksinimi karşılandı), veritabanı analizi yapılıyor...", analysisArea, relevantJobs.size());

            // Lazy collection'ları initialize et (session açıkken)
            if (profile.getSkills() != null) {
                profile.getSkills().size(); // Initialize
            }
            if (profile.getLanguages() != null) {
                profile.getLanguages().size(); // Initialize
            }
            if (profile.getEducations() != null) {
                profile.getEducations().size(); // Initialize
            }

            // Veritabanından beceri frekanslarını hesapla
            skillFrequency = calculateSkillFrequency(relevantJobs);

            // Kullanıcının eksik becerilerini belirle
            missingSkills = findMissingSkills(userSkills, skillFrequency);

            // AI ile pazar analizi yap (mevcut ilanlar üzerinden)
            try {
                String aiResult = aiClient.analyzeMarketWithAI(analysisArea, relevantJobs, profile);
                if (aiResult != null && !aiResult.trim().isEmpty()) {
                    String lowerResult = aiResult.toLowerCase();
                    // Hata mesajlarını kontrol et
                    if (!lowerResult.contains("şu an gerçekleştirilemiyor") &&
                        !lowerResult.contains("gerçekleştirilemiyor") &&
                        !lowerResult.contains("lütfen daha sonra") &&
                        aiResult.trim().length() > 50) { // En az 50 karakter olmalı
                        aiRecommendation = aiResult;
                        log.info("AI analizi başarıyla alındı, uzunluk: {}", aiResult.length());
                    } else {
                        log.warn("AI'dan hata mesajı veya çok kısa analiz döndü: {}", 
                            aiResult.length() > 100 ? aiResult.substring(0, 100) : aiResult);
                        aiRecommendation = null;
                    }
                } else {
                    log.warn("AI'dan boş analiz döndü");
                    aiRecommendation = null;
                }
            } catch (Exception e) {
                log.error("AI'dan pazar analizi alınırken hata: ", e);
                aiRecommendation = null;
            }
        }

        // Response oluştur
        return MarketAnalysisResponse.builder()
                .area(analysisArea)
                .userDepartment(profile.getDepartment())
                .userTitle(profile.getTitle())
                .topSkillsInMarket(skillFrequency)
                .userMissingSkills(missingSkills)
                .aiRecommendation(aiRecommendation) // null olabilir, frontend'de kontrol edilecek
                .isAutoAnalyzed(isAutoAnalyzed)
                .build();
    }

    /**
     * İş ilanlarını area'ya göre filtreler (position, requiredSkills, cleanedText alanlarında arama yapar)
     */
    private List<JobPosting> filterJobsByArea(List<JobPosting> allJobs, String area) {
        String areaLower = area.toLowerCase(Locale.forLanguageTag("tr"));
        return allJobs.stream()
                .filter(job -> {
                    String position = job.getPosition() != null ? job.getPosition().toLowerCase(Locale.forLanguageTag("tr")) : "";
                    String skills = job.getRequiredSkills() != null ? job.getRequiredSkills().toLowerCase(Locale.forLanguageTag("tr")) : "";
                    String text = job.getCleanedText() != null ? job.getCleanedText().toLowerCase(Locale.forLanguageTag("tr")) : "";
                    
                    return position.contains(areaLower) || 
                           skills.contains(areaLower) || 
                           text.contains(areaLower);
                })
                .collect(Collectors.toList());
    }

    /**
     * İş ilanlarından beceri frekanslarını hesaplar
     */
    private Map<String, Integer> calculateSkillFrequency(List<JobPosting> jobs) {
        Map<String, Integer> frequency = new HashMap<>();
        
        for (JobPosting job : jobs) {
            if (job.getRequiredSkills() == null || job.getRequiredSkills().trim().isEmpty()) {
                continue;
            }
            
            // RequiredSkills'i parse et (virgül, noktalı virgül veya yeni satırla ayrılmış olabilir)
            String[] skills = job.getRequiredSkills().split("[,;\\n\\r]+");
            
            for (String skill : skills) {
                String trimmed = skill.trim();
                if (!trimmed.isEmpty() && trimmed.length() > 1) {
                    // Normalize et (küçük harfe çevir)
                    String normalized = trimmed.toLowerCase(Locale.forLanguageTag("tr"));
                    frequency.put(normalized, frequency.getOrDefault(normalized, 0) + 1);
                }
            }
        }
        
        // En çok geçen 20 beceriyi döndür (sıralı)
        return frequency.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(20)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
    }

    /**
     * Kullanıcının eksik becerilerini bulur (en çok talep edilen beceriler arasından)
     */
    private List<String> findMissingSkills(List<String> userSkills, Map<String, Integer> marketSkills) {
        List<String> missing = new ArrayList<>();
        
        // En çok talep edilen 15 beceriyi kontrol et
        marketSkills.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(15)
                .forEach(entry -> {
                    String marketSkill = entry.getKey();
                    // Kullanıcının bu beceriye sahip olup olmadığını kontrol et
                    boolean hasSkill = userSkills.stream()
                            .anyMatch(userSkill -> {
                                String userSkillLower = userSkill.toLowerCase(Locale.forLanguageTag("tr"));
                                String marketSkillLower = marketSkill.toLowerCase(Locale.forLanguageTag("tr"));
                                return userSkillLower.contains(marketSkillLower) || marketSkillLower.contains(userSkillLower);
                            });
                    
                    if (!hasSkill) {
                        missing.add(marketSkill);
                    }
                });
        
        return missing;
    }

    /**
     * AI'dan gelen JSON formatındaki beceri verilerini parse eder ve Map'e dönüştürür
     */
    private Map<String, Integer> parseSkillFrequencyFromAI(String skillsJson) {
        Map<String, Integer> skillFrequency = new LinkedHashMap<>();
        
        try {
            // Güvenli null ve boş string kontrolü
            if (skillsJson == null || skillsJson.trim().isEmpty()) {
                skillsJson = "{\"skills\":[]}";
            }
            
            JsonNode root = objectMapper.readTree(skillsJson);
            
            if (root.has("skills") && root.get("skills").isArray()) {
                List<Map.Entry<String, Integer>> skillList = new ArrayList<>();
                
                for (JsonNode skillNode : root.get("skills")) {
                    if (skillNode.has("name")) {
                        String skillName = skillNode.get("name").asText().trim();
                        int frequency = skillNode.has("frequency") ? skillNode.get("frequency").asInt() : 50;
                        
                        if (!skillName.isEmpty()) {
                            skillList.add(new AbstractMap.SimpleEntry<>(skillName, frequency));
                        }
                    }
                }
                
                // Frequency'ye göre sırala (yüksekten düşüğe)
                skillList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
                
                // LinkedHashMap'e ekle (sıralı)
                for (Map.Entry<String, Integer> entry : skillList) {
                    skillFrequency.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            log.error("AI beceri JSON parse hatası: ", e);
            // Hata durumunda boş map döndür
        }
        
        return skillFrequency;
    }
}
