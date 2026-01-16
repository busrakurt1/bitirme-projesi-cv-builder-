package com.cvbuilder.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSkillDTO {

    private Long id;

    private String category;  
    private String skillName;            // "Java", "Spring Boot"
    private String level;           // "BEGINNER", "INTERMEDIATE", "ADVANCED"
    private Integer years;          // Opsiyonel: tecrübe yılı
}
