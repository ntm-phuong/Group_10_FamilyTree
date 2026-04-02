package com.family.app.config;

import com.family.app.model.Family;
import com.family.app.model.Role;
import com.family.app.model.User;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.RoleRepository;
import com.family.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Khởi tạo Role (Dùng saveAndFlush để tránh lỗi ConstraintViolation)
        Role headRole = roleRepository.findByRoleName("FAMILY_HEAD")
                .orElseGet(() -> {
                    Role r = Role.builder()
                            .roleId("role-head") // Sửa từ .id() thành .roleId() cho khớp Entity
                            .roleName("FAMILY_HEAD")
                            .build();
                    return roleRepository.saveAndFlush(r);
                });

        Role memberRole = roleRepository.findByRoleName("MEMBER")
                .orElseGet(() -> {
                    Role r = Role.builder()
                            .roleId("role-member") // Sửa thành .roleId()
                            .roleName("MEMBER")
                            .build();
                    return roleRepository.saveAndFlush(r);
                });

        // 2. Khởi tạo Family mẫu (Bắt buộc vì User cần Object Family)
        Family defaultFamily = familyRepository.findById("fam-test-tree-100")
                .orElseGet(() -> {
                    Family f = new Family();
                    f.setFamilyId("fam-test-tree-100");
                    f.setFamilyName("Họ Nguyễn Đông Anh");
                    return familyRepository.saveAndFlush(f);
                });

        // 3. Khởi tạo tài khoản TRƯỞNG HỌ mẫu để test Postman
//        if (userRepository.findByEmail("truongho@giapha.vn").isEmpty()) {
//            User head = User.builder()
//                    .userId(UUID.randomUUID().toString())
//                    .fullName("Nguyễn Văn Trưởng")
//                    .email("truongho@giapha.vn")
//                    .password(passwordEncoder.encode("123456"))
//                    .status(1) // Active
//                    .generation(1)
//                    .orderInFamily(1)
//                    .role(headRole)    // Gán Object Role
//                    .family(defaultFamily) // Gán Object Family
//                    .build();
//
//            userRepository.save(head);
//            System.out.println("----------------------------------------------------------");
//            System.out.println(">>> ĐÃ TẠO TÀI KHOẢN TEST: truongho@giapha.vn / 123456");
//            System.out.println("----------------------------------------------------------");
//        }
    }
}