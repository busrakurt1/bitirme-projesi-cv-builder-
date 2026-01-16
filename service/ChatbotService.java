package com.cvbuilder.service;

import com.cvbuilder.dto.ChatRequest;
import com.cvbuilder.dto.ChatResponse;
import com.cvbuilder.entity.ChatMessage;
import com.cvbuilder.entity.User;
import com.cvbuilder.entity.UserProfile;
import com.cvbuilder.repository.ChatMessageRepository;
import com.cvbuilder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final TranslationService translationService;

    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        log.info("Chatbot mesajı alındı - User: {}, Message: {}", request.getUserId(), request.getMessage());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        // Son 10 mesajı al (context için)
        List<ChatMessage> recentMessages = chatMessageRepository.findRecentMessagesByUserId(request.getUserId());
        List<ChatMessage> contextMessages = recentMessages.stream()
                .limit(10)
                .collect(Collectors.toList());

        // AI'ya gönderilecek prompt'u oluştur
        String prompt = buildChatPrompt(user, request.getMessage(), contextMessages);
        log.debug("Chatbot prompt oluşturuldu, uzunluk: {}", prompt.length());

        // AI'dan yanıt al
        String aiResponse;
        try {
            log.info("AI servisine istek gönderiliyor...");
            aiResponse = translationService.generateContent(prompt);
            log.info("AI yanıtı alındı, uzunluk: {}", aiResponse != null ? aiResponse.length() : 0);
            
            // Boş veya null yanıt kontrolü
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                log.warn("AI servisi boş yanıt döndü!");
                aiResponse = "Üzgünüm, şu anda yanıt veremiyorum. Lütfen daha sonra tekrar deneyin.";
            }
        } catch (Exception e) {
            log.error("AI yanıt hatası: {}", e.getMessage(), e);
            aiResponse = "Üzgünüm, şu anda yanıt veremiyorum. Lütfen daha sonra tekrar deneyin.";
        }

        // Kullanıcı mesajını kaydet
        ChatMessage userMessage = ChatMessage.builder()
                .user(user)
                .message(request.getMessage())
                .response("")
                .role("user")
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(userMessage);

        // AI yanıtını kaydet
        ChatMessage assistantMessage = ChatMessage.builder()
                .user(user)
                .message("")
                .response(aiResponse)
                .role("assistant")
                .createdAt(LocalDateTime.now())
                .build();
        ChatMessage saved = chatMessageRepository.save(assistantMessage);

        return ChatResponse.builder()
                .id(saved.getId())
                .message(request.getMessage())
                .response(aiResponse)
                .createdAt(saved.getCreatedAt())
                .role("assistant")
                .build();
    }

    public List<ChatResponse> getChatHistory(Long userId) {
        List<ChatMessage> messages = chatMessageRepository.findByUserIdOrderByCreatedAtAsc(userId);
        return messages.stream()
                .map(msg -> ChatResponse.builder()
                        .id(msg.getId())
                        .message(msg.getMessage())
                        .response(msg.getResponse())
                        .createdAt(msg.getCreatedAt())
                        .role(msg.getRole())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void clearChatHistory(Long userId) {
        chatMessageRepository.deleteByUserId(userId);
        log.info("Chat geçmişi temizlendi - User: {}", userId);
    }

    private String buildChatPrompt(User user, String currentMessage, List<ChatMessage> contextMessages) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("SEN YARDIMCI BİR ASİSTANSIN. Kullanıcıya her konuda yardımcı olabilirsin.\n\n");
        prompt.append("KRİTİK TALİMATLAR - MUTLAKA UY:\n");
        prompt.append("1. Her konu hakkında konuşabilirsin - hiçbir konu kısıtlaması yok.\n");
        prompt.append("2. Yanıtların MUTLAKA KISA ve ÖZ olmalı - maksimum 3-4 cümle, nokta atışı cevaplar ver.\n");
        prompt.append("3. CÜMLE YAPISI ÇOK ÖNEMLİ: Her cümle gramer olarak doğru, akıcı ve anlaşılır olmalı.\n");
        prompt.append("4. Cümlelerini tamamla - yarım kalan, eksik veya bozuk cümleler yazma.\n");
        prompt.append("5. Türkçe dilbilgisi kurallarına uy - özne, yüklem, nesne sıralaması doğru olsun.\n");
        prompt.append("6. Cümleler arasında mantıklı geçişler yap, paragrafları düzgün oluştur.\n");
        prompt.append("7. Gereksiz uzun açıklamalar yapma, doğrudan soruya cevap ver.\n");
        prompt.append("8. Türkçe yanıt ver - doğal ve akıcı Türkçe kullan.\n");
        prompt.append("9. Samimi ve yardımcı bir dil kullan.\n");
        prompt.append("10. Eğer kullanıcının profili hakkında bilgi varsa, ona göre özelleştirilmiş tavsiyeler ver.\n\n");

        // Kullanıcı profili bilgisi
        UserProfile profile = user.getProfile();
        if (profile != null) {
            prompt.append("KULLANICI PROFİLİ:\n");
            prompt.append("- İsim: ").append(user.getFullName() != null ? user.getFullName() : "Belirtilmemiş").append("\n");
            if (profile.getTitle() != null) {
                prompt.append("- Meslek: ").append(profile.getTitle()).append("\n");
            }
            if (profile.getTotalExperienceYear() != null) {
                prompt.append("- Deneyim: ").append(profile.getTotalExperienceYear()).append(" yıl\n");
            }
            prompt.append("\n");
        }

        // Önceki mesaj geçmişi (context)
        if (!contextMessages.isEmpty()) {
            prompt.append("ÖNCEKİ KONUŞMA GEÇMİŞİ:\n");
            for (ChatMessage msg : contextMessages) {
                if ("user".equals(msg.getRole()) && msg.getMessage() != null && !msg.getMessage().isEmpty()) {
                    prompt.append("Kullanıcı: ").append(msg.getMessage()).append("\n");
                }
                if ("assistant".equals(msg.getRole()) && msg.getResponse() != null && !msg.getResponse().isEmpty()) {
                    prompt.append("Asistan: ").append(msg.getResponse()).append("\n");
                }
            }
            prompt.append("\n");
        }

        prompt.append("KULLANICININ ŞU ANKİ SORUSU:\n");
        prompt.append(currentMessage).append("\n\n");
        prompt.append("Yukarıdaki soruya uygun, yardımcı ve profesyonel bir yanıt ver.\n");
        prompt.append("ÖNEMLİ: Cümlelerini tamamla, gramer olarak doğru yaz, akıcı ve anlaşılır ol. Yarım kalan veya bozuk cümleler yazma.");

        return prompt.toString();
    }
}

