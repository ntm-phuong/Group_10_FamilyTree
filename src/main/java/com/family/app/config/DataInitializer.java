package com.family.app.config;

import com.family.app.model.Family;
import com.family.app.model.Permission;
import com.family.app.model.Role;
import com.family.app.model.User;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.PermissionRepository;
import com.family.app.repository.RoleRepository;
import com.family.app.repository.UserRepository;
import com.family.app.security.AppPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Seed role, permission, dòng họ gốc và tài khoản mẫu (idempotent).
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private FamilyRepository familyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        /* Quyền “site” vẫn có trong DB nhưng seed không gán cho ai — không tài khoản nào quản trị toàn bộ. */
        ensurePermission(
                AppPermissions.MANAGE_SITE_NEWS,
                "Tin /news theo từng dòng họ (không dùng quyền global trong seed)."
        );
        Permission manageNews = ensurePermission(
                AppPermissions.MANAGE_FAMILY_NEWS,
                "Quản lý tin / sự kiện trong phạm vi dòng họ của tài khoản và các chi con (nhánh dưới)."
        );
        Permission manageMembers = ensurePermission(
                AppPermissions.MANAGE_FAMILY_MEMBERS,
                "Quản lý thành viên và danh sách chi — phạm vi dòng họ của tài khoản và các chi con (nhánh dưới)."
        );

        Role headRole = ensureRoleWithPermissions("FAMILY_HEAD", "role-head", Set.of(manageNews, manageMembers));
        Role memberRole = ensureRoleWithPermissions("MEMBER", "role-member", Set.of());

        Family rootFamily = ensureRootFamily();

        Family defaultFamily = familyRepository.findById("fam-001").orElseGet(() -> {
            Family f = new Family();
            f.setFamilyId("fam-001");
            f.setFamilyName("Họ Nguyễn Đông Anh");
            f.setPrivacySetting("PUBLIC");
            f.setParentFamily(rootFamily);
            return familyRepository.saveAndFlush(f);
        });
        if (defaultFamily.getParentFamily() == null) {
            defaultFamily.setParentFamily(rootFamily);
            familyRepository.save(defaultFamily);
        }

        if (familyRepository.findById("fam-nguyen-001").isEmpty()) {
            Family f = new Family();
            f.setFamilyId("fam-nguyen-001");
            f.setFamilyName("Dòng họ Nguyễn (seed)");
            f.setPrivacySetting("PUBLIC");
            f.setParentFamily(rootFamily);
            familyRepository.saveAndFlush(f);
        } else {
            familyRepository.findById("fam-nguyen-001").ifPresent(f -> {
                if (f.getParentFamily() == null) {
                    f.setParentFamily(rootFamily);
                    familyRepository.save(f);
                }
            });
        }

        if (familyRepository.findById("fam-mock-001").isEmpty()) {
            Family f = new Family();
            f.setFamilyId("fam-mock-001");
            f.setFamilyName("Dòng họ mock (seed)");
            f.setDescription("Chi họ phục vụ thử nghiệm");
            f.setPrivacySetting("PUBLIC");
            f.setParentFamily(rootFamily);
            familyRepository.saveAndFlush(f);
        } else {
            familyRepository.findById("fam-mock-001").ifPresent(f -> {
                if (f.getParentFamily() == null) {
                    f.setParentFamily(rootFamily);
                    familyRepository.save(f);
                }
            });
        }

        if (userRepository.findByEmail("truongho@giapha.vn").isEmpty()) {
            User head = User.builder()
                    .userId(UUID.randomUUID().toString())
                    .fullName("Nguyễn Văn Trưởng")
                    .email("truongho@giapha.vn")
                    .password(passwordEncoder.encode("123456"))
                    .status(1)
                    .generation(1)
                    .orderInFamily(1)
                    .role(headRole)
                    .family(defaultFamily)
                    .build();
            userRepository.save(head);
            log.info("[DataInitializer] Đã tạo tài khoản trưởng họ: truongho@giapha.vn / 123456");
        }

        if (userRepository.findByEmail("admin@giapha.vn").isEmpty()) {
            Family famNguyen = familyRepository.findById("fam-nguyen-001").orElseThrow();
            User admin = User.builder()
                    .userId("user-admin-seed")
                    .fullName("Trưởng họ — chi Nguyễn (seed)")
                    .email("admin@giapha.vn")
                    .password(passwordEncoder.encode("123456"))
                    .status(1)
                    .generation(1)
                    .orderInFamily(1)
                    .role(headRole)
                    .family(famNguyen)
                    .build();
            userRepository.save(admin);
            log.info("[DataInitializer] Tài khoản trưởng họ (chi Nguyễn seed): admin@giapha.vn / 123456 — chỉ fam-nguyen-001");
        }

        if (userRepository.findByEmail("member@giapha.vn").isEmpty()) {
            User mem = User.builder()
                    .userId(UUID.randomUUID().toString())
                    .fullName("Thành viên mẫu")
                    .email("member@giapha.vn")
                    .password(passwordEncoder.encode("123456"))
                    .status(1)
                    .role(memberRole)
                    .family(defaultFamily)
                    .build();
            userRepository.save(mem);
            log.info("[DataInitializer] Đã tạo thành viên mẫu: member@giapha.vn / 123456");
        }

        if (userRepository.findByEmail("truongroot@giapha.vn").isEmpty()) {
            User rootHead = User.builder()
                    .userId("user-truongroot-seed")
                    .fullName("Trưởng họ — dòng gốc (fam-root)")
                    .email("truongroot@giapha.vn")
                    .password(passwordEncoder.encode("123456"))
                    .status(1)
                    .generation(1)
                    .orderInFamily(1)
                    .role(headRole)
                    .family(rootFamily)
                    .build();
            userRepository.save(rootHead);
            log.info("[DataInitializer] Trưởng họ fam-root: truongroot@giapha.vn / 123456");
        }
    }

    private Permission ensurePermission(String name, String description) {
        return permissionRepository.findByName(name).orElseGet(() -> {
            Permission p = new Permission();
            p.setName(name);
            p.setDescription(description);
            return permissionRepository.saveAndFlush(p);
        });
    }

    private Role ensureRoleWithPermissions(String roleName, String roleId, Set<Permission> permissions) {
        Role r = roleRepository.findByRoleName(roleName).orElseGet(() -> {
            Role x = new Role();
            x.setRoleId(roleId);
            x.setRoleName(roleName);
            return roleRepository.saveAndFlush(x);
        });
        r.setPermissions(new HashSet<>(permissions));
        return roleRepository.save(r);
    }

    private Family ensureRootFamily() {
        return familyRepository.findById("fam-root").orElseGet(() -> {
            Family f = new Family();
            f.setFamilyId("fam-root");
            f.setFamilyName("Họ Nguyễn — toàn hệ (dòng họ gốc)");
            f.setDescription("Một dòng họ lớn: các chi (fam-001, fam-nguyen-001, …) là nhánh con.");
            f.setPrivacySetting("PUBLIC");
            return familyRepository.saveAndFlush(f);
        });
    }

}
