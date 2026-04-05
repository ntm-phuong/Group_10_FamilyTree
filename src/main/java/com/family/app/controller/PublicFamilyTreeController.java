package com.family.app.controller;

import com.family.app.config.AppClanProperties;
import com.family.app.dto.FamilyTreeResponse;
import com.family.app.dto.RelationshipCompareResponse;
import com.family.app.repository.FamilyRepository;
import com.family.app.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class PublicFamilyTreeController {

    private final MemberService memberService;
    private final AppClanProperties clanProperties;
    private final FamilyRepository familyRepository;

    @GetMapping("/family-tree")
    public String familyTreePage(Model model) {
        String fid = clanProperties.getFamilyId();
        String name = familyRepository.findById(fid)
                .map(f -> f.getFamilyName())
                .orElse(clanProperties.getDisplayName());
        model.addAttribute("clanFamilyId", fid);
        model.addAttribute("clanFamilyName", name);
        model.addAttribute("activeMenu", "family-tree");
        return "public/family-tree";
    }

    @GetMapping("/api/public/family-tree")
    @ResponseBody
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

    @GetMapping("/api/public/relationship")
    @ResponseBody
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
