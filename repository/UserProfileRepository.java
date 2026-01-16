package com.cvbuilder.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cvbuilder.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    // Sade, fetch join YOK
    Optional<UserProfile> findByUserId(Long userId);
}
