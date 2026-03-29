package com.family.app.controller;

import com.family.app.dto.LoginRequest;
import com.family.app.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Map<String, Object> authData = authService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", authData.get("token"));
        response.put("tokenType", "Bearer");
        response.put("userId", authData.get("userId"));
        response.put("fullName", authData.get("fullName"));
        response.put("role", authData.get("role"));

        return ResponseEntity.ok(response);
    }
}