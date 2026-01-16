// UserSkill.java
package com.cvbuilder.entity;

import jakarta.persistence.*;
import lombok.*;

//UserSkill.java
@Entity
@Table(name = "user_skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSkill {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 // Hangi profile ait?
 @ManyToOne(fetch = FetchType.LAZY)
 @JoinColumn(name = "profile_id")
 private UserProfile userProfile;

 @Column(name = "skill_name", nullable = false)
 private String skillName;   // Örn: "Java", "Spring Boot"

 private String level;       // Örn: "BEGINNER", "INTERMEDIATE", "ADVANCED"
 private Integer years;      // Örn: 1, 2, 3
 public Object getYearsOfExperience() {
	// TODO Auto-generated method stub
	return null;
 }
}
