package com.cvbuilder.model;

public enum DescriptionLevel {
    BEGINNER("Başlangıç", "Yeni başlayan seviye, temel kavramlar"),
    INTERMEDIATE("Orta", "Temel-orta düzey deneyim"),
    EXPERT("İleri", "İleri düzey, uzmanlık gerektiren");
    
    private final String displayName;
    private final String description;
    
    DescriptionLevel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    // String'den enum'a dönüştürme
    public static DescriptionLevel fromString(String level) {
        if (level == null) return INTERMEDIATE;
        try {
            return DescriptionLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return INTERMEDIATE;
        }
    }

}
