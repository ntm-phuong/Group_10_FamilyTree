package com.family.app.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UserResponse {
    private String userId;
    private String fullName;
    private String email;
    private String gender;
    private LocalDate dob;
    private String phoneNumber;
    private String hometown;
    private String currentAddress;
    private String occupation;
    private String bio;
    private String avatar;
    /** ID dòng họ (FK) — tiện cho màn quản trị. */
    private String familyId;
    private String familyName;
    private String roleName;


    private String parentId;
    private Integer generation;
    private Integer orderInFamily;
}