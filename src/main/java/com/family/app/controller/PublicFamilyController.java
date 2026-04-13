package com.family.app.controller;

import com.family.app.config.AppClanProperties;
import com.family.app.dto.UserResponse;
import com.family.app.model.Family;
import com.family.app.model.User;
import com.family.app.repository.CategoryRepository;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.UserRepository;
import com.family.app.service.FamilyScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicFamilyController {

    private final FamilyRepository familyRepository;
    private final CategoryRepository categoryRepository;
    private final AppClanProperties clanProperties;
    private final UserRepository userRepository;
    private final FamilyScopeService familyScopeService;

    /**
     * Các dòng họ gốc (không có parent_family_id), chỉ {@code PUBLIC} — dùng chọn dòng họ trên gia phả.
     * Rỗng thì trả về một phần tử fallback từ {@code app.clan}.
     */
    @GetMapping("/families")
    public List<Map<String, String>> getFamilies() {
        List<Map<String, String>> list = new ArrayList<>();
        for (Family f : familyRepository.findByParentFamilyIsNullOrderByFamilyNameAsc()) {
            String ps = f.getPrivacySetting();
            if (ps != null && !ps.isBlank() && !"PUBLIC".equalsIgnoreCase(ps.trim())) {
                continue;
            }
            String id = f.getFamilyId();
            String nm = f.getFamilyName();
            String name = nm != null && !nm.isBlank() ? nm : id;
            list.add(Map.of("id", id, "name", name));
        }
        if (list.isEmpty()) {
            String id = clanProperties.getFamilyId();
            String name = familyRepository.findById(id)
                    .map(Family::getFamilyName)
                    .orElse(clanProperties.getDisplayName());
            return List.of(Map.of("id", id, "name", name));
        }
        return list;
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

        String fromParam = familyId != null ? familyId.trim() : "";
        String fromUser = principal != null && principal.getFamily() != null
                ? principal.getFamily().getFamilyId()
                : null;
        String fromConfig = clanProperties.getFamilyId() != null ? clanProperties.getFamilyId().trim() : "";

        String fid = !fromParam.isBlank() ? fromParam
                : (fromUser != null && !fromUser.isBlank() ? fromUser
                : (!fromConfig.isBlank() ? fromConfig : null));

        if (fid == null || fid.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Không xác định được dòng họ."));
        }

        String root = familyScopeService.resolveRootFamilyId(fid);
        List<String> scope = familyRepository.findDescendantFamilyIds(root);
        if (scope == null || scope.isEmpty()) {
            scope = List.of(root);
        }
        scope = scope.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        if (scope.isEmpty()) {
            scope = List.of(root);
        }

        User head = userRepository.findFamilyHeadByFamilyIds(scope).orElse(null);

        if (head == null) {
            return ResponseEntity.ok(Map.of());
        }

        head = userRepository.findByIdWithFamily(head.getUserId()).orElse(head);

        UserResponse dto = new UserResponse();
        dto.setUserId(head.getUserId());
        dto.setFullName(head.getFullName());
        dto.setGender(head.getGender());
        dto.setBio(head.getBio());
        dto.setAvatar(head.getAvatar());
        dto.setEmail(head.getEmail());
        dto.setPhoneNumber(head.getPhoneNumber());
        dto.setCurrentAddress(head.getCurrentAddress());
        dto.setGeneration(head.getGeneration());
        dto.setOccupation(head.getOccupation());
        if (head.getFamily() != null) {
            dto.setFamilyId(head.getFamily().getFamilyId());
            dto.setFamilyName(head.getFamily().getFamilyName());
        }

        return ResponseEntity.ok(dto);
    }

}
