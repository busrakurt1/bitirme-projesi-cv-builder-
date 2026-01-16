package com.cvbuilder.repository;

import com.cvbuilder.entity.JobPosting;
import com.cvbuilder.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    // Bir kullanıcıya ait tüm iş ilanları
    List<JobPosting> findByUserId(Long userId);

    // Aynı URL daha önce eklenmiş mi kontrol etmek için
    List<JobPosting> findByUserIdAndUrl(Long userId, String url);

	List<JobPosting> findTop100ByPositionContainingIgnoreCaseOrderByCreatedAtDesc(String area);
	
	// Pazar analizi için en az 200 ilan getirmek için
	List<JobPosting> findTop200ByPositionContainingIgnoreCaseOrderByCreatedAtDesc(String area);

	List<JobPosting> findByUserIdOrderByCreatedAtDesc(Long userId);

	Optional<User> findFirstByUser_IdOrderByCreatedAtDesc(Long id);
}