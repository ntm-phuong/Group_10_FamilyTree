package com.family.app.controller;

import com.family.app.dto.HomeResponse;
import com.family.app.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/family-head")
public class FamilyHeadDashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/dashboard/{familyId}")
    public ResponseEntity<HomeResponse> getDashboardApi(@PathVariable String familyId) {
        // Trả về dữ liệu JSON cho JavaScript fetch
        HomeResponse data = dashboardService.getDashboardData(familyId);
        return ResponseEntity.ok(data);
    }
}