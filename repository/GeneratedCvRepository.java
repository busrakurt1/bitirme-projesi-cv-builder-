package com.cvbuilder.repository;

import com.cvbuilder.entity.GeneratedCv;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneratedCvRepository extends JpaRepository<GeneratedCv, Long> {

    // Kullanıcının tüm CV taslakları
    List<GeneratedCv> findByUser_Id(Long userId);

    // Belirli bir iş ilanı için üretilmiş CV’ler
    List<GeneratedCv> findByUser_IdAndJobPosting_Id(Long userId, Long jobPostingId);

    // Tek bir CV çekmek için
    Optional<GeneratedCv> findByIdAndUser_Id(Long id, Long userId);
}
