package com.cvbuilder.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String issuer;
    private String date;
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id")
    private UserProfile userProfile;

	public Object getOrganization() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getCertificateName() {
		// TODO Auto-generated method stub
		return null;
	}
}