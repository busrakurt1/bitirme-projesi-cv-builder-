package com.cvbuilder.controller;

import com.cvbuilder.dto.UserProfileRequest;
import com.cvbuilder.dto.UserProfileResponse;
import com.cvbuilder.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserProfileService userProfileService;

    // ✅ Profil getir
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        log.info("📥 Profil GET /me - userId: {}", userId);

        UserProfileResponse response = userProfileService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    // ✅ Profil güncelle / kaydet
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody UserProfileRequest request
    ) {
        log.info("📥 Profil PUT /me - userId: {}", userId);

        UserProfileResponse response = userProfileService.saveOrUpdate(userId, request);
        return ResponseEntity.ok(response);
    }
}
