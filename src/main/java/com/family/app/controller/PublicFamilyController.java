package com.family.app.controller;

import com.family.app.model.Family;
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
}
