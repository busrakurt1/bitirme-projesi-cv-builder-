package com.cvbuilder.model;

import java.util.List;
import java.util.Map;

public class JobAnalysis {
    private double matchScore;
    private String matchLevel;
    private Map<String, Double> categoryScores;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private List<String> recommendations;
    private String analysisSummary;

    // Constructor
    public JobAnalysis() {}

    public JobAnalysis(double matchScore, String matchLevel, Map<String, Double> categoryScores, 
                      List<String> matchingSkills, List<String> missingSkills, 
                      List<String> recommendations, String analysisSummary) {
        this.matchScore = matchScore;
        this.matchLevel = matchLevel;
        this.categoryScores = categoryScores;
        this.matchingSkills = matchingSkills;
        this.missingSkills = missingSkills;
        this.recommendations = recommendations;
        this.analysisSummary = analysisSummary;
    }

    // Getters and Setters
    public double getMatchScore() { return matchScore; }
    public void setMatchScore(double matchScore) { this.matchScore = matchScore; }

    public String getMatchLevel() { return matchLevel; }
    public void setMatchLevel(String matchLevel) { this.matchLevel = matchLevel; }

    public Map<String, Double> getCategoryScores() { return categoryScores; }
    public void setCategoryScores(Map<String, Double> categoryScores) { this.categoryScores = categoryScores; }

    public List<String> getMatchingSkills() { return matchingSkills; }
    public void setMatchingSkills(List<String> matchingSkills) { this.matchingSkills = matchingSkills; }

    public List<String> getMissingSkills() { return missingSkills; }
    public void setMissingSkills(List<String> missingSkills) { this.missingSkills = missingSkills; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

    public String getAnalysisSummary() { return analysisSummary; }
    public void setAnalysisSummary(String analysisSummary) { this.analysisSummary = analysisSummary; }
}