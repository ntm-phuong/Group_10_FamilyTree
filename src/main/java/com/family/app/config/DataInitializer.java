package com.family.app.config;

import com.family.app.model.Family;
import com.family.app.model.Role;
import com.family.app.model.User;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.RoleRepository;
import com.family.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FamilyRepository familyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Đảm bảo có dòng họ fam-nguyen-001
        Family family = familyRepository.findById("fam-nguyen-001").orElseGet(() -> {
            Family newFamily = Family.builder()
                    .familyId("fam-nguyen-001")
                    .familyName("Dòng họ Nguyễn")
                    .build();
            return familyRepository.save(newFamily);
        });

        // 2. Kiểm tra tài khoản login (ví dụ email: admin@gmail.com)
        if (userRepository.findByEmail("admin@gmail.com").isEmpty()) {

            // Tìm Role ADMIN (Giả sử bạn đã có bảng roles)
            // Nếu chưa có bảng roles, bạn có thể tạm bỏ qua hoặc tạo mới

            User admin = User.builder()
                    .userId("admin-id")
                    .fullName("Admin Dòng Họ")
                    .email("admin@gmail.com")
                    .password(passwordEncoder.encode("123456")) // Mật khẩu là 123456
                    .gender("MALE")
                    .family(family)
                    .generation(1)
                    .branch("Hội đồng")
                    .status(1) // Active
                    .build();

            userRepository.save(admin);
            System.out.println(">>> Đã khởi tạo tài khoản: admin@gmail.com / 123456");
        }
    }
}