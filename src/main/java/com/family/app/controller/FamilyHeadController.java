package com.family.app.controller;

import com.family.app.dto.UserRequest;
import com.family.app.dto.UserResponse;
import com.family.app.service.FamilyHeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family-head")
@PreAuthorize("hasRole('FAMILY_HEAD')")
public class FamilyHeadController {

    @Autowired private FamilyHeadService memberService;
    @Autowired private FamilyHeadService familyHeadService;

    @GetMapping("/members")
    public ResponseEntity<List<UserResponse>> getAllMembers() {
        return ResponseEntity.ok(memberService.getAllMembers());
    }

    @PostMapping("/members")
    public ResponseEntity<?> addMember(@RequestBody UserRequest request) {
        return ResponseEntity.ok(memberService.saveMember(request));
    }

    @PutMapping("/members/{id}")
    public ResponseEntity<?> updateMember(@PathVariable String id, @RequestBody UserRequest request) {
        return ResponseEntity.ok(memberService.updateMember(id, request));
    }

    @DeleteMapping("/members/{id}")
    public ResponseEntity<?> deleteMember(@PathVariable String id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok("Đã xóa thành viên thành công.");
    }
}