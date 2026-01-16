// src/main/java/com/cvbuilder/dto/UserEducationDTO.java
package com.cvbuilder.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEducationDTO {
    private Long id;
    private String schoolName;     // Örn: İTÜ
    private String department;     // Örn: Bilgisayar Müh.
    private String degree;         // Örn: Lisans, Yüksek Lisans
    private String startYear;
    private String graduationYear; // null ise "Devam Ediyor"
    private String gpa;            // Örn: 3.50
}