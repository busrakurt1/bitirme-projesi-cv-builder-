package com.cvbuilder.repository;

import com.cvbuilder.entity.UserExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserExperienceRepository extends JpaRepository<UserExperience, Long> {

    // Profile ID üzerinden deneyimleri getir
    List<UserExperience> findByUserProfile_Id(Long profileId);

    // Kullanıcı ID üzerinden deneyimleri getir (opsiyonel)
    List<UserExperience> findByUserProfile_User_Id(Long userId);
}
