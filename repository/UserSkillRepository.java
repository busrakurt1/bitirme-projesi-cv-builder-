package com.cvbuilder.repository;

import com.cvbuilder.entity.UserExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSkillRepository extends JpaRepository<UserExperience, Long> {

    // Profil üzerinden tüm deneyimleri getir
    List<UserExperience> findByUserProfile_Id(Long profileId);

    // Kullanıcı üzerinden tüm deneyimleri getir (opsiyonel)
    List<UserExperience> findByUserProfile_User_Id(Long userId);
}
