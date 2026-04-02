package com.family.app.controller;

import com.family.app.dto.HomeResponse;
import com.family.app.service.FamilyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/families")
public class HomeController {

    @Autowired
    private FamilyService familyService;

    @GetMapping("/{id}/home")
    public ResponseEntity<HomeResponse> getHomeData(@PathVariable String id) {
        return ResponseEntity.ok(familyService.getHomeData(id));
    }
}