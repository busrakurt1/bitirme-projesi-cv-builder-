package com.cvbuilder.controller;

import com.cvbuilder.dto.ChatRequest;
import com.cvbuilder.dto.ChatResponse;
import com.cvbuilder.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "X-USER-ID", required = false) Long userIdFromHeader
    ) {
        try {
            log.info("Chatbot mesaj isteği alındı - Header userId: {}, Body userId: {}, Message: {}", 
                    userIdFromHeader, request.getUserId(), request.getMessage());
            
            // Header'dan veya body'den userId al
            Long userId = request.getUserId() != null ? request.getUserId() : userIdFromHeader;
            if (userId == null) {
                log.warn("Chatbot mesaj isteği reddedildi: userId bulunamadı");
                return ResponseEntity.badRequest().body("Kullanıcı ID'si bulunamadı");
            }
            
            request.setUserId(userId);
            ChatResponse response = chatbotService.sendMessage(request);
            log.info("Chatbot yanıtı oluşturuldu - Response ID: {}, Response uzunluğu: {}", 
                    response.getId(), response.getResponse() != null ? response.getResponse().length() : 0);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Chatbot mesaj işleme hatası: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Mesaj işlenirken bir hata oluştu: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatResponse>> getChatHistory(
            @RequestHeader(value = "X-USER-ID", required = false) Long userId
    ) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        List<ChatResponse> history = chatbotService.getChatHistory(userId);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearChatHistory(
            @RequestHeader(value = "X-USER-ID", required = false) Long userId
    ) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        chatbotService.clearChatHistory(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("CHATBOT OK");
    }
}







