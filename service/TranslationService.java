package com.cvbuilder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TranslationService
 * Zincirleme AI Sistemi (Fallback Mechanism):
 * 1. Gemini (Multi-Key) -> PRIMARY (ÖNCELİKLİ)
 * 2. Groq (Multi-Key)   -> SECONDARY (FALLBACK 1)
 * 3. DeepSeek (Multi-Key) -> TERTIARY (FALLBACK 2)
 */
@Slf4j
@Service
public class TranslationService {

    // ==========================================
    // 1. GEMINI CONFIG (PRIMARY - ÖNCELİKLİ)
    // ==========================================
    @Value("${gemini.api.keys:}")
    private String geminiApiKeysString;
    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;
    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    // ==========================================
    // 2. GROQ CONFIG (SECONDARY / FALLBACK 1)
    // ==========================================
    @Value("${groq.api.keys:}")
    private String groqApiKeysString;
    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqUrl;
    @Value("${groq.model:llama-3.1-8b-instant}")
    private String groqModel;

    // ==========================================
    // 3. DEEPSEEK CONFIG (TERTIARY)
    // ==========================================
    @Value("${deepseek.api.keys:}")
    private String deepSeekApiKeysString;
    @Value("${deepseek.api.url:https://api.deepseek.com/chat/completions}")
    private String deepSeekUrl;
    @Value("${deepseek.model:deepseek-chat}")
    private String deepSeekModel;

    // Anahtar Listeleri ve İndeksler
    private List<String> geminiKeys;
    private List<String> groqKeys;
    private List<String> deepSeekKeys;

    private final AtomicInteger currentGeminiIndex = new AtomicInteger(0);
    private final AtomicInteger currentGroqIndex = new AtomicInteger(0);
    private final AtomicInteger currentDeepSeekIndex = new AtomicInteger(0);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TranslationService() {
        this.restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(120))
                .build();
    }

    @PostConstruct
    public void init() {
        // 1. Gemini Init (PRIMARY)
        this.geminiKeys = parseKeys(geminiApiKeysString);
        if (this.geminiKeys.isEmpty()) {
            log.warn("UYARI: Gemini anahtarları yok!");
        } else {
            log.info("Gemini anahtarları yüklendi: {} adet", this.geminiKeys.size());
        }

        // 2. Groq Init (SECONDARY)
        this.groqKeys = parseKeys(groqApiKeysString);
        if (this.groqKeys.isEmpty()) {
            log.warn("UYARI: Groq anahtarları yok!");
        } else {
            log.info("Groq anahtarları yüklendi: {} adet", this.groqKeys.size());
        }

        // 3. DeepSeek Init (TERTIARY - Optional)
        this.deepSeekKeys = parseKeys(deepSeekApiKeysString);
        if (this.deepSeekKeys.isEmpty()) {
            log.warn("UYARI: DeepSeek anahtarları yok! (Opsiyonel servis)");
        } else {
            log.info("DeepSeek anahtarları yüklendi: {} adet", this.deepSeekKeys.size());
        }
    }

    private List<String> parseKeys(String raw) {
        List<String> list = new ArrayList<>();
        if (raw != null && !raw.isBlank()) {
            for (String k : raw.split(",")) {
                String t = k.trim();
                if (!t.isEmpty()) list.add(t);
            }
        }
        return list;
    }

    private String getNextKey(List<String> keys, AtomicInteger index) {
        if (keys == null || keys.isEmpty()) throw new RuntimeException("API Key Listesi Boş!");
        int idx = Math.abs(index.getAndIncrement() % keys.size());
        return keys.get(idx);
    }

    // =========================================================
    // PUBLIC METHODS (ZİNCİRLEME MANTIK: GEMINI -> GROQ -> DEEPSEEK)
    // =========================================================

    public String generateContent(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            log.warn("generateContent çağrıldı ancak prompt boş!");
            return "";
        }
        
        log.info("AI içerik üretimi başlatılıyor, prompt uzunluğu: {}", prompt.length());
        
        // 1. GEMINI (Primary - Öncelikli)
        try {
            log.debug("Gemini servisi deneniyor...");
            String response = sendRequestToGemini(prompt);
            if (response != null && !response.trim().isEmpty()) {
                log.info("Gemini başarılı, yanıt uzunluğu: {}", response.length());
                return response;
            } else {
                log.warn("Gemini boş yanıt döndü, Groq deneniyor...");
                throw new RuntimeException("Gemini boş yanıt döndü");
            }
        } catch (Exception e1) {
            log.warn("Gemini başarısız ({}), Groq deneniyor...", e1.getMessage());

            // 2. GROQ (Secondary / Fallback 1)
            try {
                log.debug("Groq servisi deneniyor...");
                String response = sendRequestToGroq(prompt);
                if (response != null && !response.trim().isEmpty()) {
                    log.info("Groq başarılı, yanıt uzunluğu: {}", response.length());
                    return response;
                } else {
                    log.warn("Groq boş yanıt döndü, DeepSeek deneniyor...");
                    throw new RuntimeException("Groq boş yanıt döndü");
                }
            } catch (Exception e2) {
                log.warn("Groq başarısız ({}), DeepSeek deneniyor...", e2.getMessage());

                // 3. DEEPSEEK (Tertiary / Fallback 2 - Optional)
                try {
                    if (deepSeekKeys == null || deepSeekKeys.isEmpty()) {
                        log.warn("DeepSeek anahtarları yok, atlanıyor...");
                        throw new RuntimeException("DeepSeek yapılandırılmamış");
                    }
                    log.debug("DeepSeek servisi deneniyor...");
                    String response = sendRequestToDeepSeek(prompt);
                    if (response != null && !response.trim().isEmpty()) {
                        log.info("DeepSeek başarılı, yanıt uzunluğu: {}", response.length());
                        return response;
                    } else {
                        log.error("DeepSeek boş yanıt döndü!");
                        throw new RuntimeException("DeepSeek boş yanıt döndü");
                    }
                } catch (Exception e3) {
                    log.error("Tüm AI servisleri başarısız oldu! Son hata: {}", e3.getMessage(), e3);
                    throw new RuntimeException("AI servisine ulaşılamadı. Lütfen backend konsolunu kontrol edin.", e3);
                }
            }
        }
    }

    public Map<String, Object> translateCV(Object userCvData, String targetLang) {
        String languageName = targetLang.equalsIgnoreCase("en") ? "English" : targetLang;
        String jsonInput;
        try {
            jsonInput = objectMapper.writeValueAsString(userCvData);
        } catch (Exception e) {
            throw new RuntimeException("JSON Hatası: " + e.getMessage());
        }

        String prompt = String.format(
                "You are an expert CV translator. Translate the JSON values into %s. " +
                "STRICT RULES:\n" +
                "1) Do not change JSON structure. Only translate string values.\n" +
                "2) Keep technical terms (e.g., Java, Spring, AWS) and company names unchanged.\n" +
                "3) Do not modify id, email, phone, linkedinUrl or date fields.\n" +
                "4) Return ONLY valid JSON (no markdown, no backticks).\n\nINPUT_JSON:\n%s",
                languageName, jsonInput
        );

        String aiText = generateContent(prompt); // Ortak generateContent metodunu kullanır (Gemini -> Groq -> DeepSeek sırası)

        String cleaned = cleanPossibleCodeFences(aiText);
        try {
            return objectMapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("AI yanıtı JSON formatında değil: " + cleaned);
        }
    }

    // =========================================================
    // CORE: GROQ REQUEST
    // =========================================================
    private String sendRequestToGroq(String prompt) {
        if (groqKeys == null || groqKeys.isEmpty()) throw new RuntimeException("Groq keys yok");
        int maxAttempts = groqKeys.size();

        Map<String, Object> systemMessage = Map.of(
                "role", "system",
                "content", "Sen profesyonel bir asistanısın. Cümlelerini her zaman tamamla, gramer olarak doğru yaz ve akıcı Türkçe kullan. Yarım kalan veya bozuk cümleler yazma."
        );
        Map<String, Object> userMessage = Map.of("role", "user", "content", prompt);
        Map<String, Object> requestBody = Map.of(
                "model", groqModel,
                "messages", List.of(systemMessage, userMessage),
                "temperature", 0.5,
                "max_tokens", 4000
        );

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String key = getNextKey(groqKeys, currentGroqIndex);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + key);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            try {
                ResponseEntity<String> response = restTemplate.postForEntity(groqUrl, entity, String.class);
                log.info("Groq HTTP yanıtı: Status={}, Body uzunluğu={}", 
                        response.getStatusCode(), response.getBody() != null ? response.getBody().length() : 0);
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    // Ham yanıtı logla
                    String rawBody = response.getBody();
                    log.info("Groq ham yanıt (ilk 1000 karakter): {}", 
                            rawBody.length() > 1000 ? rawBody.substring(0, 1000) + "..." : rawBody);
                    
                    String parsed = parseResponseSafe(rawBody);
                    log.info("Groq parse edilmiş yanıt uzunluğu: {}, içerik: {}", 
                            parsed != null ? parsed.length() : 0,
                            parsed != null && parsed.length() > 0 ? (parsed.length() > 200 ? parsed.substring(0, 200) + "..." : parsed) : "BOŞ");
                    
                    if (parsed == null || parsed.trim().isEmpty()) {
                        log.error("Groq yanıtı parse edildi ama boş! Ham yanıt: {}", rawBody);
                        throw new RuntimeException("Groq boş yanıt döndü");
                    }
                    return parsed;
                } else {
                    log.error("Groq başarısız HTTP yanıtı: Status={}, Body={}", 
                            response.getStatusCode(), 
                            response.getBody() != null && response.getBody().length() < 500 ? response.getBody() : "çok uzun");
                    throw new RuntimeException("Groq HTTP hatası: " + response.getStatusCode());
                }
            } catch (HttpClientErrorException e) {
                log.error("Groq HTTP Client Hatası ({}): Status={}, Body={}", 
                        maskKey(key), e.getStatusCode(), e.getResponseBodyAsString());
                if (e.getStatusCode().value() == 429) {
                    log.warn("Groq 429 - Key değişiyor: {}", maskKey(key));
                    continue;
                }
                throw new RuntimeException("Groq HTTP hatası: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            } catch (Exception e) {
                log.error("Groq Hatası ({}): {}", maskKey(key), e.getMessage(), e);
                throw e;
            }
        }
        throw new RuntimeException("Tüm Groq anahtarları tükendi.");
    }

    // =========================================================
    // CORE: GEMINI REQUEST
    // =========================================================
    private String sendRequestToGemini(String prompt) {
        if (geminiKeys == null || geminiKeys.isEmpty()) throw new RuntimeException("Gemini keys yok");
        String safeModelName = geminiModel.startsWith("models/") ? geminiModel : "models/" + geminiModel;
        int maxAttempts = geminiKeys.size();

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> requestBody = Map.of("contents", List.of(content));

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String key = getNextKey(geminiKeys, currentGeminiIndex);
            String apiUrl = UriComponentsBuilder.fromHttpUrl(geminiBaseUrl)
                    .pathSegment(safeModelName + ":generateContent")
                    .queryParam("key", key)
                    .build().toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            try {
                ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
                log.info("Gemini HTTP yanıtı: Status={}, Body uzunluğu={}", 
                        response.getStatusCode(), response.getBody() != null ? response.getBody().length() : 0);
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    // Ham yanıtı logla
                    String rawBody = response.getBody();
                    log.info("Gemini ham yanıt (ilk 1000 karakter): {}", 
                            rawBody.length() > 1000 ? rawBody.substring(0, 1000) + "..." : rawBody);
                    
                    String parsed = parseResponseSafe(rawBody);
                    log.info("Gemini parse edilmiş yanıt uzunluğu: {}, içerik: {}", 
                            parsed != null ? parsed.length() : 0,
                            parsed != null && parsed.length() > 0 ? (parsed.length() > 200 ? parsed.substring(0, 200) + "..." : parsed) : "BOŞ");
                    
                    if (parsed == null || parsed.trim().isEmpty()) {
                        log.error("Gemini yanıtı parse edildi ama boş! Ham yanıt: {}", rawBody);
                        throw new RuntimeException("Gemini boş yanıt döndü");
                    }
                    return parsed;
                } else {
                    log.error("Gemini başarısız HTTP yanıtı: Status={}, Body={}", 
                            response.getStatusCode(), 
                            response.getBody() != null && response.getBody().length() < 500 ? response.getBody() : "çok uzun");
                    throw new RuntimeException("Gemini HTTP hatası: " + response.getStatusCode());
                }
            } catch (HttpClientErrorException e) {
                log.error("Gemini HTTP Client Hatası ({}): Status={}, Body={}", 
                        maskKey(key), e.getStatusCode(), e.getResponseBodyAsString());
                if (e.getStatusCode().value() == 429) {
                    log.warn("Gemini 429 - Key değişiyor: {}", maskKey(key));
                    continue;
                }
                throw new RuntimeException("Gemini HTTP hatası: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            } catch (Exception e) {
                log.error("Gemini Hatası ({}): {}", maskKey(key), e.getMessage(), e);
                throw e;
            }
        }
        throw new RuntimeException("Tüm Gemini anahtarları tükendi.");
    }

    // =========================================================
    // CORE: DEEPSEEK REQUEST
    // =========================================================
    private String sendRequestToDeepSeek(String prompt) {
        if (deepSeekKeys == null || deepSeekKeys.isEmpty()) throw new RuntimeException("DeepSeek keys yok");
        
        Map<String, Object> message = Map.of("role", "user", "content", prompt);
        Map<String, Object> requestBody = Map.of(
                "model", deepSeekModel,
                "messages", List.of(message)
        );

        String key = getNextKey(deepSeekKeys, currentDeepSeekIndex);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + key);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(deepSeekUrl, entity, String.class);
            log.info("DeepSeek HTTP yanıtı: Status={}, Body uzunluğu={}", 
                    response.getStatusCode(), response.getBody() != null ? response.getBody().length() : 0);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Ham yanıtı logla
                String rawBody = response.getBody();
                log.info("DeepSeek ham yanıt (ilk 1000 karakter): {}", 
                        rawBody.length() > 1000 ? rawBody.substring(0, 1000) + "..." : rawBody);
                
                String parsed = parseResponseSafe(rawBody);
                log.info("DeepSeek parse edilmiş yanıt uzunluğu: {}, içerik: {}", 
                        parsed != null ? parsed.length() : 0,
                        parsed != null && parsed.length() > 0 ? (parsed.length() > 200 ? parsed.substring(0, 200) + "..." : parsed) : "BOŞ");
                
                if (parsed == null || parsed.trim().isEmpty()) {
                    log.error("DeepSeek yanıtı parse edildi ama boş! Ham yanıt: {}", rawBody);
                    throw new RuntimeException("DeepSeek boş yanıt döndü");
                }
                return parsed;
            } else {
                log.error("DeepSeek başarısız HTTP yanıtı: Status={}, Body={}", 
                        response.getStatusCode(), 
                        response.getBody() != null && response.getBody().length() < 500 ? response.getBody() : "çok uzun");
                throw new RuntimeException("DeepSeek HTTP hatası: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            log.error("DeepSeek HTTP Client Hatası ({}): Status={}, Body={}", 
                    maskKey(key), e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("DeepSeek HTTP hatası: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("DeepSeek Hatası: {}", e.getMessage(), e);
            throw new RuntimeException("DeepSeek Hatası: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private String parseResponseSafe(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            log.warn("parseResponseSafe: responseBody boş!");
            return "";
        }
        
        // Ham yanıtı logla (ilk 500 karakter)
        log.debug("parseResponseSafe: Ham yanıt (ilk 500 karakter): {}", 
                responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);
        
        // Önce eksik/invalid JSON kontrolü yap
        String trimmed = responseBody.trim();
        
        // Sadece parantez/boşluk kontrolü
        if (trimmed.matches("^[\\s\\{\\}\\[\\]\\(\\)]*$")) {
            log.warn("parseResponseSafe: Yanıt sadece parantez/boşluk içeriyor, boş döndürülüyor");
            return "";
        }
        
        // Eksik JSON kontrolü: Sadece { veya { ile başlayıp kapanmamışsa
        if (trimmed.equals("{") || (trimmed.startsWith("{") && !trimmed.contains("}"))) {
            log.warn("parseResponseSafe: Eksik JSON tespit edildi (sadece açılış parantezi veya kapanmamış JSON), boş döndürülüyor. Trimmed: '{}'", trimmed);
            return "";
        }
        
        // Çok kısa yanıt kontrolü (sadece 1-2 karakter)
        if (trimmed.length() <= 2 && trimmed.matches("^[\\{\\}\\[\\]\\s]*$")) {
            log.warn("parseResponseSafe: Yanıt çok kısa ve sadece parantez içeriyor, boş döndürülüyor. Trimmed: '{}'", trimmed);
            return "";
        }
        
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            
            // OpenAI / Groq / DeepSeek Format
            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                JsonNode firstChoice = root.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    String content = firstChoice.get("message").get("content").asText();
                    if (content == null || content.trim().isEmpty()) {
                        log.warn("Groq/OpenAI/DeepSeek yanıtında content boş!");
                        return "";
                    }
                    // Parse edilen içeriği de kontrol et - sadece parantez içeriyorsa boş döndür
                    String cleanedContent = content.trim();
                    if (cleanedContent.matches("^[\\s\\{\\}\\[\\]\\(\\)]*$") || cleanedContent.length() <= 2) {
                        log.warn("Groq/OpenAI/DeepSeek parse edilen içerik geçersiz (sadece parantez veya çok kısa): '{}'", cleanedContent);
                        return "";
                    }
                    log.info("Groq/OpenAI/DeepSeek formatından yanıt parse edildi, uzunluk: {}, içerik (ilk 200 karakter): {}", 
                            cleanedContent.length(), cleanedContent.length() > 200 ? cleanedContent.substring(0, 200) + "..." : cleanedContent);
                    return cleanedContent;
                } else {
                    log.warn("Groq/OpenAI/DeepSeek yanıtında message.content bulunamadı! firstChoice: {}", firstChoice.toString());
                }
            }
            
            // Gemini Format
            if (root.has("candidates") && root.get("candidates").isArray() && root.get("candidates").size() > 0) {
                JsonNode firstCandidate = root.get("candidates").get(0);
                if (firstCandidate.has("content") && firstCandidate.get("content").has("parts") 
                    && firstCandidate.get("content").get("parts").isArray() 
                    && firstCandidate.get("content").get("parts").size() > 0) {
                    JsonNode firstPart = firstCandidate.get("content").get("parts").get(0);
                    if (firstPart.has("text")) {
                        String text = firstPart.get("text").asText();
                        if (text == null || text.trim().isEmpty()) {
                            log.warn("Gemini yanıtında text boş!");
                            return "";
                        }
                        // Parse edilen içeriği de kontrol et - sadece parantez içeriyorsa boş döndür
                        String cleanedText = text.trim();
                        if (cleanedText.matches("^[\\s\\{\\}\\[\\]\\(\\)]*$") || cleanedText.length() <= 2) {
                            log.warn("Gemini parse edilen içerik geçersiz (sadece parantez veya çok kısa): '{}'", cleanedText);
                            return "";
                        }
                        log.info("Gemini formatından yanıt parse edildi, uzunluk: {}, içerik (ilk 200 karakter): {}", 
                                cleanedText.length(), cleanedText.length() > 200 ? cleanedText.substring(0, 200) + "..." : cleanedText);
                        return cleanedText;
                    } else {
                        log.warn("Gemini yanıtında parts[0].text bulunamadı! firstPart: {}", firstPart.toString());
                    }
                } else {
                    log.warn("Gemini yanıtında content.parts[0] bulunamadı! firstCandidate: {}", firstCandidate.toString());
                }
            }
            
            // Eğer beklenen format yoksa, ham yanıtı döndürme - boş döndür
            log.warn("Beklenmeyen yanıt formatı! Ham yanıt (ilk 200 karakter): {}", 
                    responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody);
            // Beklenmeyen format varsa boş döndür, ham yanıtı döndürme
            return "";
        } catch (Exception e) {
            log.error("Yanıt parse edilemedi: {}", e.getMessage());
            log.debug("Ham yanıt (ilk 500 karakter): {}", responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);
            
            // Parse hatası durumunda, eğer yanıt sadece parantez içeriyorsa boş döndür
            String trimmedError = responseBody.trim();
            if (trimmedError.matches("^[\\s\\{\\}\\[\\]\\(\\)]*$") || trimmedError.length() <= 2) {
                log.warn("Parse hatası ve yanıt geçersiz format (sadece parantez veya çok kısa), boş döndürülüyor. Trimmed: '{}'", trimmedError);
                return "";
            }
            
            // Parse hatası durumunda boş döndür - ham yanıtı döndürme
            log.warn("Parse hatası nedeniyle boş yanıt döndürülüyor");
            return "";
        }
    }

    private String cleanPossibleCodeFences(String text) {
        if (text == null) return "";
        // Markdown JSON temizleme
        String cleaned = text.replaceAll("(?s)```json\\s*", "");
        cleaned = cleaned.replaceAll("(?s)```\\s*", "");
        return cleaned.trim();
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 8) return "****";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}