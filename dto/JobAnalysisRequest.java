package com.cvbuilder.dto;

import lombok.Data;

@Data
public class JobAnalysisRequest {
    // Frontend'den gelen JSON isimleriyle aynı olmalı
    private Long userId;
    private String url;
}