package com.cvbuilder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptimizedCvItem {
    private String title;       // Örn: Senior Developer
    private String subtitle;    // Örn: Trendyol
    private String date;        // Örn: 2020 - 2023
    private List<String> description; // Madde madde optimize edilmiş açıklamalar
}