package com.family.app.dto;

import lombok.Data;

@Data
public class FamilyResponse {
    private String familyId;
    private String familyName;
    private String description;
    private String privacySetting;
    private String parentFamilyId;
    private String parentFamilyName;
}
