package com.family.app.controller;

import com.family.app.dto.FamilyTreeResponse;
import com.family.app.dto.RelationshipCompareResponse;
import com.family.app.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicFamilyTreeController {

    private final MemberService memberService;

    @GetMapping("/family-tree")
    public ResponseEntity<FamilyTreeResponse> getFamilyTree(
            @RequestParam(required = false) String familyId) {
        try {
            return ResponseEntity.ok(memberService.getFamilyTreeDataForPublic(familyId));
        } catch (IllegalStateException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/relationship")
    public ResponseEntity<RelationshipCompareResponse> compareRelationship(
            @RequestParam(required = false) String familyId,
            @RequestParam String memberAId,
            @RequestParam String memberBId) {
        try {
            return ResponseEntity.ok(memberService.compareRelationship(familyId, memberAId, memberBId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
