package com.family.app.dto;

import lombok.Data;

@Data
public class FamilyRegisterRequest {
    private String fullName;
    private String email;
    private String familyName;
    private String password;
}