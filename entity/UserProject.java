package com.cvbuilder.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_projects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProject {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String projectName;
    
    private String startDate;
    private String endDate;
    
    @Column(nullable = false)
    private Boolean isOngoing = false;

    // --- YENİ EKLENEN ALANLAR ---
    @Column(length = 1000)
    private String technologies; // Örn: "React, Node.js"

    private String url;          // Örn: GitHub veya Live link

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id")
    private UserProfile userProfile;
}