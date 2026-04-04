package com.family.app.controller;

import com.family.app.dto.DashboardResponse;
import com.family.app.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/family-head")
public class FamilyHeadDashboardController {
    @Autowired private DashboardService dashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboardData(@RequestParam String familyId) {
        // Tương lai: Lấy familyId từ thông tin đăng nhập của Trưởng họ (JWT)
        return ResponseEntity.ok(dashboardService.getFamilyHeadDashboard(familyId));
    }
}