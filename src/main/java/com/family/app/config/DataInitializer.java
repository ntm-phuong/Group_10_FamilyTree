package com.family.app.config;

import com.family.app.model.Family;
import com.family.app.model.Permission;
import com.family.app.model.Relationship;
import com.family.app.model.Role;
import com.family.app.model.User;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.PermissionRepository;
import com.family.app.repository.RelationshipRepository;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Seed quyền, vai trò, một dòng họ gốc + các chi phụ thuộc (parent_family_id) và 10 thế hệ mẫu.
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** Chi phụ thuộc trực tiếp tổ tông — thế hệ II–V. */
    private static final String SEED_CHI_PHU_KE = "seed-chi-phu-ke";
    /** Chi lồng — thế hệ VI–VIII. */
    private static final String SEED_CHI_TIEU = "seed-chi-tieu";
    /** Chi lồng — thế hệ IX–X. */
    private static final String SEED_CHI_DOI_TRE = "seed-chi-doi-tre";

    /** Một tài khoản ADMIN duy nhất — email trưởng họ chính thức (quyền {@link AppPermissions#FAMILY_HEAD}). */
    private static final String SEED_ADMIN_USER_ID = "seed-system-admin-01";
    private static final String SEED_ADMIN_EMAIL = "truongho@giapha.vn";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private FamilyRepository familyRepository;
    @Autowired
    private RelationshipRepository relationshipRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AppClanProperties clanProperties;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        Permission siteNewsPerm = ensurePermission(
                AppPermissions.MANAGE_SITE_NEWS,
                "Tin /news toàn site (ADMIN)."
        );
        Permission manageNews = ensurePermission(
                AppPermissions.MANAGE_FAMILY_NEWS,
                "Quản lý tin / sự kiện trong phạm vi dòng họ."
        );
        Permission manageMembers = ensurePermission(
                AppPermissions.MANAGE_FAMILY_MEMBERS,
                "Quản lý thành viên trong phạm vi dòng họ."
        );
        Permission manageClan = ensurePermission(
                AppPermissions.MANAGE_CLAN,
                "Toàn bộ chi trong cây phả hệ (gốc app.clan.family-id) — chỉ role ADMIN."
        );
        Permission familyHeadPerm = ensurePermission(
                AppPermissions.FAMILY_HEAD,
                "Trưởng họ chính thức — chỉ gán cho tài khoản ADMIN (email trưởng họ)."
        );

        Role branchLeadRole = ensureRoleById("role-head", "FAMILY_BRANCH_MANAGER",
                Set.of(manageNews, manageMembers));
        Role newsMgrRole = ensureRoleWithPermissions("FAMILY_NEWS_MANAGER", "role-news-mgr", Set.of(manageNews));
        Role memberRole = ensureRoleWithPermissions("MEMBER", "role-member", Set.of());
        Role adminRole = ensureRoleById("role-admin", "ADMIN",
                Set.of(siteNewsPerm, manageNews, manageMembers, manageClan, familyHeadPerm));

        String fid = clanProperties.getFamilyId();
        Family clan = familyRepository.findById(fid).orElseGet(() -> {
            Family f = new Family();
            f.setFamilyId(fid);
            f.setFamilyName(clanProperties.getDisplayName());
            f.setDescription(ClanBranding.rootClanDescription(clanProperties.getDisplayName()));
            f.setPrivacySetting("PUBLIC");
            f.setParentFamily(null);
            return familyRepository.saveAndFlush(f);
        });
        clan.setParentFamily(null);
        clan.setFamilyName(clanProperties.getDisplayName());
        clan.setDescription(ClanBranding.rootClanDescription(clanProperties.getDisplayName()));
        familyRepository.save(clan);

        Family chiPhuKe = ensureBranchFamily(
                SEED_CHI_PHU_KE,
                "Chi phả kế (thôn Đông Anh)",
                "Thế hệ II–V — phụ thuộc trực tiếp tổ tông.",
                clan
        );
        Family chiTieu = ensureBranchFamily(
                SEED_CHI_TIEU,
                "Chi thứ — nhánh trưởng tộc",
                "Thế hệ VI–VIII — chi con của phả kế.",
                chiPhuKe
        );
        Family chiDoiTre = ensureBranchFamily(
                SEED_CHI_DOI_TRE,
                "Chi đời trẻ",
                "Thế hệ IX–X — chi con của chi thứ.",
                chiTieu
        );

        seedTenGenerations(clan, chiPhuKe, chiTieu, chiDoiTre, branchLeadRole, newsMgrRole, memberRole);

        syncBranchLeadDemoIdentity();
        ensureAndSyncAdminAccount(clan, adminRole);

        migrateLegacySingleRoleColumnToUserRoles();

        long adminCount = userRepository.countDistinctByRoleName("ADMIN");
        if (adminCount > 1) {
            log.warn("[DataInitializer] Cảnh báo: có {} tài khoản ADMIN — hệ thống thiết kế chỉ 1. Kiểm tra DB.", adminCount);
        }

        log.info("[DataInitializer] Dòng họ {} + 3 chi. Email trưởng họ (ADMIN): {} / 123456 | Phụ trách nhánh: nguyenvantruong@giapha.vn / 123456",
                fid, SEED_ADMIN_EMAIL);
    }

    /** Đồng bộ tài khoản phụ trách nhánh (không dùng email trưởng họ — email đó chỉ cho ADMIN). */
    private void syncBranchLeadDemoIdentity() {
        userRepository.findById("seed-clan-m-07").ifPresent(u -> {
            u.setFullName("Nguyễn Văn Trưởng");
            u.setEmail("nguyenvantruong@giapha.vn");
            roleRepository.findById("role-head").ifPresent(br ->
                    roleRepository.findById("role-news-mgr").ifPresent(nm ->
                            u.setRoles(new HashSet<>(Set.of(br, nm)))));
            userRepository.save(u);
        });
    }

    /**
     * Đồng bộ tài khoản ADMIN ({@link #SEED_ADMIN_USER_ID}): email trưởng họ + quyền đầy đủ.
     * Chỉ tạo mới nếu chưa có bất kỳ user nào mang role ADMIN.
     */
    private void ensureAndSyncAdminAccount(Family clanRoot, Role adminRole) {
        userRepository.findById(SEED_ADMIN_USER_ID).ifPresent(u -> {
            u.setEmail(SEED_ADMIN_EMAIL);
            u.setFullName("Ban trưởng họ (quản trị hệ thống)");
            u.setFamily(clanRoot);
            u.setRoles(new HashSet<>(Set.of(adminRole)));
            if (u.getPassword() == null || u.getPassword().isBlank()) {
                u.setPassword(passwordEncoder.encode("123456"));
            }
            u.setStatus(2);
            userRepository.save(u);
        });
        if (userRepository.findById(SEED_ADMIN_USER_ID).isPresent()) {
            return;
        }
        long existingAdmins = userRepository.countDistinctByRoleName("ADMIN");
        if (existingAdmins >= 1) {
            return;
        }
        userRepository.save(User.builder()
                .userId(SEED_ADMIN_USER_ID)
                .fullName("Ban trưởng họ (quản trị hệ thống)")
                .email(SEED_ADMIN_EMAIL)
                .password(passwordEncoder.encode("123456"))
                .gender("MALE")
                .family(clanRoot)
                .generation(1)
                .orderInFamily(0)
                .roles(new HashSet<>(Set.of(adminRole)))
                .status(2)
                .build());
    }

    /**
     * Copy cột users.role_id (mô hình cũ) sang user_roles chỉ khi cột đó còn tồn tại.
     * Nếu Hibernate đã drop role_id thì không chạy INSERT — tránh SQL 1054 và transaction rollback-only.
     */
    private void migrateLegacySingleRoleColumnToUserRoles() {
        try {
            Object colCnt = entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'role_id'"
            ).getSingleResult();
            long hasLegacyColumn = colCnt instanceof Number ? ((Number) colCnt).longValue() : 0L;
            if (hasLegacyColumn == 0) {
                return;
            }
            int n = entityManager.createNativeQuery(
                    "INSERT IGNORE INTO user_roles (user_id, role_id) "
                            + "SELECT user_id, role_id FROM users WHERE role_id IS NOT NULL"
            ).executeUpdate();
            if (n > 0) {
                log.info("[DataInitializer] Đã copy {} dòng từ users.role_id sang user_roles.", n);
            }
        } catch (Exception e) {
            log.warn("[DataInitializer] Migrate user_roles bỏ qua: {}", e.getMessage());
        }
    }

    private Family ensureBranchFamily(String familyId, String name, String description, Family parent) {
        return familyRepository.findById(familyId).map(f -> {
            f.setParentFamily(parent);
            if (f.getFamilyName() == null || f.getFamilyName().isBlank()) {
                f.setFamilyName(name);
            }
            if (f.getDescription() == null || f.getDescription().isBlank()) {
                f.setDescription(description);
            }
            return familyRepository.save(f);
        }).orElseGet(() -> {
            Family f = new Family();
            f.setFamilyId(familyId);
            f.setFamilyName(name);
            f.setDescription(description);
            f.setPrivacySetting("PUBLIC");
            f.setParentFamily(parent);
            return familyRepository.saveAndFlush(f);
        });
    }

    private static Family familyForGenerationIndex(int i, Family root, Family chiPhuKe, Family chiTieu, Family chiDoiTre) {
        if (i == 0) {
            return root;
        }
        if (i <= 4) {
            return chiPhuKe;
        }
        if (i <= 7) {
            return chiTieu;
        }
        return chiDoiTre;
    }

    private static String branchLabelForGen(int i) {
        if (i == 0) {
            return "Tổ tông (gốc)";
        }
        if (i <= 4) {
            return "Chi phả kế";
        }
        if (i <= 7) {
            return "Chi thứ";
        }
        return "Chi đời trẻ";
    }

    private void seedTenGenerations(
            Family root,
            Family chiPhuKe,
            Family chiTieu,
            Family chiDoiTre,
            Role branchLeadRole,
            Role newsMgrRole,
            Role memberRole
    ) {
        final int n = 10;
        String[][] males = new String[][]{
                {"seed-clan-m-01", "Nguyễn Văn Khiêm", "1886-02-11", "1958-04-20", "Nông dân, trưởng tộc", "0903000001", ""},
                {"seed-clan-m-02", "Nguyễn Văn Lợi", "1900-06-03", "1972-09-01", "Thợ mộc", "0903000002", ""},
                {"seed-clan-m-03", "Nguyễn Văn Thịnh", "1914-01-19", "1985-11-10", "Giáo làng", "0903000003", ""},
                {"seed-clan-m-04", "Nguyễn Văn Bình", "1928-07-25", "1998-02-14", "Công chức xã", "0903000004", ""},
                {"seed-clan-m-05", "Nguyễn Văn Tám", "1942-03-08", "", "Bộ đội xuất ngũ", "0903000005", ""},
                {"seed-clan-m-06", "Nguyễn Văn Hải", "1956-10-30", "", "Kỹ sư xây dựng", "0903000006", ""},
                {"seed-clan-m-07", "Nguyễn Văn Trưởng", "1970-05-17", "", "Phụ trách chi (hưu)", "0903000007", "nguyenvantruong@giapha.vn"},
                {"seed-clan-m-08", "Nguyễn Văn Minh", "1984-12-01", "", "Kế toán doanh nghiệp", "0903000008", ""},
                {"seed-clan-m-09", "Nguyễn Văn Quân", "1998-08-22", "", "Lập trình viên", "0903000009", ""},
                {"seed-clan-m-10", "Nguyễn Văn Phúc", "2012-01-05", "", "Học sinh THCS", "0903000010", "member@giapha.vn"}
        };
        String[][] females = new String[][]{
                {"seed-clan-f-01", "Nguyễn Thị Nhàn", "1888-09-22", "1960-01-15", "Tết cửa hàng tạp hóa", "0903010001"},
                {"seed-clan-f-02", "Nguyễn Thị Sen", "1902-04-14", "1975-06-20", "", "0903010002"},
                {"seed-clan-f-03", "Nguyễn Thị Huệ", "1916-11-07", "1988-03-03", "", "0903010003"},
                {"seed-clan-f-04", "Nguyễn Thị Lan", "1930-02-28", "2001-07-19", "", "0903010004"},
                {"seed-clan-f-05", "Nguyễn Thị Cúc", "1944-06-16", "", "", "0903010005"},
                {"seed-clan-f-06", "Nguyễn Thị Hồng", "1958-01-09", "", "Y tá", "0903010006"},
                {"seed-clan-f-07", "Nguyễn Thị Thu", "1972-08-14", "", "Giáo viên tiểu học", "0903010007"},
                {"seed-clan-f-08", "Nguyễn Thị Mai", "1986-03-29", "", "Nhân viên ngân hàng", "0903010008"},
                {"seed-clan-f-09", "Nguyễn Thị Ngọc", "2000-10-11", "", "Sinh viên", "0903010009"},
                {"seed-clan-f-10", "Nguyễn Thị An", "2014-05-27", "", "Học sinh tiểu học", "0903010010"}
        };

        User[] m = new User[n];
        User[] f = new User[n];
        for (int i = 0; i < n; i++) {
            int gen = i + 1;
            Family fam = familyForGenerationIndex(i, root, chiPhuKe, chiTieu, chiDoiTre);
            String branch = branchLabelForGen(i);
            String[] mr = males[i];
            String[] fr = females[i];
            String loginEmail = mr[6] != null && !mr[6].isBlank() ? mr[6].trim() : null;
            String emailM = loginEmail != null ? loginEmail : ("nguyen.th" + gen + ".nam@donganh.clan.local");
            LocalDate dobM = LocalDate.parse(mr[2]);
            LocalDate dodM = (mr[3] != null && !mr[3].isBlank()) ? LocalDate.parse(mr[3]) : null;
            Set<Role> maleRoles = (i == 6)
                    ? new HashSet<>(Set.of(branchLeadRole, newsMgrRole))
                    : new HashSet<>(Set.of(memberRole));

            m[i] = upsertPerson(
                    mr[0],
                    mr[1],
                    emailM,
                    "MALE",
                    dobM,
                    dodM,
                    mr[5],
                    "Thôn Đông Anh, Đông Anh, Hà Nội",
                    "Hà Nội",
                    mr[4],
                    "Thế hệ " + gen + " — phả kế nam (dữ liệu minh họa).",
                    branch,
                    gen,
                    1,
                    i > 0 ? males[i - 1][0] : null,
                    fam,
                    maleRoles,
                    passwordEncoder.encode("123456"),
                    2
            );

            LocalDate dobF = LocalDate.parse(fr[2]);
            LocalDate dodF = (fr[3] != null && !fr[3].isBlank()) ? LocalDate.parse(fr[3]) : null;
            String occF = (fr[4] != null && !fr[4].isBlank()) ? fr[4] : "Nội trợ / việc gia đình";

            f[i] = upsertPerson(
                    fr[0],
                    fr[1],
                    "nguyen.th" + gen + ".nu@donganh.clan.local",
                    "FEMALE",
                    dobF,
                    dodF,
                    fr[5],
                    "Thôn Đông Anh, Đông Anh, Hà Nội",
                    "Hà Nội",
                    occF,
                    "Thế hệ " + gen + " (dữ liệu minh họa).",
                    branch,
                    gen,
                    2,
                    null,
                    fam,
                    new HashSet<>(Set.of(memberRole)),
                    passwordEncoder.encode("123456"),
                    2
            );
        }

        for (int i = 0; i < n; i++) {
            ensureSpouse(m[i], f[i]);
        }
        /*
         * Phả kế nam m[i] là con ruột của cặp m[i-1] + f[i-1].
         * Vợ f[i] (con dâu) KHÔNG là con của cặp thế hệ trước — chỉ có quan hệ SPOUSE với m[i].
         * Nếu gắn cha–mẹ cho cả f[i], FE coi vợ chồng là anh chị em → không ghép cặp trên cây (lỗi "chồng một mình có 2 con").
         */
        for (int i = 1; i < n; i++) {
            relationshipRepository.deleteParentChildBetween(m[i - 1].getUserId(), f[i].getUserId());
            relationshipRepository.deleteParentChildBetween(f[i - 1].getUserId(), f[i].getUserId());
            ensureParentChild(m[i - 1], m[i]);
            ensureParentChild(f[i - 1], m[i]);
        }
        for (int i = 1; i < n; i++) {
            User maleChild = userRepository.findById(m[i].getUserId()).orElseThrow();
            maleChild.setParentId(m[i - 1].getUserId());
            userRepository.save(maleChild);
            User femaleInLaw = userRepository.findById(f[i].getUserId()).orElseThrow();
            femaleInLaw.setParentId(null);
            userRepository.save(femaleInLaw);
        }

        seedParallelBranches(m, f, root, chiPhuKe, chiTieu, chiDoiTre, memberRole);
    }

    /**
     * Nhánh phụ: anh em cùng tổ / cùng cha mẹ (chỉ phả kế nam có PARENT_CHILD; con dâu chỉ SPOUSE).
     */
    private void seedParallelBranches(
            User[] m,
            User[] f,
            Family root,
            Family chiPhuKe,
            Family chiTieu,
            Family chiDoiTre,
            Role memberRole
    ) {
        /* Nhánh Đạt: em trai Lợi (cùng Khiêm + Nhàn) → Cường → Dũng */
        User da = upsertSideMale(
                "seed-br-g2-m",
                "Nguyễn Văn Đạt",
                "1902-05-18",
                "1980-09-10",
                "Thợ rèn làng nghề",
                "0903020001",
                2,
                3,
                familyForGenerationNumber(2, root, chiPhuKe, chiTieu, chiDoiTre),
                "Em trai Lợi — nhánh phụ.",
                memberRole
        );
        User loan = upsertSideFemale(
                "seed-br-g2-f",
                "Nguyễn Thị Loan",
                "1904-08-01",
                "1982-04-20",
                "Nội trợ, buôn chợ",
                "0903020002",
                2,
                4,
                familyForGenerationNumber(2, root, chiPhuKe, chiTieu, chiDoiTre),
                memberRole
        );
        ensureSpouse(da, loan);
        linkBloodSonToParents(m[0], f[0], da);
        persistMalePatriline(da, m[0].getUserId());
        persistWifeNoParent(loan);

        User cuong = upsertSideMale(
                "seed-br-g3-m",
                "Nguyễn Văn Cường",
                "1926-11-09",
                "2005-03-15",
                "Công nhân nhà máy",
                "0903020003",
                3,
                5,
                familyForGenerationNumber(3, root, chiPhuKe, chiTieu, chiDoiTre),
                "Con trai Đạt — nhánh phụ.",
                memberRole
        );
        User oanh = upsertSideFemale(
                "seed-br-g3-f",
                "Nguyễn Thị Oanh",
                "1928-02-14",
                "2008-01-10",
                "",
                "0903020004",
                3,
                6,
                familyForGenerationNumber(3, root, chiPhuKe, chiTieu, chiDoiTre),
                memberRole
        );
        ensureSpouse(cuong, oanh);
        linkBloodSonToParents(da, loan, cuong);
        persistMalePatriline(cuong, da.getUserId());
        persistWifeNoParent(oanh);

        User dzung = upsertSideMale(
                "seed-br-g4-m",
                "Nguyễn Văn Dũng",
                "1948-07-22",
                "2018-11-30",
                "Chủ HTX nông nghiệp",
                "0903020005",
                4,
                7,
                familyForGenerationNumber(4, root, chiPhuKe, chiTieu, chiDoiTre),
                "Cháu nội Đạt — lá nhánh tận cùng.",
                memberRole
        );
        User hanh = upsertSideFemale(
                "seed-br-g4-f",
                "Nguyễn Thị Hạnh",
                "1950-01-30",
                "2019-05-01",
                "Y tá hưu",
                "0903020006",
                4,
                8,
                familyForGenerationNumber(4, root, chiPhuKe, chiTieu, chiDoiTre),
                memberRole
        );
        ensureSpouse(dzung, hanh);
        linkBloodSonToParents(cuong, oanh, dzung);
        persistMalePatriline(dzung, cuong.getUserId());
        persistWifeNoParent(hanh);

        /* Nhánh Hùng: em trai Thịnh (cùng Lợi + Sen) */
        User hung = upsertSideMale(
                "seed-br-g3b-m",
                "Nguyễn Văn Hùng",
                "1916-01-12",
                "1990-06-01",
                "Buôn bán tại chợ huyện",
                "0903020010",
                3,
                7,
                familyForGenerationNumber(3, root, chiPhuKe, chiTieu, chiDoiTre),
                "Em trai Thịnh — nhánh phụ.",
                memberRole
        );
        User nguyet = upsertSideFemale(
                "seed-br-g3b-f",
                "Nguyễn Thị Nguyệt",
                "1918-04-25",
                "1992-08-18",
                "",
                "0903020011",
                3,
                8,
                familyForGenerationNumber(3, root, chiPhuKe, chiTieu, chiDoiTre),
                memberRole
        );
        ensureSpouse(hung, nguyet);
        linkBloodSonToParents(m[1], f[1], hung);
        persistMalePatriline(hung, m[1].getUserId());
        persistWifeNoParent(nguyet);

        /* Nhánh Thắng: em trai Tám (cùng Bình + Lan) → một cặp con gen VI */
        User thang = upsertSideMale(
                "seed-br-g5-m",
                "Nguyễn Văn Thắng",
                "1945-12-01",
                "",
                "Lương y, bốc thuốc Nam",
                "0903020020",
                5,
                3,
                familyForGenerationNumber(5, root, chiPhuKe, chiTieu, chiDoiTre),
                "Em trai Tám — nhánh phụ.",
                memberRole
        );
        User van = upsertSideFemale(
                "seed-br-g5-f",
                "Nguyễn Thị Vân",
                "1947-09-19",
                "",
                "Hộ sinh hưu",
                "0903020021",
                5,
                4,
                familyForGenerationNumber(5, root, chiPhuKe, chiTieu, chiDoiTre),
                memberRole
        );
        ensureSpouse(thang, van);
        linkBloodSonToParents(m[3], f[3], thang);
        persistMalePatriline(thang, m[3].getUserId());
        persistWifeNoParent(van);

        User vu = upsertSideMale(
                "seed-br-g6-m",
                "Nguyễn Văn Vũ",
                "1972-03-08",
                "",
                "Kinh doanh tự do",
                "0903020022",
                6,
                5,
                familyForGenerationNumber(6, root, chiPhuKe, chiTieu, chiDoiTre),
                "Con trai Thắng — nhánh phụ.",
                memberRole
        );
        User tram = upsertSideFemale(
                "seed-br-g6-f",
                "Nguyễn Thị Trâm",
                "1974-11-02",
                "",
                "Giáo viên THPT",
                "0903020023",
                6,
                6,
                familyForGenerationNumber(6, root, chiPhuKe, chiTieu, chiDoiTre),
                memberRole
        );
        ensureSpouse(vu, tram);
        linkBloodSonToParents(thang, van, vu);
        persistMalePatriline(vu, thang.getUserId());
        persistWifeNoParent(tram);

        /* Nhánh Kiên: em trai Minh (cùng Trường + Thu) */
        User kien = upsertSideMale(
                "seed-br-g8-m",
                "Nguyễn Văn Kiên",
                "1982-06-20",
                "",
                "Kiến trúc sư",
                "0903020030",
                8,
                3,
                familyForGenerationNumber(8, root, chiPhuKe, chiTieu, chiDoiTre),
                "Em trai Minh — nhánh phụ.",
                memberRole
        );
        User linh = upsertSideFemale(
                "seed-br-g8-f",
                "Nguyễn Thị Linh",
                "1984-10-15",
                "",
                "Dược sĩ",
                "0903020031",
                8,
                4,
                familyForGenerationNumber(8, root, chiPhuKe, chiTieu, chiDoiTre),
                memberRole
        );
        ensureSpouse(kien, linh);
        linkBloodSonToParents(m[6], f[6], kien);
        persistMalePatriline(kien, m[6].getUserId());
        persistWifeNoParent(linh);
    }

    private static Family familyForGenerationNumber(
            int generationOneBased,
            Family root,
            Family chiPhuKe,
            Family chiTieu,
            Family chiDoiTre
    ) {
        int idx = generationOneBased - 1;
        return familyForGenerationIndex(idx, root, chiPhuKe, chiTieu, chiDoiTre);
    }

    private User upsertSideMale(
            String userId,
            String fullName,
            String dob,
            String dodOrEmpty,
            String occupation,
            String phone,
            int generation,
            int orderInFamily,
            Family family,
            String bio,
            Role role
    ) {
        LocalDate dobD = LocalDate.parse(dob);
        LocalDate dodD = (dodOrEmpty != null && !dodOrEmpty.isBlank()) ? LocalDate.parse(dodOrEmpty) : null;
        String email = userId + ".nam@donganh.clan.local";
        return upsertPerson(
                userId,
                fullName,
                email,
                "MALE",
                dobD,
                dodD,
                phone,
                "Thôn Đông Anh, Đông Anh, Hà Nội",
                "Hà Nội",
                occupation,
                bio,
                branchLabelForGen(generation - 1) + " — nhánh phụ",
                generation,
                orderInFamily,
                null,
                family,
                new HashSet<>(Set.of(role)),
                passwordEncoder.encode("123456"),
                2
        );
    }

    private User upsertSideFemale(
            String userId,
            String fullName,
            String dob,
            String dodOrEmpty,
            String occupation,
            String phone,
            int generation,
            int orderInFamily,
            Family family,
            Role role
    ) {
        LocalDate dobD = LocalDate.parse(dob);
        LocalDate dodD = (dodOrEmpty != null && !dodOrEmpty.isBlank()) ? LocalDate.parse(dodOrEmpty) : null;
        String occ = (occupation != null && !occupation.isBlank()) ? occupation : "Nội trợ / việc gia đình";
        String email = userId + ".nu@donganh.clan.local";
        return upsertPerson(
                userId,
                fullName,
                email,
                "FEMALE",
                dobD,
                dodD,
                phone,
                "Thôn Đông Anh, Đông Anh, Hà Nội",
                "Hà Nội",
                occ,
                "Nhánh phụ — thế hệ " + generation + ".",
                branchLabelForGen(generation - 1) + " — nhánh phụ",
                generation,
                orderInFamily,
                null,
                family,
                new HashSet<>(Set.of(role)),
                passwordEncoder.encode("123456"),
                2
        );
    }

    private void linkBloodSonToParents(User father, User mother, User son) {
        ensureParentChild(father, son);
        ensureParentChild(mother, son);
    }

    private void persistMalePatriline(User male, String patrilinealParentId) {
        User u = userRepository.findById(male.getUserId()).orElseThrow();
        u.setParentId(patrilinealParentId);
        userRepository.save(u);
    }

    private void persistWifeNoParent(User wife) {
        User u = userRepository.findById(wife.getUserId()).orElseThrow();
        u.setParentId(null);
        userRepository.save(u);
    }

    private User upsertPerson(
            String userId,
            String fullName,
            String email,
            String gender,
            LocalDate dob,
            LocalDate dod,
            String phone,
            String hometown,
            String address,
            String occupation,
            String bio,
            String branch,
            int generation,
            int orderInFamily,
            String parentId,
            Family family,
            Set<Role> roles,
            String encodedPassword,
            int status
    ) {
        return userRepository.findById(userId).map(existing -> {
            if (existing.getFullName() == null || existing.getFullName().isBlank()) {
                existing.setFullName(fullName);
            }
            existing.setFamily(family);
            if (branch != null && !branch.isBlank()) {
                existing.setBranch(branch);
            }
            existing.setStatus(status);
            existing.setRoles(new HashSet<>(roles));
            return userRepository.save(existing);
        }).orElseGet(() -> {
            User u = User.builder()
                    .userId(userId)
                    .fullName(fullName)
                    .email(email)
                    .password(encodedPassword)
                    .gender(gender)
                    .dob(dob)
                    .dod(dod)
                    .phoneNumber(phone)
                    .hometown(hometown)
                    .currentAddress(address)
                    .occupation(occupation)
                    .bio(bio)
                    .branch(branch)
                    .generation(generation)
                    .orderInFamily(orderInFamily)
                    .parentId(parentId)
                    .family(family)
                    .roles(new HashSet<>(roles))
                    .status(status)
                    .build();
            return userRepository.save(u);
        });
    }

    private void ensureSpouse(User a, User b) {
        if (relationshipRepository.findSpouses(a.getUserId()).stream().anyMatch(r ->
                partnerId(r, a.getUserId()).equals(b.getUserId()))) {
            return;
        }
        Relationship r = new Relationship();
        r.setPerson1(a);
        r.setPerson2(b);
        r.setRelType("SPOUSE");
        relationshipRepository.save(r);
    }

    private void ensureParentChild(User parent, User child) {
        if (relationshipRepository.existsByRelTypeAndPerson1_UserIdAndPerson2_UserId(
                "PARENT_CHILD", parent.getUserId(), child.getUserId())) {
            return;
        }
        Relationship r = new Relationship();
        r.setPerson1(parent);
        r.setPerson2(child);
        r.setRelType("PARENT_CHILD");
        relationshipRepository.save(r);
    }

    private static String partnerId(Relationship r, String selfId) {
        if (r.getPerson1() != null && r.getPerson1().getUserId().equals(selfId)) {
            return r.getPerson2() != null ? r.getPerson2().getUserId() : "";
        }
        return r.getPerson1() != null ? r.getPerson1().getUserId() : "";
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

    /** Cố định {@code roleId} (ổn định FK {@code user_roles}) — cập nhật tên role + tập quyền mỗi lần seed. */
    private Role ensureRoleById(String roleId, String roleName, Set<Permission> permissions) {
        Role r = roleRepository.findById(roleId).orElseGet(() -> {
            Role x = new Role();
            x.setRoleId(roleId);
            x.setRoleName(roleName);
            return roleRepository.saveAndFlush(x);
        });
        r.setRoleName(roleName);
        r.setPermissions(new HashSet<>(permissions));
        return roleRepository.save(r);
    }
}
