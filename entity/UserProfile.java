package com.cvbuilder.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // --- TEMEL BİLGİLER ---
    private String title;          // Örn: "Avukat", "İnşaat Mühendisi" (Artık her meslek olabilir)
    private Integer totalExperienceYear; // Örn: 5

    @Column(length = 2000)
    private String summary;

    private String linkedinUrl;
    private String githubUrl;
    private String websiteUrl;

    // --- ❌ ESKİ EĞİTİM ALANLARI SİLİNDİ ---
    // private String educationSchool;
    // private String educationStartYear; ...

    // --- ✅ YENİ: EĞİTİM LİSTESİ ---
    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserEducation> educations = new ArrayList<>();

    // --- İLİŞKİSEL LİSTELER (DİĞERLERİ AYNI) ---
    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSkill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserProject> projects = new ArrayList<>();

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserExperience> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserLanguage> languages = new ArrayList<>();

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserCertificate> certificates = new ArrayList<>();

 // ✅ EKLE
    private String department;        // Örn: Bilgisayar Mühendisliği
    private String militaryStatus;    // Örn: Muaf / Tecilli / Yapıldı
	}
