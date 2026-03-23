package com.family.app.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FamilyTreeStats {
    private final int totalMembers;
    private final int totalGenerations;
}
