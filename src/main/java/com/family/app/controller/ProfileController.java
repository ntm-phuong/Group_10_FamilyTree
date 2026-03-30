package com.family.app.controller;

import com.family.app.model.User;
import com.family.app.service.ProfileService;
import com.family.app.dto.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;
    @GetMapping("/{id}")
    public ResponseEntity<?> getProfile(@PathVariable String id) {
        try {
            UserResponse response = profileService.getProfileById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser,
            @RequestBody UserResponse updateData) {

        if (currentUser == null || !currentUser.getUserId().equals(id)) {
            return ResponseEntity.status(403).body("Bạn không có quyền chỉnh sửa hồ sơ của người khác!");
        }

        try {
            UserResponse updatedProfile = profileService.updateProfile(id, updateData);
            return ResponseEntity.ok(updatedProfile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/{id}/upload-avatar")
    public ResponseEntity<?> uploadAvatar(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file) {

        if (currentUser == null || !currentUser.getUserId().equals(id)) {
            return ResponseEntity.status(403).body("Không có quyền!");
        }
        try {
            String avatarUrl = profileService.uploadAvatarToCloudinary(file);
            UserResponse response = profileService.updateAvatar(id, avatarUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Lỗi khi upload lên Cloudinary: " + e.getMessage());
        }
    }
}