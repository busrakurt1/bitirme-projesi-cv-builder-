package com.cvbuilder.dto;

import lombok.Data;

@Data
public class JobMatchRequest {
    private Long userId;
    private String jobDescription;
}