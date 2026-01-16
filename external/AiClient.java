package com.cvbuilder.external;

import com.cvbuilder.dto.OptimizedCvItem;
import com.cvbuilder.dto.UserCertificateDTO;
import com.cvbuilder.dto.UserEducationDTO;
import com.cvbuilder.dto.UserLanguageDTO;
import com.cvbuilder.entity.JobPosting;
import com.cvbuilder.entity.UserProfile;
import com.cvbuilder.entity.UserSkill;
import com.cvbuilder.repository.JobPostingRepository;
import com.cvbuilder.service.TranslationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiClient {

    private final TranslationService translationService;
    private final JobPostingRepository jobPostingRepository; // mevcut kodun içinde var, ileride kullanılıyor olabilir
    private final ObjectMapper objectMapper;

    /**
     * İŞ İLANI DETAYLI ANALİZİ - GÖRSELDEKİ TÜM EKSİK BİLGİLER İÇİN
     * (Yeni koddan entegre edildi)
     */
    public Map<String, Object> analyzeJobPostingDetailed(String rawJobText) {
        if (rawJobText == null || rawJobText.isBlank()) return createEmptyResponse();

        String prompt = String.format("""
                SEN BİR VERİ AYIKLAMA SİSTEMİSİN. Aşağıdaki iş ilanı metninden istenen alanları kesinlikle ayıkla ve SADECE JSON döndür.
                                
                ÖZELLİKLE ŞU ÜÇ BİLGİYİ METİN İÇİNDEN BUL:
                1. location: Şehir/İlçe bilgisi.
                2. workType: Hibrit, Uzaktan (Remote), Tam Zamanlı gibi çalışma modeli.
                3. experienceLevel: Stajyer, Junior, Senior gibi deneyim beklentisi.

                ANALİZ EDİLECEK METİN:
                %s
                                
                DOLDURULACAK JSON ŞEMASI (Asla açıklama yapma, sadece JSON):
                {
                  "position": "İş başlığı",
                  "company": "Şirket adı",
                  "location": "Konum bilgisi (örn: İstanbul, Türkiye)",
                  "workType": "Çalışma modeli (örn: Hibrit veya Remote)",
                  "experienceLevel": "Aranan tecrübe (örn: 0-2 Yıl veya Stajyer)",
                  "educationLevel": "Eğitim kriteri",
                  "technicalSkills": ["skill1", "skill2"],
                  "responsibilities": ["görev1", "görev2"],
                  "summary": "İşin 2 cümlelik özeti"
                }
                """, rawJobText);

        try {
            String response = translationService.generateContent(prompt);
            String cleanJson = extractJsonFromResponse(response);
            return objectMapper.readValue(cleanJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("AI JSON Ayrıştırma Hatası: ", e);
            return createEmptyResponse();
        }
    }

    private String extractJsonFromResponse(String response) {
        if (response == null) return "{}";
        // Regex: ilk '{' ve son '}' karakterleri arasındaki bloğu alır
        String clean = response.replaceAll("(?s)^.*?(\\{.*\\}).*$", "$1").trim();
        return clean.isEmpty() ? "{}" : clean;
    }

    private Map<String, Object> createEmptyResponse() {
        Map<String, Object> map = new HashMap<>();
        String def = "Belirtilmemiş";
        map.put("position", def);
        map.put("company", "Bilinmiyor");
        map.put("location", def);
        map.put("workType", def);
        map.put("experienceLevel", def);
        map.put("educationLevel", def);
        map.put("technicalSkills", new ArrayList<String>());
        map.put("responsibilities", new ArrayList<String>());
        map.put("summary", "");
        return map;
    }

    /**
     * BİREYSEL İŞ ANALİZİ: Aday profili ile iş ilanını ATS kriterlerine göre karşılaştırır.
     */
    public String analyzeJobSubmission(UserProfile user, String rawJobText) {
        String userContext = formatUserProfile(user);
        String jobText = safe(rawJobText);

        String prompt = """
              SEN DÜNYA STANDARTLARINDA BİR KIDEMLİ TEKNİK RECRUITER VE STRATEJİK İŞ ANALİSTİSİN.
Görevin, adayın profilini bir büyüteç altına alarak iş ilanıyla "Semantik (Anlamsal)" bir karşılaştırma yapmaktır. 

### ANALİZ TALİMATLARI:
1. **Derin Karşılaştırma:** Sadece anahtar kelime eşleşmesine bakma. Adayın iş deneyimlerindeki sorumluluklarını, iş ilanındaki "Sorumluluklar" maddeleriyle eşleştir. 
2. **Kritiklik Seviyesi:** İlandaki teknolojileri "Kritik", "Destekleyici" ve "Yumuşak Beceriler" olarak sınıflandır ve analizi buna göre yap.
3. **Dil ve Kültür:** Adayın dil seviyesinin (Örn: B2), ilandaki teknik dökümantasyon okuma veya toplantı yönetme ihtiyacını karşılayıp karşılamayacağını yorumla.
4. **Çıkarım Yap:** Eğer aday "Spring Boot" biliyorsa, onun "Microservices" ve "Java" ekosistemine hakim olduğunu varsayarak yetkinlik skorunu buna göre işle.
5) ÇIKARIM YAP (INFERENCE): Eğer aday "Veritabanı süreçlerini yönettim" diyorsa, doğrudan belirtmese bile 'SQL' bildiğini varsay ve bunu "Eşleşenler" kısmında "Tecrübeden çıkarılmıştır" notuyla belirt.
6) GRUPLAMA YAP: "Microsoft Office", "Excel" ve "Powerpoint" gibi yetenekleri tek tek saymak yerine "Ofis Teknolojileri Uyumlu" şeklinde stratejik bir başlıkta birleştir.
7) SKORLAMA: Adayın bu işi yapıp yapamayacağına dair 100 üzerinden bir 'Yeterlilik Skoru' belirle.

---
### ÇIKTI FORMATI:

### 📊 Detaylı Teknik Uyumluluk Analizi
- [Stratejik Yorum]: Adayın kariyer yolculuğu bu pozisyonun evrimiyle ne kadar örtüşüyor? (En az 5 cümlelik, teknik derinliği olan bir paragraf).
- [ATS Puanı Tahmini]: 100 üzerinden bir uyum skoru ver ve nedenini açıkla.

### ✅ Eşleşen Teknik Yetkinlikler ve Deneyim Transferi
- (Adayın sahip olduğu bir yeteneğin, ilandaki tam olarak hangi problemi çözeceğini açıkla. Örn: "Adayın X projesindeki tecrübesi, ilandaki Y sisteminin kurulması için kritik önemde.")
- (En az 6 detaylı madde)

### ⚠️ Kritik Yetkinlik Boşlukları ve Operasyonel Riskler
- (Sadece eksik listesi değil; bu eksiğin işe alım sonrası oryantasyon süresini nasıl etkileyeceğini belirt.)
- (En az 6 detaylı madde)

### 💡 Mülakat İçin Teknik Soru Önerileri
- (Adayın profilinde belirsiz kalan veya ilanda çok kritik olan noktalar için adaya sorulması gereken 3 teknik soru hazırla.)

### 🎯 Teknik Sonuç ve Başvuru Durumu
- **DURUM:** [UYGUN / KISMEN UYGUN / RİSKLİ / UYGUN DEĞİL]
- **GEREKÇE:** (Verilere dayalı, nihai profesyonel karar özeti.)
---

[Aday Profili]
%s

[İş İlanı]
%s
                """.formatted(userContext, jobText);

        try {
            return translationService.generateContent(prompt);
        } catch (Exception e) {
            log.error("AI Analiz Hatası: ", e);
            return "Analiz servisine şu anda ulaşılamıyor.";
        }
    }

    /**
     * PAZAR ANALİZİ: Belirli bir uzmanlık alanı için toplanan verileri adayın profiliyle kıyaslar.
     */
    public String performMarketTrendAnalysis(String area, String aggregatedJobData, UserProfile userProfile) {
        String userContext = formatUserProfileForMarketAnalysis(userProfile);

        String prompt = """
            SEN ÜST DÜZEY BİR TEKNOLOJİ PAZAR ANALİSTİ VE KARİYER DANIŞMANISIN.
            Aşağıdaki veriler, veritabanında bulunan son iş ilanlarından derlenmiştir.

            GÖREVİN:
            1. '%s' alanıyla ilgili tüm iş ilanlarını otomatik olarak tespit et
            2. Bu ilanlardaki beceri trendlerini analiz et
            3. Adayın mevcut profiliyle karşılaştır
            4. Kişiselleştirilmiş gelişim önerileri sun

            ÖNEMLİ KURALLAR:
            - İlanları sadece başlıkla değil, içerikte geçen teknoloji ve becerilere göre filtrele
            - "Machine Learning" aranıyorsa "Yapay Zeka", "AI", "Veri Bilimi" gibi ilgili terimleri de dikkate al
            - İstatistiksel analiz yap: "100 ilanın 85'inde Python gerekiyor (%85)"
            - Somut ve ölçülebilir öneriler sun

            ÇIKTI FORMATI (TÜRKÇE):

            ### 📊 GENEL PAZAR DURUMU
            - Toplam analiz edilen ilan sayısı: [sayı]
            - '%s' ile ilgili bulunan ilan sayısı: [sayı]
            - Pazar büyüklüğü ve talep eğilimleri

            ### 🔥 EN ÇOK TALEP EDİLEN 10 BECERİ
            1. [Beceri 1] - [%X] oranında talep ediliyor
            2. [Beceri 2] - [%Y] oranında talep ediliyor
            ...

            ### ✅ PROFİLİNİZLE EŞLEŞEN BECERİLER
            - [Beceri 1]: Bu beceriye sahipsiniz - pazar değerinizi artırıyor ✓
            - [Beceri 2]: ...

            ### ⚠️ KRİTİK EKSİK BECERİLERİNİZ
            - [Beceri 1]: %[X] talep oranı - ÖNCELİKLİ ÖĞRENMENİZ GEREKİYOR
            - [Beceri 2]: %[Y] talep oranı - ÖNEMLİ BİR EKSİK
            ...

            ### 🎯 SİZE ÖZEL GELİŞİM YOL HARİTASI
            - İLK 3 AY: [En kritik 3 beceri]
            - 3-6 AY: [Orta vadeli hedefler]
            - 6-12 AY: [Uzun vadeli uzmanlaşma]

            ### 💎 SİZİ ÖNE ÇIKARACAK "KILLER SKILLS"
            - [Niche beceri 1]: Neden önemli?
            - [Niche beceri 2]: Rakiplerden farkınız

            ### 📚 ÖNERİLEN ÖĞRENME KAYNAKLARI
            - [Beceri 1 için]: [Kurs/Kaynak önerisi]
            - [Beceri 2 için]: [Kurs/Kaynak önerisi]

            [TÜM İLAN VERİLERİ]
            %s

            [ADAY PROFİLİ]
            %s
            """.formatted(area, area, aggregatedJobData, userContext);

        try {
            return translationService.generateContent(prompt);
        } catch (Exception e) {
            log.error("Pazar Analizi Hatası: ", e);
            return "Pazar analizi şu an gerçekleştirilemiyor.";
        }
    }

    public String formatAllJobPostingsForAI(List<JobPosting> allJobs) {
        StringBuilder sb = new StringBuilder();
        sb.append("TOPLAM İLAN SAYISI: ").append(allJobs == null ? 0 : allJobs.size()).append("\n\n");

        if (allJobs == null) return sb.toString();

        for (int i = 0; i < Math.min(allJobs.size(), 100); i++) {
            JobPosting job = allJobs.get(i);
            sb.append("--- İLAN ").append(i + 1).append(" ---\n");
            sb.append("POZİSYON: ").append(safe(job.getPosition())).append("\n");
            sb.append("GEREKLİ BECERİLER: ").append(safe(job.getRequiredSkills())).append("\n");
            String cleaned = safe(job.getCleanedText());
            sb.append("AÇIKLAMA: ")
              .append(cleaned.substring(0, Math.min(500, cleaned.length())))
              .append("...\n\n");
        }

        return sb.toString();
    }

    private String formatUserProfileForMarketAnalysis(UserProfile user) {
        if (user == null) return "Profil bilgisi bulunamadı.";

        StringBuilder sb = new StringBuilder();
        sb.append("=== TEMEL BİLGİLER ===\n");
        sb.append("Başlık/Uzmanlık: ").append(safe(user.getTitle())).append("\n");
        if (user.getTotalExperienceYear() != null) {
            sb.append("Toplam Deneyim: ").append(user.getTotalExperienceYear()).append(" yıl\n");
        }

        sb.append("\n=== TEKNİK BECERİLER ===\n");
        if (user.getSkills() != null && !user.getSkills().isEmpty()) {
            user.getSkills().forEach(skill -> sb.append("- ").append(safe(skill.getSkillName())).append("\n"));
        } else {
            sb.append("Belirtilmemiş\n");
        }

        sb.append("\n=== DİL BİLGİSİ ===\n");
        if (user.getLanguages() != null && !user.getLanguages().isEmpty()) {
            user.getLanguages().forEach(lang -> sb.append("- ").append(safe(lang.getLanguage()))
                    .append(" (").append(safe(lang.getLevel())).append(")\n"));
        } else {
            sb.append("Belirtilmemiş\n");
        }

        sb.append("\n=== EĞİTİM ===\n");
        if (user.getEducations() != null && !user.getEducations().isEmpty()) {
            user.getEducations().forEach(edu -> sb.append("- ").append(safe(edu.getDepartment()))
                    .append(", ").append(safe(edu.getSchoolName()))
                    .append(" (").append(safe(edu.getDegree())).append(")\n"));
        } else {
            sb.append("Belirtilmemiş\n");
        }

        return sb.toString();
    }

    /**
     * Universal JSON analiz (JobAnalysisServiceImpl'in kullandığı metot)
     */
    public String analyzeJobPostingUniversal(String rawJobText) {
        if (rawJobText == null) rawJobText = "";

        String prompt = String.format(
                "SEN KIDEMLI BIR TEKNIK RECRUITER + IS ANALISTISIN.\n" +
                "Asagidaki is ilanini analiz et ve SADECE JSON DONDUR.\n" +
                "JSON DISINDA HICBIR SEY YAZMA. Markdown yok. Kod blogu yok.\n\n" +
                "JSON SCHEMA:\n" +
                "{\n" +
                "  \"position\": \"...\",\n" +
                "  \"company\": \"...\",\n" +
                "  \"location\": \"...\",\n" +
                "  \"workType\": \"...\",\n" +
                "  \"experienceLevel\": \"...\",\n" +
                "  \"educationLevel\": \"...\",\n" +
                "  \"militaryStatus\": \"...\",\n" +
                "  \"languages\": [\"...\"],\n" +
                "  \"salary\": \"...\",\n" +
                "  \"summary\": \"...\",\n" +
                "  \"technicalSkills\": [\"...\"],\n" +
                "  \"responsibilities\": [\"...\"]\n" +
                "}\n\n" +
                "IS ILANI METNI:\n%s\n",
                rawJobText
        );

        try {
            String response = translationService.generateContent(prompt);
            if (response == null) return "{}";
            return response.replaceAll("```json|```", "").trim();
        } catch (Exception e) {
            log.error("AI JSON Analiz Hatası: ", e);
            return "{}";
        }
    }

    public List<String> generateTailoredSummaries(UserProfile profile, String jobContext) {
        String rawTitle = profile != null ? safe(profile.getTitle()) : "Profesyonel";
        String userTitle = toTitleCase(rawTitle);
        String skills = getPrioritizedSkills(profile);
        int years = (profile != null && profile.getTotalExperienceYear() != null) ? profile.getTotalExperienceYear() : 0;

        List<String> summaries = new ArrayList<>();

        // 1-2: ŞABLON ÖZETLERİ (Kullanıcı deneyim bilgileriyle doldurulmuş)
        String experienceAreas = getExperienceAreas(profile);
        String template1 = buildTemplateSummary1(userTitle, years, skills, experienceAreas);
        String template2 = buildTemplateSummary2(userTitle, years, skills, experienceAreas);
        summaries.add(template1);
        summaries.add(template2);

        // 3-7: AI ÖZETLERİ (CV ve iş ilanı karşılaştırmalı, 10-12 cümlelik, AI kalıntısı olmadan)
        try {
            String userContextForAI = formatUserProfileForSummary(profile);
            List<String> aiSummaries = generateAISummaries(userContextForAI, jobContext, userTitle, years, skills);
            if (aiSummaries != null && !aiSummaries.isEmpty()) {
                summaries.addAll(aiSummaries);
            } else {
                log.warn("AI özetler boş döndü, fallback kullanılıyor");
                // Fallback: Detaylı özetler ekle
                summaries.addAll(generateFallbackSummaries(userTitle, years, skills, experienceAreas));
            }
        } catch (Exception e) {
            log.error("AI Özet oluşturma hatası: {}", e.getMessage(), e);
            // Fallback: Detaylı özetler ekle
            summaries.addAll(generateFallbackSummaries(userTitle, years, skills, experienceAreas));
        }

        // Toplam 7 özet olmalı
        while (summaries.size() < 7) {
            summaries.add(template1); // Eksikse şablonu tekrarla
        }

        return summaries.subList(0, Math.min(7, summaries.size()));
    }

    /**
     * Şablon Özet 1: Deneyim odaklı (15 cümlelik)
     */
    private String buildTemplateSummary1(String title, int years, String skills, String experienceAreas) {
        String yearText = years > 0 ? years + " yıl" : "Yeni mezun";
        String expText = experienceAreas.isEmpty() ? "çeşitli projeler" : experienceAreas;
        
        return String.format(
            "%s deneyime sahip bir %s olarak, %s konularında derinlemesine bilgi ve pratik deneyim kazandım. " +
            "Kariyerim boyunca %s üzerinde çalışarak teknik yetkinliğimi sürekli geliştirdim. " +
            "Özellikle %s alanlarında uzmanlaşmış durumdayım ve bu yetkinliklerimi yeni projelerde etkin bir şekilde kullanabilirim. " +
            "Takım çalışmasına yatkın, problem çözme odaklı ve sürekli öğrenmeye açık bir profesyonel olarak, " +
            "organizasyonlara değer katmayı ve başarılı sonuçlar elde etmeyi hedefliyorum. " +
            "Karmaşık teknik problemleri analiz edip çözümler üretebilme yeteneği kazandım ve bu yeteneğimi çeşitli projelerde uyguladım. " +
            "Proje yönetimi konusunda deneyimliyim ve ekip içi koordinasyonu sağlayabilirim. " +
            "Teknik bilgimin yanı sıra, iletişim becerilerim ve ekip çalışmasına olan yatkınlığım ile " +
            "projelerin başarıyla tamamlanmasına katkı sağlayabilirim. " +
            "Yeni teknolojilere adapte olma konusunda hızlıyım ve sürekli kendimi geliştirmeye devam ediyorum. " +
            "Detaylara dikkat eden, analitik düşünebilen ve sonuç odaklı çalışan bir profesyonel olarak, " +
            "verilen görevleri en iyi şekilde yerine getirmeyi hedefliyorum. " +
            "Sürekli öğrenme ve gelişim konusunda istekliyim ve yeni teknolojilere hızlı adapte olabilirim. " +
            "Kariyerim boyunca edindiğim deneyimler sayesinde, farklı projelerde başarılı sonuçlar elde ettim. " +
            "Organizasyonlara değer katmayı ve kariyerimde ilerlemeyi hedefliyorum.",
            yearText, title, skills, expText, skills
        );
    }

    /**
     * Şablon Özet 2: Kariyer gelişimi odaklı (15 cümlelik)
     */
    private String buildTemplateSummary2(String title, int years, String skills, String experienceAreas) {
        String yearText = years > 0 ? years + " yıl" : "Yeni mezun";
        String expText = experienceAreas.isEmpty() ? "farklı sektörlerde" : experienceAreas + " alanlarında";
        
        return String.format(
            "%s profesyonel deneyime sahip bir %s olarak, %s çalışma fırsatı buldum. " +
            "Bu süreçte %s teknolojilerinde ve metodolojilerinde derinlemesine bilgi edindim. " +
            "Kariyerim boyunca edindiğim deneyimler sayesinde, karmaşık problemleri analiz edip çözümler üretebilme, " +
            "projeleri zamanında ve kaliteli bir şekilde teslim edebilme yeteneği kazandım. " +
            "Teknik bilgimin yanı sıra, iletişim becerilerim ve ekip çalışmasına olan yatkınlığım ile " +
            "organizasyonlara katkı sağlamayı hedefliyorum. " +
            "Problem çözme konusunda güçlü bir altyapıya sahibim ve karmaşık teknik sorunları çözebilirim. " +
            "%s teknolojilerinde uzmanlaşmış durumdayım ve bu yetkinliklerimi yeni projelerde etkin bir şekilde kullanabilirim. " +
            "Takım çalışması ve iletişim becerilerim sayesinde, projelerin başarıyla tamamlanmasına katkı sağlayabilirim. " +
            "Sürekli öğrenme ve gelişim konusunda istekliyim ve yeni teknolojilere hızlı adapte olabilirim. " +
            "Detaylara dikkat eden, analitik düşünebilen ve sonuç odaklı çalışan bir profesyonel olarak, " +
            "verilen görevleri en iyi şekilde yerine getirmeyi hedefliyorum. " +
            "Kariyerim boyunca sürekli öğrenme ve gelişim konusunda istekli oldum ve yeni teknolojilere hızlı adapte oldum. " +
            "Farklı projelerde yer alarak deneyimlerimi artırdım ve teknik yetkinliğimi sürekli geliştirdim. " +
            "Organizasyonlara değer katmayı ve kariyerimde ilerlemeyi hedefliyorum.",
            yearText, title, expText, skills, skills
        );
    }

    /**
     * AI ile 5 adet özet oluştur (10-12 cümlelik, AI kalıntısı olmadan)
     */
    private List<String> generateAISummaries(String userContext, String jobContext, String title, int years, String skills) {
        List<String> aiSummaries = new ArrayList<>();
        
        String prompt = String.format("""
            SEN PROFESYONEL BİR CV YAZARISIN. Aşağıdaki adayın CV'sini ve iş ilanını analiz ederek, 
            ATS (Applicant Tracking System) uyumlu, profesyonel bir CV özeti yaz.
            
            KRİTİK KURALLAR:
            1. Metin TAMAMEN DOĞAL ve İNSAN YAZISI gibi görünmeli - hiçbir AI kalıntısı olmamalı
            2. "AI", "yapay zeka", "oluşturuldu", "üretildi" gibi ifadeler KESİNLİKLE kullanma
            3. Birinci tekil şahıs kullan (ben, bana, benim)
            4. İlk 2 özet için EN AZ 15 CÜMLE yaz, diğerleri için en az 10, en fazla 12 cümle
            5. Her cümle anlamlı ve profesyonel olsun
            6. İş ilanındaki gereksinimleri CV'deki deneyimlerle eşleştir
            7. Teknik terimleri doğal bir şekilde kullan
            8. Metin akıcı ve okunabilir olsun
            
            YAZIM TARZI:
            - Profesyonel ama samimi
            - Somut başarılar ve deneyimler vurgula
            - İş ilanındaki anahtar kelimeleri doğal bir şekilde kullan
            - Fazla tekrar yapma
            - Detaylı ve kapsamlı açıklamalar yap
            
            [ADAY CV BİLGİLERİ]
            %s
            
            [İŞ İLANI BİLGİLERİ]
            %s
            
            LÜTFEN SADECE ÖZET METNİNİ YAZ, başlık, açıklama veya ek bilgi ekleme.
            """, userContext, jobContext);

        // 5 farklı özet oluştur (her biri farklı açıdan)
        String[] perspectives = {
            "Teknik yetkinlikler ve deneyimler üzerine odaklan - EN AZ 15 CÜMLE yaz",
            "Proje yönetimi ve liderlik deneyimlerini vurgula - EN AZ 15 CÜMLE yaz",
            "Problem çözme ve inovasyon yeteneklerini öne çıkar - En az 10 cümle yaz",
            "Takım çalışması ve iletişim becerilerini vurgula - En az 10 cümle yaz",
            "Kariyer gelişimi ve öğrenme isteğini öne çıkar - En az 10 cümle yaz"
        };

        for (int i = 0; i < 5; i++) {
            try {
                String specificPrompt = prompt + "\n\nÖZEL TALİMAT: " + perspectives[i];
                String aiResponse = translationService.generateContent(specificPrompt);
                
                if (aiResponse != null && !aiResponse.isBlank()) {
                    String cleaned = cleanAIText(aiResponse);
                    // İlk 2 özet için 15 cümle, diğerleri için 10-12 cümle
                    if (i < 2) {
                        cleaned = ensureSentenceCount(cleaned, 15, 20);
                    } else {
                        cleaned = ensureSentenceCount(cleaned, 10, 12);
                    }
                    if (!cleaned.isEmpty() && cleaned.length() > 100) {
                        aiSummaries.add(cleaned);
                    }
                }
            } catch (Exception e) {
                log.warn("AI özet {} oluşturulamadı: {}", i + 1, e.getMessage());
            }
        }

        // Eğer yeterli özet oluşturulamadıysa, fallback ekle
        while (aiSummaries.size() < 5) {
            String fallback = generateDetailedFallbackSummary(title, years, skills, aiSummaries.size());
            aiSummaries.add(fallback);
        }

        return aiSummaries.subList(0, Math.min(5, aiSummaries.size()));
    }

    /**
     * Fallback özetleri oluştur (10-12 cümlelik)
     */
    private List<String> generateFallbackSummaries(String title, int years, String skills, String experienceAreas) {
        List<String> fallbacks = new ArrayList<>();
        String yearText = years > 0 ? years + " yıl" : "Yeni mezun";
        String expText = experienceAreas.isEmpty() ? "çeşitli projeler" : experienceAreas;
        
        // Fallback 1: Teknik odaklı
        fallbacks.add(String.format(
            "%s deneyime sahip bir %s olarak, %s konularında derinlemesine bilgi ve pratik deneyim kazandım. " +
            "Kariyerim boyunca %s üzerinde çalışarak teknik yetkinliğimi sürekli geliştirdim. " +
            "Özellikle %s teknolojilerinde uzmanlaşmış durumdayım ve bu yetkinliklerimi yeni projelerde etkin bir şekilde kullanabilirim. " +
            "Karmaşık problemleri analiz edip çözümler üretebilme, projeleri zamanında ve kaliteli bir şekilde teslim edebilme yeteneği kazandım. " +
            "Takım çalışmasına yatkın, problem çözme odaklı ve sürekli öğrenmeye açık bir profesyonel olarak, " +
            "organizasyonlara değer katmayı ve başarılı sonuçlar elde etmeyi hedefliyorum. " +
            "Teknik bilgimin yanı sıra, iletişim becerilerim ve ekip çalışmasına olan yatkınlığım ile " +
            "projelerin başarıyla tamamlanmasına katkı sağlayabilirim. " +
            "Yeni teknolojilere adapte olma konusunda hızlıyım ve sürekli kendimi geliştirmeye devam ediyorum.",
            yearText, title, skills, expText, skills
        ));
        
        // Fallback 2: Proje yönetimi odaklı
        fallbacks.add(String.format(
            "%s profesyonel deneyime sahip bir %s olarak, %s çalışma fırsatı buldum. " +
            "Bu süreçte %s teknolojilerinde ve metodolojilerinde derinlemesine bilgi edindim. " +
            "Kariyerim boyunca edindiğim deneyimler sayesinde, karmaşık problemleri analiz edip çözümler üretebilme, " +
            "projeleri zamanında ve kaliteli bir şekilde teslim edebilme yeteneği kazandım. " +
            "Proje yönetimi konusunda deneyimliyim ve ekip içi koordinasyonu sağlayabilirim. " +
            "Teknik bilgimin yanı sıra, iletişim becerilerim ve ekip çalışmasına olan yatkınlığım ile " +
            "organizasyonlara katkı sağlamayı hedefliyorum. " +
            "Sürekli öğrenme ve gelişim konusunda istekliyim ve yeni teknolojilere hızlı adapte olabilirim. " +
            "Detaylara dikkat eden, analitik düşünebilen ve sonuç odaklı çalışan bir profesyonel olarak, " +
            "verilen görevleri en iyi şekilde yerine getirmeyi hedefliyorum.",
            yearText, title, expText, skills
        ));
        
        // Fallback 3: Problem çözme odaklı
        fallbacks.add(String.format(
            "%s deneyime sahip bir %s olarak, %s alanlarında derinlemesine bilgi ve pratik deneyim kazandım. " +
            "Kariyerim boyunca çeşitli projelerde yer alarak teknik yetkinliğimi sürekli geliştirdim. " +
            "Problem çözme konusunda güçlü bir altyapıya sahibim ve karmaşık teknik sorunları çözebilirim. " +
            "%s teknolojilerinde uzmanlaşmış durumdayım ve bu yetkinliklerimi yeni projelerde etkin bir şekilde kullanabilirim. " +
            "Takım çalışması ve iletişim becerilerim sayesinde, projelerin başarıyla tamamlanmasına katkı sağlayabilirim. " +
            "Sürekli öğrenme ve gelişim konusunda istekliyim ve yeni teknolojilere hızlı adapte olabilirim. " +
            "Detaylara dikkat eden, analitik düşünebilen ve sonuç odaklı çalışan bir profesyonel olarak, " +
            "organizasyonlara değer katmayı hedefliyorum. " +
            "Yeni projelerde yer alarak deneyimlerimi artırmayı ve kariyerimde ilerlemeyi hedefliyorum.",
            yearText, title, skills, skills
        ));
        
        // Fallback 4: Takım çalışması odaklı
        fallbacks.add(String.format(
            "%s profesyonel deneyime sahip bir %s olarak, %s teknolojilerinde uzmanlaşmış durumdayım. " +
            "Kariyerim boyunca çeşitli projelerde yer alarak teknik yetkinliğimi sürekli geliştirdim. " +
            "Takım çalışmasına yatkın bir profesyonel olarak, ekip içi koordinasyonu sağlayabilir ve " +
            "projelerin başarıyla tamamlanmasına katkı sağlayabilirim. " +
            "%s konularında derinlemesine bilgi ve pratik deneyim kazandım. " +
            "Problem çözme, analitik düşünme ve sonuç odaklı çalışma konularında güçlü bir altyapıya sahibim. " +
            "İletişim becerilerim sayesinde, teknik ve teknik olmayan ekipler arasında köprü kurabilirim. " +
            "Sürekli öğrenme ve gelişim konusunda istekliyim ve yeni teknolojilere hızlı adapte olabilirim. " +
            "Organizasyonlara değer katmayı ve kariyerimde ilerlemeyi hedefliyorum.",
            yearText, title, skills, skills
        ));
        
        // Fallback 5: Kariyer gelişimi odaklı
        fallbacks.add(String.format(
            "%s deneyime sahip bir %s olarak, %s alanlarında derinlemesine bilgi ve pratik deneyim kazandım. " +
            "Kariyerim boyunca sürekli öğrenme ve gelişim konusunda istekli oldum ve yeni teknolojilere hızlı adapte oldum. " +
            "%s teknolojilerinde uzmanlaşmış durumdayım ve bu yetkinliklerimi yeni projelerde etkin bir şekilde kullanabilirim. " +
            "Problem çözme, analitik düşünme ve sonuç odaklı çalışma konularında güçlü bir altyapıya sahibim. " +
            "Takım çalışmasına yatkın, iletişim becerileri güçlü bir profesyonel olarak, " +
            "projelerin başarıyla tamamlanmasına katkı sağlayabilirim. " +
            "Karmaşık problemleri analiz edip çözümler üretebilme, projeleri zamanında ve kaliteli bir şekilde teslim edebilme yeteneği kazandım. " +
            "Yeni projelerde yer alarak deneyimlerimi artırmayı ve kariyerimde ilerlemeyi hedefliyorum. " +
            "Organizasyonlara değer katmayı ve başarılı sonuçlar elde etmeyi hedefliyorum.",
            yearText, title, skills, skills
        ));
        
        return fallbacks;
    }

    /**
     * Detaylı fallback özeti oluştur (10-12 cümlelik)
     */
    private String generateDetailedFallbackSummary(String title, int years, String skills, int index) {
        String yearText = years > 0 ? years + " yıl" : "Yeni mezun";
        String[] variations = {
            String.format(
                "%s deneyime sahip bir %s olarak, %s konularında derinlemesine bilgi ve pratik deneyim kazandım. " +
                "Kariyerim boyunca çeşitli projelerde yer alarak teknik yetkinliğimi sürekli geliştirdim. " +
                "Özellikle %s teknolojilerinde uzmanlaşmış durumdayım ve bu yetkinliklerimi yeni projelerde etkin bir şekilde kullanabilirim. " +
                "Karmaşık problemleri analiz edip çözümler üretebilme, projeleri zamanında ve kaliteli bir şekilde teslim edebilme yeteneği kazandım. " +
                "Takım çalışmasına yatkın, problem çözme odaklı ve sürekli öğrenmeye açık bir profesyonel olarak, " +
                "organizasyonlara değer katmayı ve başarılı sonuçlar elde etmeyi hedefliyorum. " +
                "Teknik bilgimin yanı sıra, iletişim becerilerim ve ekip çalışmasına olan yatkınlığım ile " +
                "projelerin başarıyla tamamlanmasına katkı sağlayabilirim. " +
                "Yeni teknolojilere adapte olma konusunda hızlıyım ve sürekli kendimi geliştirmeye devam ediyorum.",
                yearText, title, skills, skills
            ),
            String.format(
                "%s profesyonel deneyime sahip bir %s olarak, %s teknolojilerinde uzmanlaşmış durumdayım. " +
                "Kariyerim boyunca edindiğim deneyimler sayesinde, karmaşık problemleri analiz edip çözümler üretebilme, " +
                "projeleri zamanında ve kaliteli bir şekilde teslim edebilme yeteneği kazandım. " +
                "%s konularında derinlemesine bilgi ve pratik deneyim kazandım. " +
                "Proje yönetimi konusunda deneyimliyim ve ekip içi koordinasyonu sağlayabilirim. " +
                "Teknik bilgimin yanı sıra, iletişim becerilerim ve ekip çalışmasına olan yatkınlığım ile " +
                "organizasyonlara katkı sağlamayı hedefliyorum. " +
                "Sürekli öğrenme ve gelişim konusunda istekliyim ve yeni teknolojilere hızlı adapte olabilirim. " +
                "Detaylara dikkat eden, analitik düşünebilen ve sonuç odaklı çalışan bir profesyonel olarak, " +
                "verilen görevleri en iyi şekilde yerine getirmeyi hedefliyorum.",
                yearText, title, skills, skills
            )
        };
        return variations[index % variations.length];
    }

    /**
     * AI metninden kalıntıları temizle (ATS uyumlu, doğal metin)
     */
    private String cleanAIText(String text) {
        if (text == null || text.isBlank()) return "";
        
        // "Aşağıdaki metin, anlamını bozmadan..." gibi ön ek metinlerini temizle
        text = removeAIPrefixText(text);
        
        // AI kalıntılarını temizle (daha kapsamlı)
        text = text.replaceAll("(?i)\\b(ai|yapay zeka|artificial intelligence|oluşturuldu|üretildi|generated|created by|created with|automatically generated)\\b", "");
        text = text.replaceAll("(?i)\\b(bu metin|bu özet|bu cv|bu özgeçmiş|this text|this summary|this cv|this resume)\\b", "");
        text = text.replaceAll("(?i)\\b(lütfen|please|not:|note:|important:|dikkat:|attention:)\\b", "");
        text = text.replaceAll("(?i)\\b(as an ai|as a language model|i am an ai|ben bir ai|yapay zeka olarak)\\b", "");
        text = text.replaceAll("(?i)\\b(here is|işte|aşağıda|below is|following is)\\b", "");
        
        // Markdown formatlarını temizle
        text = text.replaceAll("```[\\w]*", "");
        text = text.replaceAll("\\*\\*", "");
        text = text.replaceAll("##+", "");
        text = text.replaceAll("^#+\\s*", "");
        text = text.replaceAll("\\*", "");
        text = text.replaceAll("_", "");
        text = text.replaceAll("`", "");
        
        // HTML etiketlerini temizle
        text = text.replaceAll("<[^>]+>", "");
        
        // Özel karakterleri temizle (bazıları)
        text = text.replaceAll("→", "");
        text = text.replaceAll("•", "");
        text = text.replaceAll("✓", "");
        
        // Fazla boşlukları ve satır sonlarını temizle
        text = text.replaceAll("\\s+", " ");
        text = text.replaceAll("\\n+", " ");
        text = text.replaceAll("\\r+", "");
        
        // Başta ve sonda gereksiz karakterleri temizle
        text = text.replaceAll("^[\"'\\(\\)\\[\\]\\{\\}\\s]+", "");
        text = text.replaceAll("[\"'\\(\\)\\[\\]\\{\\}\\s]+$", "");
        
        // Cümle başlarında gereksiz kelimeleri temizle
        text = text.replaceAll("^\\s*(özet|summary|cv|özgeçmiş|resume):\\s*", "");
        
        return text.trim();
    }

    /**
     * Cümle sayısını kontrol et ve gerekirse ayarla
     */
    private String ensureSentenceCount(String text, int min, int max) {
        if (text == null || text.isEmpty()) return text;
        
        String[] sentences = text.split("[.!?]+");
        int count = sentences.length;
        
        if (count < min) {
            // Eksik cümleler ekle (çeşitli cümlelerle)
            StringBuilder sb = new StringBuilder(text.trim());
            if (!sb.toString().endsWith(".") && !sb.toString().endsWith("!") && !sb.toString().endsWith("?")) {
                sb.append(".");
            }
            
            String[] additionalSentences = {
                " Bu alanda sürekli kendimi geliştirmeye devam ediyorum.",
                " Teknik yetkinliğimi artırmak için sürekli öğreniyorum.",
                " Projelerde başarılı sonuçlar elde etmek için çalışıyorum.",
                " Takım çalışması ve iletişim becerilerimi geliştiriyorum.",
                " Yeni teknolojilere adapte olma konusunda hızlıyım.",
                " Problem çözme ve analitik düşünme yeteneklerimi kullanıyorum.",
                " Detaylara dikkat eden ve sonuç odaklı çalışan bir profesyonelim.",
                " Organizasyonlara değer katmayı hedefliyorum.",
                " Kariyerimde ilerlemek için sürekli çalışıyorum.",
                " Teknik bilgimi pratik projelerde uyguluyorum."
            };
            
            for (int i = count; i < min && i - count < additionalSentences.length; i++) {
                sb.append(additionalSentences[(i - count) % additionalSentences.length]);
            }
            
            // Eğer hala eksikse, genel cümleler ekle
            while (sb.toString().split("[.!?]+").length < min) {
                sb.append(" Bu konuda deneyimli ve yetkin bir profesyonelim.");
            }
            
            return sb.toString();
        } else if (count > max) {
            // Fazla cümleleri kısalt
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < max; i++) {
                if (i > 0) sb.append(" ");
                String sentence = sentences[i].trim();
                sb.append(sentence);
                if (!sentence.endsWith(".") && !sentence.endsWith("!") && !sentence.endsWith("?")) {
                    sb.append(".");
                }
            }
            return sb.toString();
        }
        
        return text;
    }

    /**
     * Kullanıcı profilini özet için formatla
     */
    private String formatUserProfileForSummary(UserProfile profile) {
        if (profile == null) return "Profil bilgisi bulunamadı.";
        
        StringBuilder sb = new StringBuilder();
        sb.append("Başlık/Unvan: ").append(safe(profile.getTitle())).append("\n");
        
        if (profile.getTotalExperienceYear() != null) {
            sb.append("Toplam Deneyim: ").append(profile.getTotalExperienceYear()).append(" yıl\n");
        }
        
        sb.append("\nYetenekler: ");
        if (profile.getSkills() != null && !profile.getSkills().isEmpty()) {
            sb.append(profile.getSkills().stream()
                .map(s -> s.getSkillName())
                .collect(Collectors.joining(", ")));
        } else {
            sb.append("Belirtilmemiş");
        }
        sb.append("\n");
        
        if (profile.getExperiences() != null && !profile.getExperiences().isEmpty()) {
            sb.append("\nDeneyimler:\n");
            profile.getExperiences().stream().limit(5).forEach(exp -> {
                sb.append("- ").append(safe(exp.getPosition()))
                  .append(" @ ").append(safe(exp.getCompany()))
                  .append(": ").append(safe(exp.getDescription())).append("\n");
            });
        }
        
        if (profile.getProjects() != null && !profile.getProjects().isEmpty()) {
            sb.append("\nProjeler:\n");
            profile.getProjects().stream().limit(3).forEach(proj -> {
                sb.append("- ").append(safe(proj.getProjectName()))
                  .append(": ").append(safe(proj.getDescription())).append("\n");
            });
        }
        
        return sb.toString();
    }

    /**
     * Kullanıcının deneyim alanlarını çıkar
     */
    private String getExperienceAreas(UserProfile profile) {
        if (profile == null || profile.getExperiences() == null || profile.getExperiences().isEmpty()) {
            return "";
        }
        
        Set<String> areas = new HashSet<>();
        profile.getExperiences().forEach(exp -> {
            String pos = safe(exp.getPosition());
            if (!pos.isEmpty() && pos.length() > 3) {
                areas.add(pos);
            }
        });
        
        return String.join(", ", areas);
    }

    public List<OptimizedCvItem> optimizeExperiences(UserProfile profile, JobPosting job) {
        if (profile == null || profile.getExperiences() == null) return Collections.emptyList();
        String jobSkills = (job != null) ? safe(job.getRequiredSkills()) : "";
        String jobContext = (job != null) ? buildJobContextForOptimization(job) : "";

        return profile.getExperiences().stream().map(exp -> {
            String originalDesc = safe(exp.getDescription());
            String desc = originalDesc;
            // Veri formatı sorunlarını temizle
            desc = cleanDescription(desc);
            
            // Orijinal açıklamadan teknik terimleri çıkar
            String extractedTechs = extractTechnicalTerms(originalDesc);
            
            // Eğer açıklama yoksa veya çok kısaysa AI ile oluştur
            if (desc.isEmpty() || desc.length() < 20 || desc.equals("{") || desc.startsWith("{")) {
                desc = generateExperienceDescription(exp, jobContext, jobSkills, extractedTechs);
            } else {
                if (desc.length() > 10) {
                    // Orijinal açıklamadaki teknik terimleri koru
                    desc = fixGrammarStrict(desc);
                    // Eğer teknik terimler kaybolduysa ekle
                    if (!extractedTechs.isEmpty() && !desc.toLowerCase().contains(extractedTechs.toLowerCase())) {
                        desc = ensureTechnicalTermsInDescription(desc, extractedTechs);
                    }
                }
            }

            // Tam olarak 2 cümle olmalı (mantıklı ve deneyimi yansıtan)
            desc = ensureExactlyTwoSentences(desc);

            String matched = findIntersection(desc, jobSkills);
            if (!matched.isEmpty() && !desc.contains(matched)) {
                desc += " Bu görevde " + matched + " yetkinliklerini aktif olarak kullandım.";
            }

            return new OptimizedCvItem(
                    safe(exp.getPosition()),
                    safe(exp.getCompany()),
                    formatDateRange(exp.getStartDate(), exp.getEndDate()),
                    Collections.singletonList(desc)
            );
        }).collect(Collectors.toList());
    }

    public List<OptimizedCvItem> optimizeProjects(UserProfile profile, JobPosting job) {
        if (profile == null || profile.getProjects() == null) return Collections.emptyList();
        String jobSkills = (job != null) ? safe(job.getRequiredSkills()) : "";
        String jobContext = (job != null) ? buildJobContextForOptimization(job) : "";
        
        return profile.getProjects().stream().map(p -> {
            String originalDesc = safe(p.getDescription());
            String desc = originalDesc;
            // Veri formatı sorunlarını temizle
            desc = cleanDescription(desc);
            
            // Orijinal açıklamadan teknik terimleri çıkar
            String extractedTechs = extractTechnicalTerms(originalDesc);
            
            // Eğer açıklama yoksa veya çok kısaysa AI ile oluştur
            if (desc.isEmpty() || desc.length() < 20 || desc.equals("{") || desc.startsWith("{")) {
                desc = generateProjectDescription(p, jobContext, jobSkills, extractedTechs);
            } else {
                desc = fixGrammarStrict(desc);
                // Eğer teknik terimler kaybolduysa ekle
                if (!extractedTechs.isEmpty() && !desc.toLowerCase().contains(extractedTechs.toLowerCase())) {
                    desc = ensureTechnicalTermsInDescription(desc, extractedTechs);
                }
            }
            
            // Tam olarak 2 cümle olmalı (mantıklı ve projeyi yansıtan)
            desc = ensureExactlyTwoSentences(desc);
            
            return new OptimizedCvItem(
                    safe(p.getProjectName()), "Proje",
                    formatDateRange(p.getStartDate(), (p.getIsOngoing() != null && p.getIsOngoing()) ? null : p.getEndDate()),
                    Collections.singletonList(desc)
            );
        }).collect(Collectors.toList());
    }

    public List<UserEducationDTO> optimizeEducation(UserProfile profile, JobPosting job) {
        if (profile == null || profile.getEducations() == null) return Collections.emptyList();
        return profile.getEducations().stream().map(e -> {
            String schoolName = safe(e.getSchoolName());
            String department = safe(e.getDepartment());
            String degree = safe(e.getDegree());
            
            // Önce süslü parantezleri ve bozuk verileri temizle
            schoolName = cleanDescription(schoolName);
            department = cleanDescription(department);
            degree = cleanDescription(degree);
            
            // Sonra grammar düzeltmesi yap
            if (!schoolName.isEmpty()) schoolName = fixGrammarStrict(schoolName);
            if (!department.isEmpty()) department = fixGrammarStrict(department);
            if (!degree.isEmpty()) degree = fixGrammarStrict(degree);
            
            // Virgül ve süslü parantez kalıntılarını temizle
            degree = degree.replaceAll(",\\s*\\{", "").replaceAll(",\\s*$", "").trim();
            department = department.replaceAll(",\\s*\\{", "").replaceAll(",\\s*$", "").trim();
            
            return UserEducationDTO.builder()
                .id(e.getId())
                .schoolName(schoolName.isEmpty() ? "Belirtilmemiş" : schoolName)
                .department(department.isEmpty() ? "" : department)
                .degree(degree.isEmpty() ? "" : degree)
                .startYear(safe(e.getStartYear()))
                .graduationYear(e.getEndYear())
                .gpa(safe(e.getGpa()))
                .build();
        }).collect(Collectors.toList());
    }

    public List<UserLanguageDTO> optimizeLanguages(UserProfile profile, JobPosting job) {
        if (profile == null || profile.getLanguages() == null) return Collections.emptyList();
        return profile.getLanguages().stream().map(l -> UserLanguageDTO.builder()
                .id(l.getId())
                .language(safe(l.getLanguage()))
                .level(safe(l.getLevel()))
                .build()).collect(Collectors.toList());
    }

    public List<UserCertificateDTO> optimizeCertificates(UserProfile profile, JobPosting job) {
        if (profile == null || profile.getCertificates() == null) return Collections.emptyList();
        return profile.getCertificates().stream().map(c -> UserCertificateDTO.builder()
                .id(c.getId())
                .name(safe(c.getName()))
                .issuer(safe(c.getIssuer()))
                .date(safe(c.getDate()))
                .url(safe(c.getUrl()))
                .build()).collect(Collectors.toList());
    }

    public String getCareerAdvice(String jobTitle) {
        if (jobTitle == null || jobTitle.isBlank()) return "Tavsiye oluşturulamadı.";
        String prompt = "Kariyer danışmanı olarak '" + jobTitle + "' pozisyonu için trendleri ve gelişim önerilerini Türkçe maddeler halinde yaz.";
        try {
            return translationService.generateContent(prompt);
        } catch (Exception e) {
            log.error("Kariyer Tavsiyesi Hatası: ", e);
            return "Kariyer tavsiyesi şu an oluşturulamıyor.";
        }
    }

    public String analyzeMarketWithAI(String area, List<JobPosting> allJobs, UserProfile userProfile) {
        String allJobsFormatted = formatAllJobPostingsForAI(allJobs);
        String userContext = formatUserProfileForMarketAnalysis(userProfile);

        String prompt = """
            SEN ÜST DÜZEY BİR TEKNOLOJİ PAZAR ANALİSTİSİN.
            Aşağıda veritabanındaki tüm iş ilanları ve bir adayın profili var.

            GÖREVİN:
            1. '%s' alanıyla ilgili TÜM iş ilanlarını BUL (sadece başlık değil, içerikteki becerilere göre)
            2. Bu ilanlardaki BECERİ TRENDLERİNİ analiz et
            3. Adayın mevcut becerileriyle KARŞILAŞTIR
            4. Kişiselleştirilmiş GELİŞİM YOL HARİTASI oluştur

            [TÜM İLAN VERİLERİ]
            %s

            [ADAY PROFİLİ]
            %s
            """.formatted(area, allJobsFormatted, userContext);

        try {
            return translationService.generateContent(prompt);
        } catch (Exception e) {
            log.error("AI Pazar Analizi Hatası: ", e);
            return "Pazar analizi şu an gerçekleştirilemiyor. Lütfen daha sonra tekrar deneyin.";
        }
    }

    public String getQuickMarketAnalysis(String area, List<JobPosting> relevantJobs, UserProfile userProfile) {
        String userContext = formatUserProfileForMarketAnalysis(userProfile);
        String jobsSummary = formatJobsSummaryForQuickAnalysis(relevantJobs);

        String prompt = """
            SEN BİR KARİYER KOÇUSUN.
            '%s' alanındaki iş ilanlarını ve adayın profilini analiz et.

            [İLAN ÖZETİ]
            %s

            [ADAY PROFİLİ]
            %s
            """.formatted(area, jobsSummary, userContext);

        try {
            return translationService.generateContent(prompt);
        } catch (Exception e) {
            log.error("Hızlı Pazar Analizi Hatası: ", e);
            return "Hızlı analiz şu an yapılamıyor.";
        }
    }

    private String formatJobsSummaryForQuickAnalysis(List<JobPosting> jobs) {
        if (jobs == null || jobs.isEmpty()) return "Bu alanda ilan bulunamadı.";

        StringBuilder sb = new StringBuilder();
        sb.append("Toplam İlan: ").append(jobs.size()).append("\n\n");

        Map<String, Integer> skillFrequency = new HashMap<>();
        for (JobPosting job : jobs) {
            if (job.getRequiredSkills() != null) {
                String[] skills = job.getRequiredSkills().split("[,;]");
                for (String skill : skills) {
                    String trimmed = skill.trim();
                    if (!trimmed.isEmpty()) {
                        skillFrequency.put(trimmed, skillFrequency.getOrDefault(trimmed, 0) + 1);
                    }
                }
            }
        }

        sb.append("En Çok Geçen Beceriler:\n");
        skillFrequency.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .forEach(entry -> {
                    double percentage = (entry.getValue() * 100.0) / jobs.size();
                    sb.append("- ").append(entry.getKey())
                      .append(": %").append(String.format("%.1f", percentage))
                      .append(" (").append(entry.getValue()).append(" ilan)\n");
                });

        return sb.toString();
    }

    /**
     * GENEL PAZAR ANALİZİ: Veritabanı olmadan, AI'ın genel bilgisiyle meslek için pazar analizi yapar.
     * Bu metod, Türkiye pazarındaki tipik iş ilanlarını ve beceri gereksinimlerini analiz eder.
     */
    public String generateGeneralMarketAnalysis(String area, UserProfile userProfile) {
        String userContext = formatUserProfileForMarketAnalysis(userProfile);

        String prompt = """
            SEN TÜRKİYE İŞ PAZARI UZMANI VE KARİYER DANIŞMANISIN.
            '%s' alanı için Türkiye pazarındaki tipik iş ilanlarını ve beceri gereksinimlerini analiz et.

            GÖREVİN:
            1. Bu meslek/alan için Türkiye'de TİPİK OLARAK ARANAN 15-20 beceriyi listele
            2. Bu becerilerin önem sırasına göre (en çok aranan en üstte) düzenle
            3. Adayın mevcut profilini bu becerilerle karşılaştır
            4. Eksik becerileri belirle ve öncelikli gelişim önerileri sun
            5. Kariyer yol haritası öner

            ÖNEMLİ:
            - Sadece Türkiye pazarına özgü gerçekçi beceriler listele
            - Her meslek için o alana özel teknik becerileri dahil et (örn: Makine Mühendisliği için SolidWorks, AutoCAD)
            - Genel becerileri de dahil et (İngilizce, Proje Yönetimi vb.)
            - Somut, ölçülebilir ve iş dünyasında gerçekten aranan beceriler olsun

            ÇIKTI FORMATI (TÜRKÇE, Markdown formatında):

            ## 📊 %s İçin Pazar Analizi

            ### 🔥 En Çok Aranan Yetkinlikler (Önem Sırasına Göre)

            1. [Beceri 1] - [Kısa açıklama neden önemli]
            2. [Beceri 2] - [Kısa açıklama]
            ...
            15-20. [Beceri]

            ### ✅ Profilinizle Eşleşen Beceriler

            - [Beceri 1]: Bu beceriye sahipsiniz ✓
            - [Beceri 2]: Bu beceriye sahipsiniz ✓
            ...

            ### ⚠️ Eksik Olan Kritik Becerileriniz

            - [Beceri 1]: [Neden önemli ve nasıl öğrenilebilir]
            - [Beceri 2]: [Neden önemli ve nasıl öğrenilebilir]
            ...

            ### 🎯 Gelişim Yol Haritanız

            **İLK 3 AY:**
            - [En kritik 3 beceri]
            - [Öğrenme kaynakları önerileri]

            **3-6 AY:**
            - [Orta vadeli hedefler]
            - [Pratik uygulama önerileri]

            **6-12 AY:**
            - [Uzun vadeli uzmanlaşma alanları]
            - [Sertifikasyon ve ileri eğitim önerileri]

            ### 💡 Ek Öneriler

            [Kariyer gelişimi için ek stratejik tavsiyeler]

            ---

            [ADAY PROFİLİ]
            %s

            Lütfen yukarıdaki formata göre, Türkiye iş pazarı gerçeklerine uygun, detaylı ve kullanışlı bir analiz hazırla.
            """.formatted(area, area, userContext);

        try {
            return translationService.generateContent(prompt);
        } catch (Exception e) {
            log.error("Genel Pazar Analizi Hatası: ", e);
            return "Genel pazar analizi şu an gerçekleştirilemiyor. Lütfen daha sonra tekrar deneyin.";
        }
    }

    /**
     * GENEL PAZAR BECERİLERİ: Meslek için tipik olarak aranan becerileri JSON formatında döndürür.
     * Bu metod, veritabanı olmadan AI'ın genel bilgisiyle beceri listesi üretir.
     */
    public String generateGeneralMarketSkills(String area) {
        String prompt = String.format(
                "SEN TÜRKİYE İŞ PAZARI UZMANISIN.\n" +
                "'%s' alanı için Türkiye'de TİPİK OLARAK ARANAN becerileri analiz et.\n" +
                "SADECE JSON DÖNDÜR, başka açıklama yapma.\n\n" +
                "JSON FORMATI:\n" +
                "{\n" +
                "  \"skills\": [\n" +
                "    {\"name\": \"Beceri Adı\", \"frequency\": 85, \"importance\": \"Yüksek/Orta/Düşük\"},\n" +
                "    {\"name\": \"Beceri Adı\", \"frequency\": 75, \"importance\": \"Yüksek\"}\n" +
                "  ]\n" +
                "}\n\n" +
                "KURALLAR:\n" +
                "- En az 15, en fazla 25 beceri listele\n" +
                "- Frequency: Bu becerinin iş ilanlarında geçme yüzdesi (0-100 arası)\n" +
                "  - Yüksek talep: 70-100\n" +
                "  - Orta talep: 40-69\n" +
                "  - Düşük talep: 10-39\n" +
                "- Importance: Becerinin kritikliği\n" +
                "- Becerileri önem sırasına göre sırala (frequency yüksekten düşüğe)\n" +
                "- Sadece gerçekçi, Türkiye pazarında aranan beceriler ekle\n" +
                "- Mesleğe özel teknik becerileri dahil et\n\n" +
                "MESLEK/ALAN: %s",
                area, area
        );

        try {
            String response = translationService.generateContent(prompt);
            if (response == null) return "{\"skills\":[]}";
            // JSON bloğunu temizle
            String cleanJson = response.replaceAll("```json|```", "").trim();
            // İlk { ve son } arasındaki içeriği al
            int firstBrace = cleanJson.indexOf('{');
            int lastBrace = cleanJson.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                return cleanJson.substring(firstBrace, lastBrace + 1);
            }
            return cleanJson;
        } catch (Exception e) {
            log.error("Genel Pazar Becerileri Hatası: ", e);
            return "{\"skills\":[]}";
        }
    }

    private String formatUserProfile(UserProfile user) {
        if (user == null) return "Profil bilgisi bulunamadı.";

        StringBuilder sb = new StringBuilder();
        sb.append("=== TEMEL BİLGİLER ===\n");
        sb.append("Başlık: ").append(safe(user.getTitle())).append("\n");
        if (user.getTotalExperienceYear() != null) sb.append("Toplam Deneyim: ").append(user.getTotalExperienceYear()).append(" yıl\n");

        sb.append("\n=== ANALİZ İÇİN KRİTİK YETKİNLİKLER (TEKNİK + DİL) ===\n");
        List<String> combinedCapabilities = new ArrayList<>();

        if (user.getLanguages() != null && !user.getLanguages().isEmpty()) {
            user.getLanguages().stream()
                    .map(l -> "DİL: " + safe(l.getLanguage()) + " (Seviye: " + safe(l.getLevel()) + ")")
                    .forEach(combinedCapabilities::add);
        }

        if (user.getSkills() != null) {
            user.getSkills().stream().map(UserSkill::getSkillName).forEach(combinedCapabilities::add);
        }
        sb.append(String.join(", ", combinedCapabilities)).append("\n");

        if (user.getExperiences() != null && !user.getExperiences().isEmpty()) {
            sb.append("\n=== DENEYİM ÖZETİ ===\n");
            user.getExperiences().stream().limit(5).forEach(exp -> sb.append("- ").append(safe(exp.getPosition()))
                    .append(" @ ").append(safe(exp.getCompany()))
                    .append(" (").append(formatDateRange(exp.getStartDate(), exp.getEndDate())).append(")\n"));
        }

        if (user.getEducations() != null && !user.getEducations().isEmpty()) {
            sb.append("\n=== EĞİTİM ===\n");
            user.getEducations().forEach(edu -> sb.append("- ").append(safe(edu.getDepartment()))
                    .append(", ").append(safe(edu.getSchoolName()))
                    .append(" (").append(safe(edu.getDegree())).append(")\n"));
        }

        sb.append("\n=== EK BİLGİLER ===\n");
        String ms = safe(user.getMilitaryStatus());
        if (!ms.isBlank()) sb.append("Askerlik Durumu: ").append(ms).append("\n");

        return sb.toString();
    }

    private String getPrioritizedSkills(UserProfile profile) {
        if (profile == null || profile.getSkills() == null || profile.getSkills().isEmpty()) return "Mesleki Yetkinlikler";
        return profile.getSkills().stream().limit(5).map(UserSkill::getSkillName).collect(Collectors.joining(", "));
    }

    private String fixGrammarStrict(String text) {
        if (text == null || text.length() < 10) return text;
        try {
            String prompt = "Aşağıdaki metni anlamını bozmadan profesyonel bir dille ve imla kurallarına uygun olarak düzelt. " +
                    "Metni daha akıcı ve doğal hale getir, ancak anlamını koru. " +
                    "SADECE düzeltilmiş metni döndür, başka açıklama, ön ek veya ek bilgi ekleme:\n\n" + text;
            String res = translationService.generateContent(prompt);
            if (res != null) {
                res = res.trim();
                // AI'nın eklediği açıklama metinlerini temizle
                res = removeAIPrefixText(res);
                return res;
            }
            return text;
        } catch (Exception e) {
            return text;
        }
    }
    
    /**
     * AI yanıtından "Aşağıdaki metin, anlamını bozmadan..." gibi ön ek metinlerini temizle
     */
    private String removeAIPrefixText(String text) {
        if (text == null || text.isBlank()) return text;
        
        // "Aşağıdaki metin" ile başlayan açıklama metinlerini temizle
        text = text.replaceAll("(?i)^.*?aşağıdaki metin[^:]*:\\s*", "");
        text = text.replaceAll("(?i)^.*?aşağıdaki metni[^:]*:\\s*", "");
        text = text.replaceAll("(?i)^.*?anlamını bozmadan[^:]*:\\s*", "");
        text = text.replaceAll("(?i)^.*?profesyonel bir dille[^:]*:\\s*", "");
        text = text.replaceAll("(?i)^.*?düzeltildi[^:]*:\\s*", "");
        text = text.replaceAll("(?i)^.*?düzeltilmiştir[^:]*:\\s*", "");
        
        // İki nokta üst üste sonrasındaki metni al (eğer varsa)
        int colonIndex = text.indexOf(':');
        if (colonIndex > 0 && colonIndex < text.length() / 2) {
            // Eğer iki nokta üst üste metnin ilk yarısındaysa, muhtemelen açıklama var
            String beforeColon = text.substring(0, colonIndex).toLowerCase();
            if (beforeColon.contains("metin") || beforeColon.contains("düzelt") || beforeColon.contains("profesyonel")) {
                text = text.substring(colonIndex + 1).trim();
            }
        }
        
        return text.trim();
    }

    private String findIntersection(String text, String skills) {
        if (text == null || skills == null || skills.isBlank()) return "";
        Set<String> match = new HashSet<>();
        String tLower = text.toLowerCase(new Locale("tr", "TR"));
        for (String s : skills.split("[,;]")) {
            String sTrim = s.trim().toLowerCase(new Locale("tr", "TR"));
            if (!sTrim.isEmpty() && tLower.contains(sTrim)) match.add(s.trim());
        }
        return String.join(", ", match);
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return "";
        return Arrays.stream(input.trim().split("\\s+"))
                .map(w -> w.isEmpty() ? "" : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase(new Locale("tr", "TR")))
                .collect(Collectors.joining(" "));
    }

    private String safe(String text) {
        return (text == null || text.equalsIgnoreCase("null")) ? "" : text.trim();
    }

    /**
     * Orijinal açıklamadan teknik terimleri çıkar (Java, Python, backend, frontend, API vb.)
     */
    private String extractTechnicalTerms(String text) {
        if (text == null || text.isBlank()) return "";
        
        // Yaygın teknik terimler ve teknolojiler
        Set<String> techTerms = new HashSet<>();
        String lowerText = text.toLowerCase();
        String originalText = text;
        
        // Teknoloji isimleri listesi
        String[] commonTechs = {
            "java", "python", "javascript", "typescript", "react", "angular", "vue", "node.js", "nodejs",
            "spring", "spring boot", "django", "flask", "express", "laravel", "php", "c#", "c++", "c",
            "sql", "mysql", "postgresql", "mongodb", "redis", "oracle", "sqlite",
            "html", "css", "sass", "less", "bootstrap", "tailwind",
            "docker", "kubernetes", "aws", "azure", "gcp", "jenkins", "git", "github", "gitlab",
            "rest", "restful", "api", "graphql", "soap", "microservice", "microservices",
            "backend", "frontend", "fullstack", "full-stack", "full stack",
            "android", "ios", "swift", "kotlin", "flutter", "react native",
            "machine learning", "ml", "ai", "deep learning", "tensorflow", "pytorch",
            "agile", "scrum", "devops", "ci/cd", "cicd"
        };
        
        // Teknoloji isimlerini bul
        for (String tech : commonTechs) {
            String techLower = tech.toLowerCase();
            if (lowerText.contains(techLower)) {
                // Orijinal metinde nasıl yazılmışsa öyle al
                int index = lowerText.indexOf(techLower);
                if (index >= 0) {
                    // Kelime sınırlarını kontrol et
                    boolean validStart = (index == 0 || !Character.isLetterOrDigit(originalText.charAt(index - 1)));
                    int endIndex = Math.min(index + tech.length(), originalText.length());
                    boolean validEnd = (endIndex == originalText.length() || !Character.isLetterOrDigit(originalText.charAt(endIndex)));
                    
                    if (validStart && validEnd) {
                        String original = originalText.substring(index, endIndex);
                        techTerms.add(original);
                    }
                }
            }
        }
        
        // "ile", "kullanarak" gibi ifadelerden sonra gelen terimleri bul
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "\\b([a-zA-Z]+(?:\\s+[a-zA-Z]+)?)\\s+(ile|kullanarak|ile\\s+backend|ile\\s+frontend)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String term = matcher.group(1).trim();
            // Teknoloji listesinde var mı kontrol et
            String termLower = term.toLowerCase();
            for (String tech : commonTechs) {
                if (termLower.equals(tech.toLowerCase()) || termLower.contains(tech.toLowerCase())) {
                    techTerms.add(term);
                    break;
                }
            }
        }
        
        // "backend", "frontend" gibi tek başına geçen terimleri de bul
        if (lowerText.contains("backend") || lowerText.contains("frontend")) {
            if (lowerText.contains("backend")) techTerms.add("backend");
            if (lowerText.contains("frontend")) techTerms.add("frontend");
        }
        
        return String.join(", ", techTerms);
    }
    
    /**
     * Cümlede teknik terimlerin geçtiğinden emin ol, yoksa ekle
     */
    private String ensureTechnicalTermsInDescription(String desc, String techTerms) {
        if (techTerms == null || techTerms.isBlank() || desc == null || desc.isBlank()) {
            return desc;
        }
        
        String lowerDesc = desc.toLowerCase();
        String[] terms = techTerms.split("[,;]");
        List<String> missingTerms = new ArrayList<>();
        
        for (String term : terms) {
            String trimmed = term.trim();
            if (!trimmed.isEmpty() && !lowerDesc.contains(trimmed.toLowerCase())) {
                missingTerms.add(trimmed);
            }
        }
        
        if (!missingTerms.isEmpty()) {
            // Eksik teknik terimleri doğal bir şekilde cümleye ekle
            String termsToAdd = String.join(", ", missingTerms.subList(0, Math.min(3, missingTerms.size())));
            
            // Cümle sonuna ekle veya ikinci cümleye ekle
            if (desc.contains(".")) {
                String[] sentences = desc.split("\\.", 2);
                if (sentences.length >= 2) {
                    // İkinci cümleye ekle
                    String secondSentence = sentences[1].trim();
                    if (!secondSentence.isEmpty()) {
                        desc = sentences[0].trim() + ". " + secondSentence + " Bu süreçte " + termsToAdd + " teknolojilerini/becerilerini kullandım.";
                    } else {
                        desc = desc.trim() + " Bu görevde " + termsToAdd + " teknolojilerini/becerilerini aktif olarak kullandım.";
                    }
                } else {
                    desc = desc.trim() + " Bu görevde " + termsToAdd + " teknolojilerini/becerilerini aktif olarak kullandım.";
                }
            } else {
                desc = desc.trim() + ". Bu görevde " + termsToAdd + " teknolojilerini/becerilerini aktif olarak kullandım.";
            }
        }
        
        return desc;
    }
    
    /**
     * Açıklama metinlerindeki format sorunlarını temizle (süslü parantez, JSON kalıntıları vb.)
     */
    private String cleanDescription(String text) {
        if (text == null || text.isBlank()) return "";
        
        // Süslü parantezleri ve JSON kalıntılarını temizle
        text = text.replaceAll("\\{[^}]*\\}", ""); // Parantez içindeki her şeyi sil
        text = text.replace("{", "").replace("}", ""); // Kalan parantezleri sil
        text = text.replaceAll("\\[[^\\]]*\\]", "");
        text = text.replaceAll("\"([^\"]*)\"", "$1"); // Tırnak içindeki metinleri koru ama tırnakları kaldır
        
        // Fazla boşlukları temizle
        text = text.replaceAll("\\s+", " ").trim();
        
        // Başta ve sonda gereksiz karakterleri temizle
        text = text.replaceAll("^[,\\s:;\\-]+", "");
        text = text.replaceAll("[,\\s:;\\-]+$", "");
        
        return text.trim();
    }

    /**
     * Deneyim açıklaması oluştur (AI ile veya fallback ile) - Profil bilgilerine göre detaylı
     */
    private String generateExperienceDescription(com.cvbuilder.entity.UserExperience exp, String jobContext, String jobSkills, String originalTechs) {
        String position = safe(exp.getPosition());
        String company = safe(exp.getCompany());
        String technologies = safe(exp.getTechnologies());
        String city = safe(exp.getCity());
        String employmentType = safe(exp.getEmploymentType());
        
        // Profil bilgilerini kullanarak detaylı açıklama oluştur
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Pozisyon: ").append(position.isEmpty() ? "Teknik Pozisyon" : position).append("\n");
        contextBuilder.append("Şirket: ").append(company.isEmpty() ? "Bir şirket" : company).append("\n");
        if (!city.isEmpty()) contextBuilder.append("Konum: ").append(city).append("\n");
        if (!employmentType.isEmpty()) contextBuilder.append("Çalışma Tipi: ").append(employmentType).append("\n");
        if (!technologies.isEmpty()) contextBuilder.append("Kullanılan Teknolojiler: ").append(technologies).append("\n");
        contextBuilder.append("Tarih: ").append(formatDateRange(exp.getStartDate(), exp.getEndDate()));
        
        String experienceContext = contextBuilder.toString();
        
        // Orijinal açıklamadan gelen teknik terimleri önceliklendir
        String techsToUse = !originalTechs.isEmpty() ? originalTechs : technologies;
        
        // Fallback: Detaylı açıklama oluştur
        String fallback = buildDetailedExperienceFallback(position, company, techsToUse, jobSkills);
        
        try {
            // Teknik bilgileri vurgula - önce orijinal açıklamadaki teknik terimler
            String techEmphasis = "";
            if (!originalTechs.isEmpty()) {
                techEmphasis = "\n\nÖNEMLİ: Mutlaka şu teknik terimleri/becerileri cümle içinde kullan: " + originalTechs + 
                    " - Bu terimleri doğal bir şekilde cümle içinde geçir, liste halinde yazma.";
            } else if (!technologies.isEmpty()) {
                techEmphasis = "\n\nÖNEMLİ: Mutlaka şu teknolojileri cümle içinde kullan: " + technologies + 
                    " - Bu teknolojileri doğal bir şekilde cümle içinde geçir, liste halinde yazma.";
            } else if (!jobSkills.isEmpty()) {
                String[] skills = jobSkills.split("[,;]");
                if (skills.length > 0) {
                    techEmphasis = "\n\nÖNEMLİ: Mutlaka şu becerileri/teknolojileri cümle içinde kullan: " + 
                        String.join(", ", Arrays.copyOf(skills, Math.min(3, skills.length))) + 
                        " - Bu becerileri doğal bir şekilde cümle içinde geçir.";
                }
            }
            
            String prompt = String.format("""
                SEN PROFESYONEL BİR CV YAZARISIN. Aşağıdaki iş deneyimini profesyonel bir dille açıkla.
                
                KURALLAR:
                1. SADECE 2 CÜMLE yaz - ne eksik ne fazla.
                2. Birinci şahıs kullan (yaptım, geliştirdim, çalıştım, uyguladım, yönettim).
                3. Her seferinde FARKLI ve YARATICI bir dil kullan - aynı kalıpları tekrarlama.
                4. İlk cümlede görevi, sorumlulukları ve yapılan işleri anlat.
                5. İkinci cümlede MUTLAKA kullanılan teknolojileri/becerileri belirt ve başarıları vurgula.
                6. Cümleleri çeşitlendir - farklı fiiller, farklı yapılar kullan.
                7. JSON, markdown, liste veya başlık kullanma.
                8. Tamamen doğal, insan yazısı gibi görünmeli - AI kalıntısı olmamalı.
                
                ÖRNEK FORMAT:
                "Spring Boot ve PostgreSQL teknolojilerini kullanarak e-ticaret platformunun backend geliştirmesinde aktif rol aldım. Bu süreçte RESTful API tasarımı yaparak sistem performansını %%30 artırdım ve mikroservis mimarisi uyguladım."
                
                DENEYİM DETAYI:
                %s
                
                İSTENEN BECERİLER:
                %s%s
                
                SADECE 2 CÜMLE YAZ - başka hiçbir şey ekleme.
                """, experienceContext, jobSkills, techEmphasis);
            
            String aiResponse = translationService.generateContent(prompt);
            if (aiResponse != null && !aiResponse.isBlank()) {
                String cleaned = cleanAIText(aiResponse);
                if (!cleaned.isEmpty() && cleaned.length() > 30) {
                    return ensureExactlyTwoSentences(cleaned);
                }
            }
        } catch (Exception e) {
            log.warn("Deneyim açıklaması AI ile oluşturulamadı: {}", e.getMessage());
        }
        
        return fallback;
    }
    
    /**
     * Detaylı deneyim fallback açıklaması oluştur (2 cümle) - Teknik bilgileri mutlaka kullan
     */
    private String buildDetailedExperienceFallback(String position, String company, String technologies, String jobSkills) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        
        String pos = position.isEmpty() ? "Yazılım Geliştirici" : position;
        String comp = company.isEmpty() ? "sektörün öncü firmalarından birinde" : company + " şirketinde";
        
        // İlk cümle - pozisyon ve şirket bilgisi
        String[] firstSentenceTemplates = {
            "%s pozisyonunda %s bünyesinde stratejik projelerde ve operasyonel süreçlerde görev aldım.",
            "%s olarak %s çalışma fırsatı buldum ve çeşitli teknik projelerde yer aldım.",
            "%s rolünde %s görev yaptım ve farklı projelerde aktif sorumluluklar üstlendim.",
            "%s unvanıyla %s ekibinde bulundum ve operasyonel süreçlerde önemli katkılar sağladım."
        };
        String firstSentence = String.format(firstSentenceTemplates[random.nextInt(firstSentenceTemplates.length)], pos, comp);
        sb.append(firstSentence).append(" ");
        
        // İkinci cümle - MUTLAKA teknik bilgileri kullan
        String techPart = technologies.isEmpty() ? jobSkills : technologies;
        String secondSentence;
        
        if (!techPart.isEmpty()) {
            String[] techs = techPart.split("[,;]");
            List<String> selectedTechs = new ArrayList<>();
            for (int i = 0; i < Math.min(techs.length, 3); i++) {
                String tech = techs[i].trim();
                if (!tech.isEmpty() && tech.length() > 1) {
                    selectedTechs.add(tech);
                }
            }
            
            if (!selectedTechs.isEmpty()) {
                String techList = String.join(", ", selectedTechs);
                String[] techTemplates = {
                    "Çalışmalarımda %s teknolojilerini etkin bir şekilde kullanarak sürdürülebilir ve ölçeklenebilir çözümler geliştirdim.",
                    "Bu görevde %s gibi modern teknolojilerle çalışarak sistem mimarisi tasarımı ve performans optimizasyonu konularında deneyim kazandım.",
                    "Projelerde %s teknolojilerini kullanarak RESTful API geliştirme, veritabanı yönetimi ve mikroservis mimarisi uyguladım.",
                    "%s ile çalışma fırsatı buldum ve bu teknolojileri kullanarak ölçeklenebilir backend sistemleri ve cloud çözümleri geliştirdim.",
                    "Bu süreçte %s teknolojilerini öğrenip uygulayarak veritabanı optimizasyonu, cache mekanizmaları ve API entegrasyonları gerçekleştirdim."
                };
                secondSentence = String.format(techTemplates[random.nextInt(techTemplates.length)], techList);
            } else {
                secondSentence = "Teknik yetkinliklerimi projelerin ihtiyaçları doğrultusunda kullanarak iş süreçlerinin iyileştirilmesine katkı sağladım.";
            }
        } else {
            secondSentence = "Teknik yetkinliklerimi projelerin ihtiyaçları doğrultusunda kullanarak iş süreçlerinin iyileştirilmesine katkı sağladım.";
        }
        
        sb.append(secondSentence);
        return sb.toString();
    }
    
    /**
     * Rastgele genel ikinci cümle üret
     */
    private String getRandomGenericSecondSentence(Random random) {
        String[] genericSentences = {
            "Bu süreçte çeşitli projelerde yer alarak teknik yetkinliğimi artırdım ve değerli deneyimler kazandım.",
            "Görevlerim sırasında problem çözme ve takım çalışması konularında kendimi geliştirdim.",
            "Bu deneyim sayesinde farklı projelerde başarılı sonuçlar elde ettim ve kariyerime önemli katkılar sağladım.",
            "Çalıştığım projelerde aktif rol alarak teknik bilgimi pratiğe dönüştürdüm ve başarılı çözümler ürettim.",
            "Bu süreçte ekip içi işbirliği ve teknik problem çözme konularında deneyim kazandım.",
            "Farklı projelerde yer alarak çok yönlü bir deneyim edindim ve bu süreçte önemli başarılar elde ettim.",
            "Görevlerim sırasında sürekli öğrenme ve gelişim odaklı çalışarak teknik yetkinliğimi artırdım.",
            "Bu deneyim boyunca çeşitli teknik zorluklarla karşılaştım ve bunları başarıyla çözerek değerli deneyimler kazandım."
        };
        return genericSentences[random.nextInt(genericSentences.length)];
    }

    /**
     * Proje açıklaması oluştur (AI ile veya fallback ile) - Profil bilgilerine göre detaylı
     */
    private String generateProjectDescription(com.cvbuilder.entity.UserProject proj, String jobContext, String jobSkills, String originalTechs) {
        String projectName = safe(proj.getProjectName());
        String technologies = safe(proj.getTechnologies());
        String url = safe(proj.getUrl());
        
        // Profil bilgilerini kullanarak detaylı açıklama oluştur
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Proje Adı: ").append(projectName.isEmpty() ? "Bir proje" : projectName).append("\n");
        if (!technologies.isEmpty()) contextBuilder.append("Kullanılan Teknolojiler: ").append(technologies).append("\n");
        if (!url.isEmpty()) contextBuilder.append("Proje Linki: ").append(url).append("\n");
        contextBuilder.append("Tarih: ").append(formatDateRange(proj.getStartDate(), 
            (proj.getIsOngoing() != null && proj.getIsOngoing()) ? null : proj.getEndDate()));
        
        String projectContext = contextBuilder.toString();
        
        // Orijinal açıklamadan gelen teknik terimleri önceliklendir
        String techsToUse = !originalTechs.isEmpty() ? originalTechs : technologies;
        
        // Fallback: Detaylı açıklama oluştur
        String fallback = buildDetailedProjectFallback(projectName, techsToUse, jobSkills);
        
        try {
            // Teknik bilgileri vurgula - önce orijinal açıklamadaki teknik terimler
            String techEmphasis = "";
            if (!originalTechs.isEmpty()) {
                techEmphasis = "\n\nÖNEMLİ: Mutlaka şu teknik terimleri/becerileri cümle içinde kullan: " + originalTechs + 
                    " - Bu terimleri doğal bir şekilde cümle içinde geçir, liste halinde yazma.";
            } else if (!technologies.isEmpty()) {
                techEmphasis = "\n\nÖNEMLİ: Mutlaka şu teknolojileri cümle içinde kullan: " + technologies + 
                    " - Bu teknolojileri doğal bir şekilde cümle içinde geçir, liste halinde yazma.";
            } else if (!jobSkills.isEmpty()) {
                String[] skills = jobSkills.split("[,;]");
                if (skills.length > 0) {
                    List<String> selectedSkills = new ArrayList<>();
                    for (int i = 0; i < Math.min(3, skills.length); i++) {
                        String skill = skills[i].trim();
                        if (!skill.isEmpty()) {
                            selectedSkills.add(skill);
                        }
                    }
                    if (!selectedSkills.isEmpty()) {
                        techEmphasis = "\n\nÖNEMLİ: Mutlaka şu becerileri/teknolojileri cümle içinde kullan: " + 
                            String.join(", ", selectedSkills) + 
                            " - Bu becerileri doğal bir şekilde cümle içinde geçir.";
                    }
                }
            }
            
            String prompt = String.format("""
                SEN PROFESYONEL BİR CV YAZARISIN. Aşağıdaki projeyi profesyonel bir dille açıkla.
                
                KURALLAR:
                1. SADECE 2 CÜMLE yaz - ne eksik ne fazla.
                2. Birinci şahıs kullan (geliştirdim, tasarladım, uyguladım, kodladım, test ettim).
                3. Her seferinde FARKLI ve YARATICI bir dil kullan - aynı kalıpları tekrarlama.
                4. İlk cümlede projenin amacını, kapsamını ve yapılan işleri anlat.
                5. İkinci cümlede MUTLAKA kullanılan teknolojileri/becerileri belirt ve sonuçları vurgula.
                6. Cümleleri çeşitlendir - farklı fiiller, farklı yapılar kullan.
                7. JSON, markdown, liste veya başlık kullanma.
                8. Tamamen doğal, insan yazısı gibi görünmeli - AI kalıntısı olmamalı.
                
                ÖRNEK FORMAT:
                "React ve Node.js kullanarak kullanıcı yönetim sistemi geliştirdim. Projede JWT authentication uygulayarak güvenli API endpoint'leri oluşturdum ve responsive tasarım ile kullanıcı deneyimini iyileştirdim."
                
                PROJE DETAYI:
                %s
                
                İSTENEN BECERİLER:
                %s%s
                
                SADECE 2 CÜMLE YAZ - başka hiçbir şey ekleme.
                """, projectContext, jobSkills, techEmphasis);
            
            String aiResponse = translationService.generateContent(prompt);
            if (aiResponse != null && !aiResponse.isBlank()) {
                String cleaned = cleanAIText(aiResponse);
                if (!cleaned.isEmpty() && cleaned.length() > 30) {
                    return ensureExactlyTwoSentences(cleaned);
                }
            }
        } catch (Exception e) {
            log.warn("Proje açıklaması AI ile oluşturulamadı: {}", e.getMessage());
        }
        
        return fallback;
    }
    
    /**
     * Detaylı proje fallback açıklaması oluştur (2 cümle) - Teknik bilgileri mutlaka kullan
     */
    private String buildDetailedProjectFallback(String projectName, String technologies, String jobSkills) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        
        // İlk cümle - proje adı ve kapsam
        String projName = projectName.isEmpty() ? "Bu projede" : projectName + " projesinde";
        String[] firstSentenceTemplates = {
            "%s aktif olarak yer alarak kullanıcı arayüzü ve backend geliştirme görevlerini üstlendim.",
            "%s geliştirme sürecinde bulunarak full-stack çözümler tasarladım ve uyguladım.",
            "%s tasarım ve geliştirme aşamalarında rol alarak modern yazılım pratiklerini uyguladım.",
            "%s üzerinde çalışarak ölçeklenebilir ve sürdürülebilir bir sistem mimarisi oluşturdum."
        };
        sb.append(String.format(firstSentenceTemplates[random.nextInt(firstSentenceTemplates.length)], projName)).append(" ");
        
        // İkinci cümle - MUTLAKA teknik bilgileri kullan
        String techPart = technologies.isEmpty() ? jobSkills : technologies;
        String secondSentence;
        
        if (!techPart.isEmpty()) {
            String[] techs = techPart.split("[,;]");
            List<String> selectedTechs = new ArrayList<>();
            for (int i = 0; i < Math.min(techs.length, 3); i++) {
                String tech = techs[i].trim();
                if (!tech.isEmpty() && tech.length() > 1) {
                    selectedTechs.add(tech);
                }
            }
            
            if (!selectedTechs.isEmpty()) {
                String techList = String.join(", ", selectedTechs);
                String[] techTemplates = {
                    "Projede %s teknolojilerini kullanarak responsive tasarım, state yönetimi ve API entegrasyonu gerçekleştirdim.",
                    "%s gibi modern araçları kullanarak authentication, veritabanı yönetimi ve real-time özellikler geliştirdim.",
                    "Bu projede %s teknolojilerini öğrenip uygulayarak RESTful API tasarımı, JWT authentication ve cloud deployment yaptım.",
                    "%s ile çalışarak component-based mimari, routing, form validation ve error handling mekanizmaları oluşturdum.",
                    "Projenin geliştirilmesinde %s teknolojilerini kullanarak mikroservis mimarisi, containerization ve CI/CD pipeline kurulumu gerçekleştirdim."
                };
                secondSentence = String.format(techTemplates[random.nextInt(techTemplates.length)], techList);
            } else {
                secondSentence = "Projenin tüm aşamalarında aktif rol alarak teknik bilgimi pratiğe dönüştürdüm ve başarılı çözümler ürettim.";
            }
        } else {
            secondSentence = "Projenin tüm aşamalarında aktif rol alarak teknik bilgimi pratiğe dönüştürdüm ve başarılı çözümler ürettim.";
        }
        
        sb.append(secondSentence);
        return sb.toString();
    }
    
    /**
     * Rastgele proje ikinci cümlesi üret
     */
    private String getRandomProjectSecondSentence(Random random) {
        String[] projectSentences = {
            "Projenin tasarımından geliştirilmesine kadar tüm aşamalarında aktif rol aldım ve başarılı sonuçlar elde ettim.",
            "Bu projede çeşitli teknik zorluklarla karşılaştım ve bunları başarıyla çözerek değerli deneyimler kazandım.",
            "Projenin geliştirilmesi sırasında problem çözme ve yaratıcı düşünme yeteneklerimi geliştirdim.",
            "Bu süreçte projenin tüm aşamalarında yer alarak teknik bilgimi pratiğe dönüştürdüm ve başarılı çözümler ürettim.",
            "Projede aktif olarak çalışarak farklı teknolojileri öğrendim ve bu deneyim sayesinde kendimi geliştirdim.",
            "Projenin başarıyla tamamlanması için çeşitli görevler üstlendim ve bu süreçte önemli başarılar elde ettim.",
            "Bu projede yer alarak teknik yetkinliğimi artırdım ve projenin hedeflerine ulaşmasında önemli bir rol oynadım.",
            "Projenin geliştirilmesi sırasında ekip çalışması ve teknik problem çözme konularında deneyim kazandım."
        };
        return projectSentences[random.nextInt(projectSentences.length)];
    }

    /**
     * En az belirtilen sayıda cümle olduğundan emin ol
     */
    private String ensureMinimumSentences(String text, int minSentences) {
        if (text == null || text.isBlank()) return text;
        
        String[] sentences = text.split("[.!?]+");
        int count = sentences.length;
        
        if (count < minSentences) {
            StringBuilder sb = new StringBuilder(text.trim());
            // Eksik cümleleri ekle
            for (int i = count; i < minSentences; i++) {
                if (!sb.toString().endsWith(".") && !sb.toString().endsWith("!") && !sb.toString().endsWith("?")) {
                    sb.append(".");
                }
                sb.append(" Bu görevde başarılı sonuçlar elde ettim.");
            }
            return sb.toString();
        }
        
        return text;
    }

    /**
     * Tam olarak 2 mantıklı cümle olduğundan emin ol (proje/deneyim için)
     * İyileştirilmiş versiyon: Daha iyi temizleme ve minimum karakter kontrolü
     */
    private String ensureExactlyTwoSentences(String text) {
        if (text == null || text.isBlank()) {
            Random random = new Random();
            String[] fallbackPairs = {
                "İlgili alanda teknik sorumluluklar üstlenerek projelerin başarıyla tamamlanmasına katkı sağladım. Süreç boyunca modern teknolojileri kullanarak verimli çözümler ürettim.",
                "Farklı projelerde yer alarak deneyim kazandım. Bu süreçte teknik bilgimi pratiğe dönüştürdüm ve başarılı sonuçlar elde ettim.",
                "Görevlerim sırasında problem çözme yeteneklerimi geliştirdim. Ekip çalışması ve teknik uygulamalar konularında değerli deneyimler kazandım."
            };
            return fallbackPairs[random.nextInt(fallbackPairs.length)];
        }
        
        // Önce tüm metni temizle (Görünmez karakterler, süslü parantezler vb.)
        text = cleanDescription(text);
        
        // Cümleleri ayır (Nokta, Ünlem, Soru işareti sonrası boşluk)
        String[] sentences = text.split("(?<=[.!?])\\s+");
        List<String> validSentences = new ArrayList<>();
        
        for (String s : sentences) {
            String trimmed = s.trim()
                .replaceAll("^[-•*\\s]+", "") // Liste işaretlerini temizle
                .replaceAll("\\s+", " ");    // Fazla boşlukları temizle
            
            // Cümle en az 25 karakter olmalı ki anlamlı olsun
            if (trimmed.length() > 25) {
                if (!trimmed.matches(".*[.!?]$")) {
                    trimmed += ".";
                }
                validSentences.add(trimmed);
            }
        }
        
        if (validSentences.isEmpty()) {
            Random random = new Random();
            String[] fallbackPairs = {
                "İlgili alanda teknik sorumluluklar üstlenerek projelerin başarıyla tamamlanmasına katkı sağladım. Süreç boyunca modern teknolojileri kullanarak verimli çözümler ürettim.",
                "Farklı projelerde yer alarak deneyim kazandım. Bu süreçte teknik bilgimi pratiğe dönüştürdüm ve başarılı sonuçlar elde ettim.",
                "Görevlerim sırasında problem çözme yeteneklerimi geliştirdim. Ekip çalışması ve teknik uygulamalar konularında değerli deneyimler kazandım."
            };
            return fallbackPairs[random.nextInt(fallbackPairs.length)];
        }
        
        // Tam 2 cümle oluştur
        if (validSentences.size() >= 2) {
            String first = validSentences.get(0);
            String second = validSentences.get(1);
            // İlk cümlenin sonunda nokta yoksa ekle
            if (!first.matches(".*[.!?]$")) {
                first += ".";
            }
            // İkinci cümlenin sonunda nokta yoksa ekle
            if (!second.matches(".*[.!?]$")) {
                second += ".";
            }
            return first + " " + second;
        } else {
            // Tek cümle varsa yanına anlamlı bir devam cümlesi ekle
            String first = validSentences.get(0);
            if (!first.matches(".*[.!?]$")) {
                first += ".";
            }
            String second = generateSecondSentence(first);
            if (!second.matches(".*[.!?]$")) {
                second += ".";
            }
            return first + " " + second;
        }
    }

    /**
     * İlk cümleye göre mantıklı ikinci cümle oluştur - Çeşitli varyasyonlar
     */
    private String generateSecondSentence(String firstSentence) {
        Random random = new Random();
        String lower = firstSentence.toLowerCase();
        
        // İlk cümleye göre uygun ikinci cümle varyasyonları
        if (lower.contains("geliştirdim") || lower.contains("geliştirme")) {
            String[] variations = {
                "Bu süreçte teknik yetkinliğimi artırdım ve projenin başarıyla tamamlanmasına katkı sağladım.",
                "Geliştirme sürecinde çeşitli teknik zorluklarla karşılaştım ve bunları başarıyla çözdüm.",
                "Bu deneyim sayesinde modern geliştirme pratiklerini öğrendim ve uyguladım.",
                "Projelerin başarıyla tamamlanması için etkili çözümler ürettim ve değerli deneyimler kazandım."
            };
            return variations[random.nextInt(variations.length)];
        } else if (lower.contains("çalıştım") || lower.contains("görev")) {
            String[] variations = {
                "Bu deneyim sayesinde problem çözme ve takım çalışması konularında kendimi geliştirdim.",
                "Görevlerim sırasında teknik bilgimi pratiğe dönüştürdüm ve başarılı sonuçlar elde ettim.",
                "Bu süreçte ekip içi işbirliği ve teknik problem çözme konularında deneyim kazandım.",
                "Çalıştığım projelerde aktif rol alarak farklı teknolojileri öğrendim ve uyguladım."
            };
            return variations[random.nextInt(variations.length)];
        } else if (lower.contains("proje") || lower.contains("projesi")) {
            String[] variations = {
                "Projenin başarıyla tamamlanmasına katkı sağladım ve bu süreçte değerli deneyimler kazandım.",
                "Bu projede çeşitli teknik görevler üstlendim ve başarılı sonuçlar elde ettim.",
                "Projenin geliştirilmesi sırasında yaratıcı çözümler ürettim ve teknik yetkinliğimi artırdım.",
                "Projede aktif olarak çalışarak modern teknolojileri öğrendim ve uyguladım."
            };
            return variations[random.nextInt(variations.length)];
        } else if (lower.contains("teknoloji") || lower.contains("teknolojiler")) {
            String[] variations = {
                "Bu teknolojileri kullanarak kaliteli çözümler ürettim ve projelerin başarıyla tamamlanmasına katkı sağladım.",
                "Teknolojileri etkin bir şekilde uygulayarak teknik yetkinliğimi geliştirdim ve başarılı sonuçlar elde ettim.",
                "Bu araçları kullanarak çeşitli projelerde yer aldım ve değerli deneyimler kazandım.",
                "Modern teknolojilerle çalışarak problem çözme yeteneklerimi geliştirdim ve etkili çözümler ürettim."
            };
            return variations[random.nextInt(variations.length)];
        } else {
            String[] variations = {
                "Bu süreçte başarılı sonuçlar elde ettim ve deneyimlerimi artırdım.",
                "Çalışmalarım sırasında teknik bilgimi geliştirdim ve önemli başarılar elde ettim.",
                "Bu deneyim sayesinde farklı projelerde yer alarak kendimi geliştirdim.",
                "Süreç boyunca aktif rol alarak değerli deneyimler kazandım ve başarılı sonuçlar elde ettim."
            };
            return variations[random.nextInt(variations.length)];
        }
    }

    /**
     * Optimizasyon için iş ilanı bağlamı oluştur
     */
    private String buildJobContextForOptimization(JobPosting job) {
        if (job == null) return "";
        return String.format("Pozisyon: %s\nGereken Beceriler: %s\nSorumluluklar: %s",
                safe(job.getPosition()), safe(job.getRequiredSkills()), safe(job.getResponsibilities()));
    }

    private String formatDateRange(String start, String end) {
        String s = (start != null && !start.isBlank()) ? start : "Belirtilmemiş";
        String e = (end != null && !end.isBlank()) ? end : "Devam Ediyor";
        return s + " - " + e;
    }
}
