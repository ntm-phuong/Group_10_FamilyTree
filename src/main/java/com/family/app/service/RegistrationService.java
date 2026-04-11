package com.family.app.service;

import com.family.app.dto.FamilyRegisterRequest;
import com.family.app.model.Family;
import com.family.app.model.Role;
import com.family.app.model.User;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.RoleRepository;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Random;

@Service
public class RegistrationService {

    @Autowired
    private FamilyRepository familyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public void registerFamily(FamilyRegisterRequest req) {
        if (req == null) throw new RuntimeException("Thiếu dữ liệu đăng ký");
        if (req.getEmail() == null || req.getEmail().isBlank()) throw new RuntimeException("Email bắt buộc");
        if (userRepository.findByEmail(req.getEmail()).isPresent()) throw new RuntimeException("Email đã tồn tại");

        Family family = Family.builder()
                .familyName(req.getFamilyName() != null ? req.getFamilyName().trim() : "Dòng họ")
                .privacySetting("PUBLIC")
                .build();
        family = familyRepository.save(family);

        User user = new User();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setFamily(family);
        user.setStatus(0); // pending

        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusHours(24));

        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), otp, user.getOtpExpiry());
    }

    @Transactional
    public void verifyEmail(String email, String code) {
        if (email == null || email.isBlank()) throw new RuntimeException("Email bắt buộc");
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
        if (user.getOtpCode() == null || !user.getOtpCode().equals(code)) {
            throw new RuntimeException("Mã xác thực không chính xác");
        }
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã xác thực đã hết hạn");
        }

        // activate
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setStatus(2);

        Role role = roleRepository.findByRoleName("ADMIN").orElse(null);
        if (role != null) {
            user.setRoles(new HashSet<>(java.util.Set.of(role)));
        }

        userRepository.save(user);

        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName(), user.getFamily() != null ? user.getFamily().getFamilyName() : null);
    }
}