package com.family.app.controller;

import com.family.app.dto.LoginRequest;
import com.family.app.model.User;
import com.family.app.service.AuthService;
import com.family.app.security.JwtTokenProvider; // Thêm dòng import này để gọi công cụ Token
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


    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Map<String, Object> authData = authService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", authData.get("token"));
        response.put("status", authData.get("status"));
        response.put("tokenType", "Bearer");
        response.put("userId", authData.get("userId"));
        response.put("fullName", authData.get("fullName"));
        response.put("role", authData.get("role"));
        response.put("familyId", authData.get("familyId"));


        return ResponseEntity.ok(response);
    }

    @PostMapping("/active")
    public ResponseEntity<?> activeUser(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            String password = request.get("password");

            // 1. Gọi service đổi mật khẩu và đổi status = 2
            authService.activateUser(userId, password);

            // 2. TẠO TOKEN MỚI TINH (Vào thẳng Home)
            String newToken = jwtTokenProvider.generateToken(userId);

            // 3. Trả token mới về cho Frontend
            return ResponseEntity.ok().body(Map.of("accessToken", newToken));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            authService.generateAndSendOtp(email);
            return ResponseEntity.ok().body(Map.of("message", "Đã gửi OTP thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String otp = request.get("otp");
            String newPassword = request.get("newPassword");

            // 1. Đổi mật khẩu
            authService.resetPasswordWithOtp(email, otp, newPassword);

            // 2. Lấy thông tin user thông qua Service
            User user = authService.getUserByEmail(email);

            // 3. Tạo Token mới để đăng nhập luôn
            String newToken = jwtTokenProvider.generateToken(user.getUserId());

            // 4. Trả về kết quả cho Frontend
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đổi mật khẩu thành công!");
            response.put("accessToken", newToken);
            response.put("fullName", user.getFullName());
            response.put("userId", user.getUserId());
            response.put("familyId", user.getFamily() != null ? user.getFamily().getFamilyId() : null);

            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String otp = request.get("otp");

            // Gọi Service để kiểm tra
            authService.verifyOtp(email, otp);

            return ResponseEntity.ok().body(Map.of("message", "Mã OTP hợp lệ!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}