package com.family.app.controller;

import com.family.app.dto.FamilyResponse;
import com.family.app.dto.FamilyWriteRequest;
import com.family.app.dto.CreateSpouseRequest;
import com.family.app.dto.MemberRoleOptionResponse;
import com.family.app.dto.UserRequest;
import com.family.app.dto.UserResponse;
import com.family.app.model.User;
import com.family.app.security.AppPermissions;
import com.family.app.service.FamilyHeadService;
import com.family.app.service.FamilyScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family-head")
public class FamilyHeadController {

    @Autowired
    private FamilyHeadService familyHeadService;
    @Autowired
    private FamilyScopeService familyScopeService;

    /** Danh sách chi trong phạm vi — dùng cho cả lọc tin (MANAGE_FAMILY_NEWS) và quản lý chi (MANAGE_FAMILY_MEMBERS). */
    @GetMapping("/families")
    @PreAuthorize("hasAnyAuthority('MANAGE_FAMILY_MEMBERS','MANAGE_FAMILY_NEWS','MANAGE_CLAN')")
    public ResponseEntity<List<FamilyResponse>> listFamilies(
            @AuthenticationPrincipal User principal,
            Authentication authentication) {
        boolean memberScope = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> AppPermissions.MANAGE_FAMILY_MEMBERS.equals(a.getAuthority())
                        || AppPermissions.MANAGE_CLAN.equals(a.getAuthority()));
        return ResponseEntity.ok(familyHeadService.listFamiliesInScope(principal.getUserId(), memberScope));
    }

    @PostMapping("/families")
    @PreAuthorize("hasAnyAuthority('MANAGE_FAMILY_MEMBERS','MANAGE_CLAN')")
    public ResponseEntity<FamilyResponse> createFamily(
            @RequestBody FamilyWriteRequest request,
            @AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(familyHeadService.createFamily(request));
    }

    @PutMapping("/families/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGE_FAMILY_MEMBERS','MANAGE_CLAN')")
    public ResponseEntity<FamilyResponse> updateFamily(
            @PathVariable String id,
            @RequestBody FamilyWriteRequest request,
            @AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(familyHeadService.updateFamily(id, request, principal.getUserId()));
    }

    @DeleteMapping("/families/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGE_FAMILY_MEMBERS','MANAGE_CLAN')")
    public ResponseEntity<String> deleteFamily(@PathVariable String id, @AuthenticationPrincipal User principal) {
        familyHeadService.deleteFamily(id, principal.getUserId());
        return ResponseEntity.ok("Đã xóa dòng họ.");
    }

    /** Vai trò có thể gán khi thêm/sửa thành viên (MEMBER, phụ trách tin, trưởng họ). */
    @GetMapping("/member-roles")
    @PreAuthorize("hasAnyAuthority('MANAGE_FAMILY_MEMBERS','MANAGE_CLAN')")
    public ResponseEntity<List<MemberRoleOptionResponse>> listMemberRoles() {
        return ResponseEntity.ok(familyHeadService.listAssignableMemberRoles());
    }

    @GetMapping("/members")
    @PreAuthorize("hasAnyAuthority('MANAGE_FAMILY_MEMBERS','MANAGE_CLAN')")
    public ResponseEntity<List<UserResponse>> getMembers(
            @RequestParam(required = false) String familyId,
            @AuthenticationPrincipal User principal) {
        String fid = (familyId != null && !familyId.isBlank())
                ? familyId.trim()
                : familyScopeService.requireManagedFamilyId(principal.getUserId());
        familyScopeService.assertCanManageFamilyMembers(principal.getUserId(), fid);
        return ResponseEntity.ok(familyHeadService.getMembersForManagedFamily(fid));
    }

    @GetMapping("/members/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGE_FAMILY_MEMBERS','MANAGE_CLAN')")
    public ResponseEntity<UserResponse> getMember(@PathVariable String id, @AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(familyHeadService.getMember(id, principal.getUserId()));
    }

    @PostMapping("/members")
    @PreAuthorize("hasAnyAuthority('MANAGE_FAMILY_MEMBERS','MANAGE_CLAN')")
    public ResponseEntity<?> addMember(@RequestBody UserRequest request, @AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(familyHeadService.saveMember(request, principal.getUserId()));
    }

    @PutMapping("/members/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGE_FAMILY_MEMBERS','MANAGE_CLAN')")
    public ResponseEntity<?> updateMember(
            @PathVariable String id,
            @RequestBody UserRequest request,
            @AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(familyHeadService.updateMember(id, request, principal.getUserId()));
    }

    @PostMapping("/members/{id}/spouse")
    @PreAuthorize("hasAnyAuthority('MANAGE_FAMILY_MEMBERS','MANAGE_CLAN')")
    public ResponseEntity<UserResponse> createSpouseForMember(
            @PathVariable String id,
            @RequestBody CreateSpouseRequest request,
            @AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(familyHeadService.createSpouseForMember(id, request, principal.getUserId()));
    }

    @DeleteMapping("/members/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGE_FAMILY_MEMBERS','MANAGE_CLAN')")
    public ResponseEntity<?> deleteMember(@PathVariable String id, @AuthenticationPrincipal User principal) {
        familyHeadService.deleteMember(id, principal.getUserId());
        return ResponseEntity.ok("Đã xóa thành viên thành công.");
    }
}
