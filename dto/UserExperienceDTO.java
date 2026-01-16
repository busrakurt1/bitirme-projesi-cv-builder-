package com.cvbuilder.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExperienceDTO {
    private Long id;
    private String position;
    private String company;
    private String city;
    
    // Yeni Alanlar
    private String employmentType;
    private String technologies;
    
    private String startDate;
    private String endDate;
    private String description;
    
    
}