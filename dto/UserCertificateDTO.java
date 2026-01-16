package com.cvbuilder.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCertificateDTO {
    private Long id;
    private String name;        // Sertifika Adı (AWS Certified Developer)
    private String issuer;      // Veren Kurum (Amazon)
    private String date;        // Alınan Tarih (2023-05)
    private String url;         // (Opsiyonel) Sertifika Linki
}