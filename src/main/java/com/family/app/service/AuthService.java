package com.family.app.service;

import com.family.app.model.Permission;
import com.family.app.model.User;
import com.family.app.repository.UserRepository;
import com.family.app.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtTokenProvider tokenProvider;
    @Autowired
    private EmailService emailService;

    public void generateAndSendOtp(String email) {
        // 1. Tìm user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống!"));

        // 2. Tạo mã OTP 6 số ngẫu nhiên
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        // 3. Set thời gian hết hạn (ví dụ: 5 phút kể từ lúc tạo)
        user.setOtpCode(otp);
        user.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        // 4. Gửi email
        emailService.sendOtpEmail(email, otp);
    }
    public void verifyOtp(String email, String otp) {
        // 1. Tìm user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));

        // 2. Kiểm tra mã OTP có khớp không
        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            throw new RuntimeException("Mã OTP không chính xác!");
        }

        // 3. Kiểm tra mã OTP đã hết hạn chưa
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Mã OTP đã hết hạn!");
        }

        // Nếu code chạy qua được đây nghĩa là OTP hợp lệ, không ném ra lỗi nào cả.
    }
    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        // 1. Tìm user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại!"));

        // 2. Kiểm tra mã OTP có khớp không
        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            throw new RuntimeException("Mã OTP không chính xác!");
        }

        // 3. Kiểm tra mã OTP đã hết hạn chưa (Quá 5 phút)
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Mã OTP đã hết hạn!");
        }

        // 4. Vượt qua hết bài test -> Tiến hành đổi mật khẩu
        user.setPassword(passwordEncoder.encode(newPassword));

        // 5. Cực kỳ quan trọng: Xóa mã OTP đi để mã này không bị xài lại lần 2
        user.setOtpCode(null);
        user.setOtpExpiry(null);

        // 6. Đổi status sang 2 (Đã kích hoạt) phòng trường hợp user chưa active mà quên pass
        user.setStatus(2);

        // 7. Lưu vào Database
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> authenticate(String email, String password) {
        User user = userRepository.findByEmailWithFamily(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại"));

        if (passwordEncoder.matches(password, user.getPassword())) {
            String token = tokenProvider.generateToken(user.getUserId());

            Map<String, Object> authData = new HashMap<>();
            authData.put("token", token);
            authData.put("userId", user.getUserId());
            authData.put("fullName", user.getFullName());

            if (user.getFamily() != null) {
                authData.put("familyId", user.getFamily().getFamilyId());
                authData.put("familyName", user.getFamily().getFamilyName());
            }

            if (user.getRole() != null) {
                authData.put("role", user.getRole().getRoleName());
                if (user.getRole().getPermissions() != null) {
                    List<String> permNames = user.getRole().getPermissions().stream()
                            .map(Permission::getName)
                            .filter(n -> n != null && !n.isBlank())
                            .collect(Collectors.toList());
                    authData.put("permissions", permNames);
                }
            }

            return authData;
        } else {
            throw new RuntimeException("Mật khẩu không chính xác");
        }
    }
    public void activateUser(String userId, String newPassword) {
        // Tìm user trong DB
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // 1. Mã hóa mật khẩu mới (Dùng BCrypt)
        user.setPassword(passwordEncoder.encode(newPassword));

        // 2. Chuyển status sang 2 (Đã kích hoạt)
        user.setStatus(2);

        // 3. Lưu xuống DB
        userRepository.save(user);
    }
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    /** Hồ sơ tối giản cho FE (sau khi đã xác thực JWT). */
    @Transactional(readOnly = true)
    public Map<String, Object> buildSessionProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        Map<String, Object> m = new HashMap<>();
        m.put("userId", user.getUserId());
        m.put("fullName", user.getFullName());
        m.put("email", user.getEmail());
        if (user.getRole() != null) {
            m.put("role", user.getRole().getRoleName());
            if (user.getRole().getPermissions() != null) {
                m.put("permissions", user.getRole().getPermissions().stream()
                        .map(Permission::getName)
                        .filter(n -> n != null && !n.isBlank())
                        .collect(Collectors.toList()));
            }
        }
        if (user.getFamily() != null) {
            m.put("familyId", user.getFamily().getFamilyId());
            m.put("familyName", user.getFamily().getFamilyName());
        }
        return m;
    }
}
