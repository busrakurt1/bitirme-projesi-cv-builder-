package com.cvbuilder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobAnalysisResponse {
    private Long jobId;
    private List<String> matchedSkills;
    private List<String> missingSkills;

    private String position;
    private String company;
    private String location;
    private String workType;
    private String experienceLevel;
    private String educationLevel;
    private String militaryStatus;
    private String salary;
    private String summary;
    private List<String> responsibilities;

    private Integer matchScore;

    private String formattedAnalysis;
}
