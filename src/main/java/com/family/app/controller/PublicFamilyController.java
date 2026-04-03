package com.family.app.controller;

import com.family.app.config.AppClanProperties;
import com.family.app.repository.CategoryRepository;
import com.family.app.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
