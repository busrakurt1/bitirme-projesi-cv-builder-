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
public class GenerateDescriptionRequest {
    private String technology;
    private String level; // "BEGINNER", "INTERMEDIATE", "EXPERT"
    private String userInput; // Kullanıcının girdiği metin
    private String context; // "experience", "project", "skill" - hangi bağlamda kullanılacak
}