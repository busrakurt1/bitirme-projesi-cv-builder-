package com.cvbuilder.dto;

import java.util.List; // ✅ DOĞRU import

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProjectDTO {
	private Long id;
	private String projectName;
	private String startDate;
	private String endDate;
	private Boolean isOngoing;

	private String url;
	private String technologies; // String olarak tutuluyor
	private List<String> technologyDetails; // ❗ Tipi de netleştir (String liste)
	private String generatedDescription;
	private String description; 
}
