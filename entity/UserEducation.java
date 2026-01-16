package com.cvbuilder.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_educations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEducation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hangi profile ait?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(nullable = false)
    private String schoolName;      // Örn: "İstanbul Teknik Üniversitesi"

    @Column(nullable = false)
    private String department;      // Örn: "Bilgisayar Mühendisliği" veya "Hukuk"

    @Column(nullable = false)
    private String degree;          // Örn: "Lisans", "Yüksek Lisans", "Önlisans", "Doktora"

    private String startYear;       // Örn: "2018"
    private String endYear;         // Örn: "2022" veya null (Devam Ediyor)
    
    // İsteğe bağlı not ortalaması
    private String gpa;             // Örn: "3.50/4.00"
}