package com.family.app.controller;

import com.family.app.dto.DashboardResponse;
import com.family.app.model.User;
import com.family.app.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/family-head/dashboard")
public class FamilyHeadDashboardController {
    @Autowired private DashboardService dashboardService;

    @GetMapping("")
    public ResponseEntity<DashboardResponse> getDashboardData() {
        // Lấy object User từ Filter đã xác thực
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof User) {
            User currentUser = (User) principal;
            return ResponseEntity.ok(dashboardService.getFamilyHeadDashboard(currentUser));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

}