package com.cvbuilder.service;

import com.cvbuilder.dto.CvGenerationRequest;
import com.cvbuilder.dto.GeneratedCvResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiCvService {

    // 1. Ana Metot: Süreci Yönetir
    public GeneratedCvResponse generateTailoredCv(CvGenerationRequest request) {
    	GeneratedCvResponse response = new GeneratedCvResponse();

        // A) Becerileri Sırala (Mantıksal İşlem)
        // Kullanıcının becerileri içinde, İlanın istedikleri varsa onları listenin en başına alıyoruz.
        List<String> sortedSkills = sortSkillsPriority(request.getUserSkills(), request.getRequiredKeywords());
        response.setPrioritizedSkills(sortedSkills);

        // B) Yapay Zekaya Özet Yazdır (AI İşlemi)
        String prompt = createPrompt(request);
        String aiGeneratedSummary = callAiApi(prompt); // Burası AI servisine gidecek
        response.setTailoredSummary(aiGeneratedSummary);

        return response;
    }

    // 2. Beceri Sıralama Mantığı (ATS İçin Kritik)
    private List<String> sortSkillsPriority(List<String> userSkills, List<String> requiredKeywords) {
        List<String> sorted = new ArrayList<>();
        
        // Önce ilandaki kelimelerle eşleşenleri ekle (Büyük harf/küçük harf duyarsız)
        for (String skill : userSkills) {
            for (String req : requiredKeywords) {
                if (skill.equalsIgnoreCase(req)) {
                    sorted.add(skill);
                    break; 
                }
            }
        }
        
        // Sonra eşleşmeyen diğer yetenekleri ekle
        for (String skill : userSkills) {
            boolean alreadyAdded = false;
            for (String s : sorted) {
                if (s.equalsIgnoreCase(skill)) alreadyAdded = true;
            }
            if (!alreadyAdded) sorted.add(skill);
        }
        return sorted;
    }

    // 3. Prompt Hazırlama (Prompt Engineering)
    private String createPrompt(CvGenerationRequest request) {
        String keywords = String.join(", ", request.getRequiredKeywords());
        String experiences = String.join("; ", request.getExperiences());

        return String.format("""
            Sen uzman bir CV danışmanısın. Aşağıdaki adayın profili ile hedeflenen iş ilanını analiz et.
            
            HEDEF İŞ POZİSYONU: %s
            İLANIN KRİTİK ANAHTAR KELİMELERİ: %s
            
            ADAYIN MEVCUT PROFİLİ:
            - İsim: %s
            - Mevcut Unvan: %s
            - Deneyim Notları: %s
            
            GÖREVİN:
            Bu aday için iş ilanındaki anahtar kelimeleri içeren, profesyonel, 3-4 cümleyi geçmeyen,
            birinci tekil şahıs ağzından (Örn: "Geliştirdim", "Deneyimliyim") yazılmış etkileyici bir
            "Professional Summary" (Özet) paragrafı yaz.
            
            Sadece özeti yaz, başka hiçbir açıklama ekleme.
            """,
            request.getTargetJobTitle(),
            keywords,
            request.getFullName(),
            request.getCurrentJobTitle(),
            experiences
        );
    }

    // 4. Mock AI Çağrısı (Sen buraya gerçek OpenAI/Gemini kodunu bağlayacaksın)
    private String callAiApi(String prompt) {
        // TODO: Buraya RestTemplate veya OpenAiService ile gerçek istek atılacak.
        // Şimdilik test edebilmen için sahte (ama mantıklı) bir cevap dönüyorum:
        System.out.println("AI Prompt Gönderildi: " + prompt);
        
        return "E-Ticaret alanında uzmanlaşmış, HTML, CSS ve JavaScript teknolojilerinde derin bilgiye sahip bir Yazılım Geliştiriciyim. " +
               "Flutter ve Android tabanlı mobil uygulama projelerinde aktif rol alarak kullanıcı dostu arayüzler tasarladım. " +
               "Teknik analiz yeteneğim ve proje süreçlerine aktif katılımımla, firmanızın dijital dönüşüm süreçlerine değer katmaya hazırım.";
    }
}