package com.cvbuilder.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLanguageDTO {
    private Long id;
    private String language; // Örn: "İngilizce"
    private String level;    // Örn: "A1", "C2", "Native"
    public String getCertificate;
}