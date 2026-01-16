package com.cvbuilder.dto;

import lombok.Data;
import java.util.Set;

import java.util.List;

@Data
public class UserDTO {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    
    // Profil Başlık/Özet Bilgileri
    private String title;
    private Integer experienceYears;
    private String summary;
    private String location;

    // İlişkisel Veriler
    private Set<UserSkillDTO> skills;         // Yetenekler
    private List experiences; // EKLENEN KISIM: İş Deneyimleri
}