package com.cvbuilder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenerateDescriptionResponse {
    private String technology;
    private String level;
    private String description;
    private String suggestedLevel; // AI'nın önerdiği seviye
    private Integer confidenceScore; // 0-100 arası güven skoru
    private String[] keywords; // Anahtar kelimeler
    private String alternativeDescription; // Alternatif açıklama
}