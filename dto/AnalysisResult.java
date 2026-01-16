package com.cvbuilder.dto;

import lombok.Data;
import java.util.List;

@Data
public class AnalysisResult {
    private double matchPercentage;
    private String matchLevel;
    private List<SkillMatch> matchingSkills;
    private List<String> missingSkills;
    private List<String> recommendations;
    private String analysisSummary;
    
    @Data
    public static class SkillMatch {
        private String skill;
        private String matchType;
        private String userLevel;
        private int importance;
    }
}