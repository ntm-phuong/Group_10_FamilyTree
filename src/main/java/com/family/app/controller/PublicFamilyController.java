package com.family.app.controller;

import com.family.app.config.AppClanProperties;
import com.family.app.dto.UserResponse;
import com.family.app.model.User;
import com.family.app.repository.CategoryRepository;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicFamilyController {

    private final FamilyRepository familyRepository;
    private final CategoryRepository categoryRepository;
    private final AppClanProperties clanProperties;
    private final UserRepository userRepository;

    /** Một bản ghi — dòng họ cấu hình ứng dụng. */
    @GetMapping("/families")
    public List<Map<String, String>> getFamilies() {
        String id = clanProperties.getFamilyId();
        String name = familyRepository.findById(id)
                .map(f -> f.getFamilyName())
                .orElse(clanProperties.getDisplayName());
        return List.of(Map.of("id", id, "name", name));
    }

    /** Danh mục tin nội bộ (dropdown CRUD tin / sự kiện dòng họ). */
    @GetMapping("/categories")
    public List<Map<String, String>> getCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(c -> Map.of(
                        "id", c.getCategoryId(),
                        "name", c.getName()
                ))
                .toList();
    }
    @Transactional(readOnly = true)
    @GetMapping("/family-head-info")
    public ResponseEntity<?> getFamilyHeadInfo(
            @RequestParam(required = false) String familyId,
            @AuthenticationPrincipal User principal) {

        String fid = (familyId != null && !familyId.isBlank())
                ? familyId.trim()
                : (principal != null && principal.getFamily() != null
                   ? principal.getFamily().getFamilyId()
                   : null);

        if (fid == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không xác định được dòng họ."));
        }

        User head = userRepository.findFamilyHeadByFamilyId(fid).orElse(null);

        if (head == null) {
            return ResponseEntity.ok(Map.of());
        }

        UserResponse dto = new UserResponse();
        dto.setUserId(head.getUserId());
        dto.setFullName(head.getFullName());
        dto.setGender(head.getGender());
        dto.setBio(head.getBio());
        dto.setAvatar(head.getAvatar());
        dto.setGeneration(head.getGeneration());
        dto.setOccupation(head.getOccupation());
        if (head.getFamily() != null) {
            dto.setFamilyId(head.getFamily().getFamilyId());
            dto.setFamilyName(head.getFamily().getFamilyName());
        }

        return ResponseEntity.ok(dto);
    }

}
