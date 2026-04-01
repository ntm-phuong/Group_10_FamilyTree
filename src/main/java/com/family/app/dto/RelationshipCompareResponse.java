 package com.family.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipCompareResponse {
    private String memberAId;
    private String memberBId;
    private String relationship;
    private String relationshipFromAToB;
    private String relationshipFromBToA;
    private String relationGroup;
    private List<String> commonAncestors;
    private List<String> notes;
}

