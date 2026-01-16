package com.cvbuilder.dto;

import lombok.Data;

@Data
public class JobUrlRequest {
    private String url;          // single için
    private String mode;         // "single" | "market"
    private String title;        // market için meslek adı
    private Integer limit;       // market için min 100
    private Boolean includeClosed; // açık/kapalı dahil
    private String sort;         // "latest"
	public Object getUserId() {
		// TODO Auto-generated method stub
		return null;
	}
}
