package com.family.app.service;

import com.family.app.model.User;
import com.family.app.repository.UserRepository;
import com.family.app.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider tokenProvider;

    public Map<String, Object> authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        if (passwordEncoder.matches(password, user.getPassword())) {
            String token = tokenProvider.generateToken(user.getUserId());

            Map<String, Object> authData = new HashMap<>();
            authData.put("token", token);
            authData.put("userId", user.getUserId());
            authData.put("fullName", user.getFullName());

            // Lấy tên Role (Giả định Role của Phú có trường roleName)
            if (user.getRole() != null) {
                authData.put("role", user.getRole().getRoleName());
            }

            return authData;
        } else {
            throw new RuntimeException("Mật khẩu không chính xác");
        }
    }
}
