package com.family.app.controller;

import com.family.app.model.User;
import com.family.app.security.AppPermissions;
import com.family.app.security.UserRoleSupport;
import com.family.app.service.ProfileService;
import com.family.app.dto.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;
    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
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
            Authentication authentication,
            @RequestBody UserResponse updateData) {

        String currentUserId = null;
        boolean canEditOthers = false;
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                currentUserId = user.getUserId();
                if (UserRoleSupport.hasRoleId(user, "role-head")
                        || UserRoleSupport.hasPermissionViaRoles(user, AppPermissions.MANAGE_CLAN)) {
                    canEditOthers = true;
                }
            } else {
                currentUserId = authentication.getName();
            }
            if (!canEditOthers && authentication.getAuthorities() != null) {
                canEditOthers = authentication.getAuthorities().stream()
                        .anyMatch(a -> AppPermissions.FAMILY_HEAD.equals(a.getAuthority())
                                || AppPermissions.MANAGE_CLAN.equals(a.getAuthority())
                                || "ROLE_FAMILY_BRANCH_MANAGER".equals(a.getAuthority()));
            }
        }

        if (currentUserId == null || (!currentUserId.equals(id) && !canEditOthers)) {
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
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {

        String currentUserId = null;
        boolean canEditOthers = false;
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                currentUserId = user.getUserId();
                if (UserRoleSupport.hasRoleId(user, "role-head")
                        || UserRoleSupport.hasPermissionViaRoles(user, AppPermissions.MANAGE_CLAN)) {
                    canEditOthers = true;
                }
            } else {
                currentUserId = authentication.getName();
            }
            if (!canEditOthers && authentication.getAuthorities() != null) {
                canEditOthers = authentication.getAuthorities().stream()
                        .anyMatch(a -> AppPermissions.FAMILY_HEAD.equals(a.getAuthority())
                                || AppPermissions.MANAGE_CLAN.equals(a.getAuthority())
                                || "ROLE_FAMILY_BRANCH_MANAGER".equals(a.getAuthority()));
            }
        }

        if (currentUserId == null || (!currentUserId.equals(id) && !canEditOthers)) {
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