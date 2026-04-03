package com.family.app.controller;

import com.family.app.model.Family;
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

    @GetMapping("/families")
    public List<Map<String, String>> getFamilies() {
        return familyRepository.findAll()
                .stream()
                .map(family -> Map.of(
                        "id", family.getFamilyId(),
                        "name", family.getFamilyName()
                ))
                .toList();
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
