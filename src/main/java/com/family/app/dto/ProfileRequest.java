package com.family.app.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ProfileRequest {
    private String fullName;
    private String phoneNumber;
    private String currentAddress;
    private String hometown;
    private String occupation;
    private String bio;
    private String gender;
    private LocalDate dob;
}