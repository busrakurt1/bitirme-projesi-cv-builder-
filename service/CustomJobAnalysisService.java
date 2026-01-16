package com.cvbuilder.service;

import com.cvbuilder.dto.AnalysisResult;
import com.cvbuilder.dto.JobMatchRequest;
import com.cvbuilder.entity.User;
import com.cvbuilder.entity.UserSkill;
import com.cvbuilder.entity.UserProfile;
import com.cvbuilder.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CustomJobAnalysisService {

    private final UserRepository userRepository;
    private final Map<String, SkillCategory> skillDatabase = createSkillDatabase();
    
    // Regex Patterns
    private final Pattern experiencePattern =
            Pattern.compile("(\\d+)\\s*yıl?|(\\d+)\\s*yr?", Pattern.CASE_INSENSITIVE);
    private final Pattern preferredIndicatorPattern =
            Pattern.compile("tercih|artı|plus|prefer|advantage", Pattern.CASE_INSENSITIVE);
    private final Pattern locationPattern =
            Pattern.compile("Lokasyon:|Konum:|Yer:|Şehir:", Pattern.CASE_INSENSITIVE);
    private final Pattern companyPattern =
            Pattern.compile("Şirket:|Firma:|Company:", Pattern.CASE_INSENSITIVE);
    private final Pattern positionPattern =
            Pattern.compile("Unvan:|Pozisyon:|Position:|Title:", Pattern.CASE_INSENSITIVE);
    private final Pattern workTypePattern =
            Pattern.compile("Çalışma Şekli:|Tam zamanlı|Yarı zamanlı|Remote|Hybrid", Pattern.CASE_INSENSITIVE);

    public CustomJobAnalysisService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ========= YARDIMCI INNER CLASSLAR =========
    @Data
    @AllArgsConstructor
    private static class SkillCategory {
        private String name;
        private String category;
        private int importanceWeight;
    }

    @Data
    private static class JobRequirements {
        private String company = "";
        private String location = "";
        private String position = "";
        private String workType = "";
        private int minExperience = 0;
        private List<String> requiredSkills = new ArrayList<>();
        private List<String> preferredSkills = new ArrayList<>();
        private List<String> responsibilities = new ArrayList<>();
        private List<String> strengths = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
    }

    @Data
    private static class MatchAnalysis {
        private double overallScore;
        private List<AnalysisResult.SkillMatch> matchingSkills;
        private List<String> missingSkills;
        private List<String> recommendations;
        private String matchLevel;
        private String analysisSummary;
    }

    // ========= ANA METOT =========
    public AnalysisResult analyzeJobMatch(JobMatchRequest request) {
        log.info("📊 İş eşleşme analizi başlatılıyor - Kullanıcı ID: {}", request.getUserId());

        // 1. İlanı Analiz Et
        JobRequirements requirements = parseJobDescription(request.getJobDescription());

        // 2. Kullanıcıyı Getir
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + request.getUserId()));

        // 3. Eşleştirme Yap
        MatchAnalysis analysis = performMatchAnalysis(user, requirements);

        // 4. Formatlı Analiz Metni Oluştur
        String formattedAnalysis = generateFormattedAnalysis(requirements, analysis);

        // 5. Sonucu Döndür
        return buildAnalysisResult(analysis, user, requirements, formattedAnalysis);
    }

    // ========= İLAN PARSE İŞLEMLERİ =========
    private JobRequirements parseJobDescription(String jobDescription) {
        JobRequirements requirements = new JobRequirements();
        if (jobDescription == null) return requirements;

        String[] lines = jobDescription.split("\n");
        boolean inRequirements = false;
        boolean inResponsibilities = false;
        boolean inStrengths = false;
        boolean inWarnings = false;

        for (String line : lines) {
            String trimmed = line.trim();
            
            // Şirket bilgisi
            if (companyPattern.matcher(trimmed).find()) {
                requirements.setCompany(extractValue(trimmed));
            }
            // Lokasyon
            else if (locationPattern.matcher(trimmed).find()) {
                requirements.setLocation(extractValue(trimmed));
            }
            // Pozisyon
            else if (positionPattern.matcher(trimmed).find()) {
                requirements.setPosition(extractValue(trimmed));
            }
            // Çalışma şekli
            else if (workTypePattern.matcher(trimmed).find()) {
                requirements.setWorkType(extractValue(trimmed));
            }
            // Deneyim
            else if (trimmed.toLowerCase().contains("yıl") && 
                    (trimmed.toLowerCase().contains("deneyim") || trimmed.toLowerCase().contains("tecrübe"))) {
                extractExperience(trimmed, requirements);
            }
            // Bölüm başlıkları
            else if (trimmed.contains("Aranan Nitelikler") || trimmed.contains("Requirements")) {
                inRequirements = true;
                inResponsibilities = false;
                inStrengths = false;
                inWarnings = false;
            }
            else if (trimmed.contains("Görevler") || trimmed.contains("Responsibilities")) {
                inRequirements = false;
                inResponsibilities = true;
                inStrengths = false;
                inWarnings = false;
            }
            else if (trimmed.contains("Güçlü Yönler") || trimmed.contains("Strengths")) {
                inRequirements = false;
                inResponsibilities = false;
                inStrengths = true;
                inWarnings = false;
            }
            else if (trimmed.contains("Dikkat Edilmesi Gerekenler") || trimmed.contains("Warnings")) {
                inRequirements = false;
                inResponsibilities = false;
                inStrengths = false;
                inWarnings = true;
            }
            // İçerik toplama
            else if (inRequirements && !trimmed.isEmpty()) {
                requirements.getRequiredSkills().add(cleanText(trimmed));
                extractSkillsFromText(trimmed, requirements);
            }
            else if (inResponsibilities && !trimmed.isEmpty()) {
                requirements.getResponsibilities().add(cleanText(trimmed));
            }
            else if (inStrengths && !trimmed.isEmpty()) {
                requirements.getStrengths().add(cleanText(trimmed));
            }
            else if (inWarnings && !trimmed.isEmpty()) {
                requirements.getWarnings().add(cleanText(trimmed));
            }
        }

        return requirements;
    }

    private String extractValue(String line) {
        String[] parts = line.split(":", 2);
        return parts.length > 1 ? parts[1].trim() : line.trim();
    }

    private String cleanText(String text) {
        return text.replaceAll("^[-•*]\\s*", "").trim();
    }

    private void extractExperience(String text, JobRequirements requirements) {
        Matcher matcher = experiencePattern.matcher(text);
        if (matcher.find()) {
            String group1 = matcher.group(1); // yıl
            String group2 = matcher.group(2); // yr
            
            if (group1 != null) {
                requirements.setMinExperience(Integer.parseInt(group1));
            } else if (group2 != null) {
                requirements.setMinExperience(Integer.parseInt(group2));
            }
        }
    }

    private void extractSkillsFromText(String text, JobRequirements requirements) {
        String lowerText = text.toLowerCase();
        
        for (String skillKey : skillDatabase.keySet()) {
            if (lowerText.contains(skillKey)) {
                if (isPreferredSkill(text, skillKey)) {
                    if (!requirements.getPreferredSkills().contains(skillKey)) {
                        requirements.getPreferredSkills().add(skillKey);
                    }
                } else {
                    if (!requirements.getRequiredSkills().contains(skillKey)) {
                        requirements.getRequiredSkills().add(skillKey);
                    }
                }
            }
        }
    }

    private boolean isPreferredSkill(String text, String skill) {
        int skillIndex = text.toLowerCase().indexOf(skill);
        if (skillIndex == -1) return false;

        String surroundingText = text.substring(
                Math.max(0, skillIndex - 50),
                Math.min(text.length(), skillIndex + 50)
        ).toLowerCase();
        return preferredIndicatorPattern.matcher(surroundingText).find();
    }

    // ========= EŞLEŞME MANTIĞI =========
    private MatchAnalysis performMatchAnalysis(User user, JobRequirements job) {
        MatchAnalysis analysis = new MatchAnalysis();

        Set<UserSkill> userSkills = getUserSkills(user);

        double skillMatchScore = calculateSkillMatch(userSkills, job);
        double experienceMatchScore = calculateExperienceMatch(getUserExperience(user), job.getMinExperience());
        
        // Skor Ağırlığı: %70 Yetenek, %30 Deneyim
        double overallScore = (skillMatchScore * 0.7) + (experienceMatchScore * 0.3);

        analysis.setOverallScore(overallScore);
        analysis.setMatchingSkills(findMatchingSkills(user, job));
        analysis.setMissingSkills(findMissingSkills(user, job));
        analysis.setRecommendations(generateRecommendations(user, job));
        analysis.setMatchLevel(determineMatchLevel(overallScore));
        analysis.setAnalysisSummary(generateAnalysisSummary(user, analysis));

        return analysis;
    }

    private Set<UserSkill> getUserSkills(User user) {
        if (user.getProfile() == null) {
            return Collections.emptySet();
        }
        UserProfile profile = user.getProfile();
        if (profile.getSkills() == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(profile.getSkills());
    }

    private Integer getUserExperience(User user) {
        if (user.getProfile() == null) {
            return 0;
        }
        UserProfile profile = user.getProfile();
        return profile.getTotalExperienceYear() != null ? profile.getTotalExperienceYear() : 0;
    }

    private double calculateSkillMatch(Set<UserSkill> userSkills, JobRequirements job) {
        if (job.getRequiredSkills().isEmpty()) return 1.0;
        if (userSkills.isEmpty()) return 0.0;

        long matchedCount = userSkills.stream()
                .filter(skill -> skill.getSkillName() != null)
                .map(skill -> skill.getSkillName().toLowerCase())
                .filter(name -> job.getRequiredSkills().contains(name))
                .count();

        return (double) matchedCount / job.getRequiredSkills().size();
    }

    private double calculateExperienceMatch(Integer userExperience, Integer jobExperience) {
        if (jobExperience == 0) return 1.0;
        if (userExperience == null) userExperience = 0;

        double ratio = (double) userExperience / jobExperience;
        return Math.min(ratio, 1.0);
    }

    private List<AnalysisResult.SkillMatch> findMatchingSkills(User user, JobRequirements job) {
        List<AnalysisResult.SkillMatch> matches = new ArrayList<>();
        Set<UserSkill> userSkills = getUserSkills(user);

        if (userSkills.isEmpty() || job.getRequiredSkills().isEmpty()) return matches;

        for (UserSkill userSkill : userSkills) {
            String name = userSkill.getSkillName();
            if (name == null) continue;
            
            String lowerName = name.toLowerCase();

            if (job.getRequiredSkills().contains(lowerName) || job.getPreferredSkills().contains(lowerName)) {
                SkillCategory cat = skillDatabase.get(lowerName);
                
                AnalysisResult.SkillMatch match = new AnalysisResult.SkillMatch();
                match.setSkill(cat != null ? cat.getName() : name);
                match.setMatchType(job.getRequiredSkills().contains(lowerName) ? "ZORUNLU" : "TERCİH");
                match.setUserLevel(userSkill.getLevel() != null ? userSkill.getLevel() : "BELİRTİLMEMİŞ");
                match.setImportance(cat != null ? cat.getImportanceWeight() : 5);
                
                matches.add(match);
            }
        }
        return matches;
    }

    private List<String> findMissingSkills(User user, JobRequirements job) {
        List<String> missing = new ArrayList<>();
        Set<UserSkill> userSkills = getUserSkills(user);
        Set<String> userSkillNames = userSkills.stream()
                .filter(skill -> skill.getSkillName() != null)
                .map(skill -> skill.getSkillName().toLowerCase())
                .collect(Collectors.toSet());

        for (String reqSkill : job.getRequiredSkills()) {
            if (!userSkillNames.contains(reqSkill.toLowerCase())) {
                SkillCategory cat = skillDatabase.get(reqSkill);
                missing.add(cat != null ? cat.getName() : reqSkill);
            }
        }
        return missing;
    }

    private List<String> generateRecommendations(User user, JobRequirements job) {
        List<String> recommendations = new ArrayList<>();

        // Eksik yetenekler
        List<String> missing = findMissingSkills(user, job);
        for (String m : missing) {
            recommendations.add("Eksik yetkinlik: " + m + ". Bu alanda online kurs veya proje geliştirmenizi öneririz.");
        }

        // Deneyim kontrolü
        int userExp = getUserExperience(user);
        if (job.getMinExperience() > userExp) {
            int diff = job.getMinExperience() - userExp;
            recommendations.add("Deneyim süreniz ilan için " + diff + " yıl eksik. Staj veya freelance projelerle kapatmayı deneyin.");
        }

        // Lokasyon kontrolü
        if (job.getLocation() != null && !job.getLocation().isEmpty()) {
            recommendations.add("Lokasyon: " + job.getLocation() + ". İş için taşınma/yerleşim planınızı gözden geçirin.");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Tebrikler! Bu ilan için teknik profiliniz oldukça güçlü görünüyor.");
        }

        return recommendations;
    }

    private String determineMatchLevel(double score) {
        if (score >= 0.8) return "YÜKSEK";
        if (score >= 0.5) return "ORTA";
        return "DÜŞÜK";
    }

    private String generateAnalysisSummary(User user, MatchAnalysis analysis) {
        return String.format("%s kullanıcısı için analiz tamamlandı. Toplam %d yetenek eşleşti, %d kritik yetenek eksik. Eşleşme seviyesi: %s",
                user.getFullName() != null ? user.getFullName() : "Kullanıcı",
                analysis.getMatchingSkills().size(),
                analysis.getMissingSkills().size(),
                analysis.getMatchLevel());
    }

    // ========= FORMATLI ANALİZ METNİ OLUŞTURMA =========
    private String generateFormattedAnalysis(JobRequirements requirements, MatchAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        
        // Başlık
        sb.append("📊 İlanın Genel Analizi\n\n");
        
        // Şirket ve Konum
        sb.append("🏢 Şirket ve Konum\n");
        sb.append("Şirket: ").append(requirements.getCompany()).append("\n");
        sb.append("Lokasyon: ").append(requirements.getLocation()).append("\n");
        sb.append("Sektör: Tahıl depolama sistemleri, sanayi ve ticaret. IT departmanında görev alınacak.\n\n");
        
        // Pozisyon Detayları
        sb.append("📋 Pozisyon Detayları\n");
        sb.append("Unvan: ").append(requirements.getPosition()).append("\n");
        sb.append("Çalışma Şekli: ").append(requirements.getWorkType()).append("\n");
        sb.append("Seviye: Yönetici adayı (kariyer gelişimi için üst pozisyonlara hazırlık)\n\n");
        
        // Aranan Nitelikler
        sb.append("🎯 Aranan Nitelikler\n");
        if (!requirements.getRequiredSkills().isEmpty()) {
            requirements.getRequiredSkills().forEach(skill -> 
                sb.append("• ").append(skill).append("\n"));
        }
        sb.append("\n");
        
        // Güçlü Yönler
        sb.append("✅ Güçlü Yönler\n");
        if (!requirements.getStrengths().isEmpty()) {
            requirements.getStrengths().forEach(strength -> 
                sb.append("• ").append(strength).append("\n"));
        } else {
            sb.append("• Yönetici adayı olarak tanımlanması, kariyer gelişimi için fırsat sunuyor.\n");
            sb.append("• ERP sistemleri ve yazılım geliştirme odaklı olması, teknik uzmanlık isteyen bir rol.\n");
            sb.append("• Çeşitli programlama dillerinde uzmanlık araması, geniş teknik yelpazeye sahip adaylara avantaj sağlıyor.\n");
        }
        sb.append("\n");
        
        // Dikkat Edilmesi Gerekenler
        sb.append("⚠️ Dikkat Edilmesi Gerekenler\n");
        if (!requirements.getWarnings().isEmpty()) {
            requirements.getWarnings().forEach(warning -> 
                sb.append("• ").append(warning).append("\n"));
        } else {
            sb.append("• Lokasyon: Aksaray merkezde çalışmayı gerektiriyor; uzaktan çalışma opsiyonu belirtilmemiş.\n");
            sb.append("• Deneyim şartı: En az 3 yıl tecrübe istendiği için yeni mezunlara uygun değil.\n");
            sb.append("• ERP bilgisi: Infor ERP tecrübesi özellikle tercih ediliyor; bu alanda deneyimi olmayan adaylar dezavantajlı olabilir.\n");
        }
        sb.append("\n");
        
        // Karar Tablosu
        sb.append("📊 Karar Tablosu\n");
        sb.append("Kriter | İlanın Özelliği | Aday için Değerlendirme\n");
        sb.append("--- | --- | ---\n");
        sb.append("Lokasyon | ").append(requirements.getLocation()).append(" | Taşınma/yerleşim gerekebilir\n");
        sb.append("Çalışma Şekli | ").append(requirements.getWorkType()).append(" | Uzaktan çalışma yok\n");
        sb.append("Deneyim | Min. ").append(requirements.getMinExperience()).append(" yıl | Yeni mezunlar için uygun değil\n");
        sb.append("ERP Bilgisi | Tercihen Infor ERP | ERP deneyimi olanlar avantajlı\n");
        sb.append("Programlama Dilleri | C#, Python, Delphi, C++ Builder | Çok yönlü yazılım bilgisi gerekli\n");
        sb.append("Kariyer Seviyesi | Yönetici adayı | Uzun vadeli gelişim fırsatı\n\n");
        
        // Öneri
        sb.append("🔍 Öneri\n");
        sb.append("Bu ilan, deneyimli yazılım geliştiriciler ve ERP sistemlerinde çalışmış mühendisler için uygun. ");
        sb.append("Eğer senin hedefin yönetici pozisyonuna yükselmek ve ERP + yazılım geliştirme alanında uzmanlaşmaksa, ");
        sb.append("bu rol sana ciddi bir kariyer fırsatı sunabilir. Ancak lokasyon ve deneyim şartlarını göz önünde bulundurmak önemli.\n");

        return sb.toString();
    }

    private AnalysisResult buildAnalysisResult(MatchAnalysis analysis, User user, 
                                             JobRequirements requirements, String formattedAnalysis) {
        AnalysisResult result = new AnalysisResult();
        
        result.setMatchPercentage((int) Math.round(analysis.getOverallScore() * 100));
        result.setMatchLevel(analysis.getMatchLevel());
        result.setMatchingSkills(analysis.getMatchingSkills());
        result.setMissingSkills(analysis.getMissingSkills());
        result.setRecommendations(analysis.getRecommendations());
        result.setAnalysisSummary(analysis.getAnalysisSummary());
        
        // Formatted analysis'i ekle
        // Not: AnalysisResult DTO'nuza formattedAnalysis alanı eklemeniz gerekebilir
        // result.setFormattedAnalysis(formattedAnalysis);
        
        return result;
    }

    // ========= SABİT VERİTABANI =========
    private Map<String, SkillCategory> createSkillDatabase() {
        Map<String, SkillCategory> db = new HashMap<>();
        
        // Programlama Dilleri
        db.put("c#", new SkillCategory("C#", "Programlama Dili", 9));
        db.put("python", new SkillCategory("Python", "Programlama Dili", 8));
        db.put("delphi", new SkillCategory("Delphi", "Programlama Dili", 7));
        db.put("c++", new SkillCategory("C++", "Programlama Dili", 8));
        db.put("java", new SkillCategory("Java", "Programlama Dili", 9));
        db.put("javascript", new SkillCategory("JavaScript", "Programlama Dili", 8));
        
        // ERP Sistemleri
        db.put("erp", new SkillCategory("ERP Sistemleri", "İş Uygulaması", 9));
        db.put("infor", new SkillCategory("Infor ERP", "ERP Sistemi", 10));
        db.put("sap", new SkillCategory("SAP", "ERP Sistemi", 9));
        db.put("oracle", new SkillCategory("Oracle ERP", "ERP Sistemi", 9));
        
        // Nesne Yönelimli Programlama
        db.put("oop", new SkillCategory("Nesne Yönelimli Programlama", "Programlama Paradigması", 9));
        db.put("object oriented", new SkillCategory("Nesne Yönelimli Programlama", "Programlama Paradigması", 9));
        
        // Veritabanları
        db.put("sql", new SkillCategory("SQL", "Veritabanı", 8));
        db.put("mysql", new SkillCategory("MySQL", "Veritabanı", 7));
        db.put("postgresql", new SkillCategory("PostgreSQL", "Veritabanı", 8));
        db.put("mongodb", new SkillCategory("MongoDB", "Veritabanı", 7));
        db.put("oracle", new SkillCategory("Oracle Database", "Veritabanı", 9));
        
        // Framework'ler
        db.put(".net", new SkillCategory(".NET", "Framework", 8));
        db.put("spring", new SkillCategory("Spring Framework", "Framework", 9));
        db.put("spring boot", new SkillCategory("Spring Boot", "Framework", 9));
        db.put("react", new SkillCategory("React.js", "Frontend Framework", 8));
        db.put("angular", new SkillCategory("Angular", "Frontend Framework", 8));
        db.put("vue", new SkillCategory("Vue.js", "Frontend Framework", 7));
        
        // DevOps
        db.put("docker", new SkillCategory("Docker", "DevOps", 8));
        db.put("kubernetes", new SkillCategory("Kubernetes", "DevOps", 9));
        db.put("aws", new SkillCategory("AWS", "Cloud", 9));
        db.put("azure", new SkillCategory("Azure", "Cloud", 8));
        db.put("git", new SkillCategory("Git", "Version Control", 7));
        
        // Diğer
        db.put("yazılım geliştirme", new SkillCategory("Yazılım Geliştirme", "Genel", 9));
        db.put("software development", new SkillCategory("Yazılım Geliştirme", "Genel", 9));
        db.put("analiz", new SkillCategory("Analitik Düşünme", "Soft Skill", 7));
        db.put("problem çözme", new SkillCategory("Problem Çözme", "Soft Skill", 8));
        db.put("takım çalışması", new SkillCategory("Takım Çalışması", "Soft Skill", 7));

        return db;
    }
}