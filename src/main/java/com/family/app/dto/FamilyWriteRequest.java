package com.family.app.dto;

import lombok.Data;

@Data
public class FamilyWriteRequest {
    private String familyName;
    private String description;
    private String privacySetting;
    /** fam-root hoặc null */
    private String parentFamilyId;
}
