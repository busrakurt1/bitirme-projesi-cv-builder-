package com.cvbuilder.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillGap {
    private String skillName;
    private Double demandPercentage;
    private String priority;
    private List<String> learningResources;
}
