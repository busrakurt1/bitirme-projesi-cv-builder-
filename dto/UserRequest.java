package com.cvbuilder.dto;

import lombok.Data;

@Data
public class UserRequest {
    private String email;
    private String password;
    private String fullName;
    private String title;
    private Integer experienceYears;
    private String summary;
    private String location;
    private String phone;
}