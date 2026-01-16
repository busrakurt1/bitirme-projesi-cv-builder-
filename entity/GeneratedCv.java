package com.cvbuilder.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "generated_cvs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class GeneratedCv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String templateName; 

    @Column(columnDefinition = "TEXT") // Uzun metinler için
    private String content; // Oluşturulan CV içeriği (HTML/JSON)

    // --- ✅ YENİ: AI MARKET ANALİZİ SONUCU ---
    // Hocanın istediği "Son 100 ilana göre öneriler" kısmını buraya kaydedebilirsin.
    @Column(columnDefinition = "TEXT")
    private String aiCareerAdvice; 

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "job_posting_id")
    private JobPosting jobPosting;
}