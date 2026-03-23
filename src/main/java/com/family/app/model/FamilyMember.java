package com.family.app.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FamilyMember {
    private final Long id;
    private final String name;
    private final Gender gender;
    private final Integer birthYear;
    private final Integer deathYear;
    private final String occupation;
    private final int generation;

    public enum Gender {
        MALE, FEMALE
    }
}
