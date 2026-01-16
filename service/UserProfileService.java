package com.cvbuilder.service;

import com.cvbuilder.dto.UserProfileRequest;
import com.cvbuilder.dto.UserProfileResponse;

public interface UserProfileService {

    UserProfileResponse getProfile(Long userId);

    // Profil kaydet / güncelle (upsert)
    UserProfileResponse saveOrUpdate(Long userId, UserProfileRequest request);
}
