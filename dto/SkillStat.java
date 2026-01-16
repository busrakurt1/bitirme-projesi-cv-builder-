package com.cvbuilder.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkillStat {
    private String skillName;
    private Integer frequency;
    private Double percentage;
    private Boolean userHasSkill;
}
