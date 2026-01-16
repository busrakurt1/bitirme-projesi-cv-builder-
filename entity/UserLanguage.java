package com.cvbuilder.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_languages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String language;
    private String level;

    // Hangi profile ait olduğunu tutuyoruz
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id")
    private UserProfile userProfile;

}