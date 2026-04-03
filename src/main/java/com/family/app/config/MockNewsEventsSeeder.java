package com.family.app.config;

import com.family.app.model.*;
import com.family.app.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seed dữ liệu mock cho {@code news_events} bằng JPA/Hibernate.
 * Mỗi lần khởi động: xóa các bản ghi mock (theo id cố định) rồi chèn lại.
 * Chạy sau {@link DataInitializer} (dòng họ, role, user đăng nhập chính).
 */
@Component
@Order(2)
public class MockNewsEventsSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MockNewsEventsSeeder.class);

    /** Cùng id với file SQL cũ — để xóa sạch trước khi seed lại. */
    public static final List<String> MOCK_NEWS_IDS = List.of(
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0001",
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0002",
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0003",
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0004",
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0005",
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0006",
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0007",
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0008",
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0009"
    );

    private final NewsEventRepository newsEventRepository;
    private final FamilyRepository familyRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public MockNewsEventsSeeder(
            NewsEventRepository newsEventRepository,
            FamilyRepository familyRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository
    ) {
        this.newsEventRepository = newsEventRepository;
        this.familyRepository = familyRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("[MockNewsEventsSeeder] Xóa {} bản ghi news_events mock (nếu có)...", MOCK_NEWS_IDS.size());
        newsEventRepository.deleteAllById(MOCK_NEWS_IDS);

        ensureCategories();
        ensureMockUsers();

        User u001 = userRepository.findById("user-001").orElseThrow();
        User adminId = userRepository.findById("admin-id").orElseThrow();
        User uMockNews = userRepository.findById("user-mock-news-001").orElseThrow();

        Family famRoot = familyRepository.findById("fam-root").orElseThrow();
        Family famNguyen = familyRepository.findById("fam-nguyen-001").orElseThrow();
        Family famMock = familyRepository.findById("fam-mock-001").orElseThrow();
        Family fam001 = familyRepository.findById("fam-001").orElseThrow();

        Category catMock = categoryRepository.findById("cat-mock-001").orElseThrow();
        Category cat001 = categoryRepository.findById("cat-001").orElseThrow();
        Category cat002 = categoryRepository.findById("cat-002").orElseThrow();

        List<NewsEvent> batch = List.of(
                site("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0001", u001, famRoot,
                        "Lễ giỗ tổ năm 2026", "le-gio-to-nam-2026",
                        "Thông tin lễ giỗ và phân công ban tổ chức.",
                        "<p>Nội dung mẫu: lịch trình, dress code, liên hệ ban tổ chức.</p>",
                        NewsCategory.HISTORY,
                        LocalDateTime.of(2026, 4, 10, 7, 0),
                        LocalDateTime.of(2026, 4, 10, 12, 0),
                        "Nhà thờ họ Nguyễn", 1440,
                        true, 328,
                        LocalDateTime.of(2026, 3, 15, 8, 0)),
                site("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0002", adminId, famRoot,
                        "Thông báo họp họ định kỳ tháng 4", "thong-bao-hop-ho-dinh-ky-thang-4",
                        "Họp trực tiếp kết hợp trực tuyến.",
                        "<p>Thời gian: 9h sáng Chủ nhật. Nghị quyết: công quỹ, sửa quy chế.</p>",
                        NewsCategory.ANNOUNCEMENT,
                        LocalDateTime.of(2026, 4, 20, 9, 0),
                        null,
                        "Hội trường Ủy ban xã", null,
                        true, 156,
                        LocalDateTime.of(2026, 3, 20, 10, 30)),
                site("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0003", u001, famRoot,
                        "Hoạt động thiện nguyện mùa xuân", "hoat-dong-thien-nguyen-mua-xuan",
                        "Quyên góp sách vở cho trẻ vùng cao.",
                        "<p>Danh sách quyên góp sẽ công bố sau họp.</p>",
                        NewsCategory.EVENT,
                        LocalDateTime.of(2026, 5, 1, 8, 0),
                        LocalDateTime.of(2026, 5, 1, 17, 0),
                        "Trường THCS xã", 2880,
                        false, 89,
                        LocalDateTime.of(2026, 3, 22, 14, 0)),
                site("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0004", adminId, famRoot,
                        "Tin chung: cập nhật website gia phả", "cap-nhat-website-gia-pha",
                        "Phiên bản mới hỗ trợ tin tức và phạm vi hiển thị.",
                        "<p>Đội ngũ phát triển xin thông báo các thay đổi giao diện.</p>",
                        NewsCategory.GENERAL,
                        null, null, null, null,
                        false, 412,
                        LocalDateTime.of(2026, 3, 28, 9, 0)),

                family("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0005", adminId, famNguyen, cat001,
                        "Họp chi họ Nguyễn — tháng 5/2026",
                        "Chỉ gửi nội bộ chi họ.",
                        "<p>Chốt ngày họp, kinh phí, phân công tiếp khách.</p>",
                        LocalDateTime.of(2026, 5, 15, 19, 0), null, "Trụ sở chi họ", 120,
                        12, LocalDateTime.of(2026, 3, 30, 11, 0)),
                family("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0006", uMockNews, famMock, catMock,
                        "Kế hoạch sửa nhà thờ (nội bộ)",
                        "Dự toán sơ bộ, chờ họp thông qua.",
                        "<p>Phụ lục vật tư đính kèm sau khi duyệt.</p>",
                        null, null, null, null,
                        3, LocalDateTime.of(2026, 3, 31, 7, 15)),

                draft("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0007", adminId, famRoot,
                        "[Nháp] Bài giới thiệu dòng họ (chưa đăng)", "ban-nhap-gioi-thieu-dong-ho",
                        "Đang soạn — chưa public.",
                        "<p>…</p>",
                        NewsCategory.HISTORY,
                        LocalDateTime.of(2026, 4, 1, 16, 0)),

                family("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0008", u001, fam001, cat002,
                        "Lễ hội đền — thử nghiệm remind",
                        "Nhắc trước 1 ngày.",
                        "<p>Lịch trình chi tiết sẽ gửi Zalo nhóm.</p>",
                        LocalDateTime.of(2026, 6, 1, 6, 0),
                        LocalDateTime.of(2026, 6, 1, 22, 0),
                        "Đền làng", 1440,
                        7, LocalDateTime.of(2026, 3, 29, 12, 0)),

                site("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0009", u001, famRoot,
                        "Thông báo đóng góp quỹ họ đầu năm", "thong-bao-dong-gop-quy-ho-dau-nam",
                        "Hạn nộp và thông tin tài khoản công khai.",
                        "<p>Mời các chi họ chuyển khoản trước ngày 30/4.</p>",
                        NewsCategory.ANNOUNCEMENT,
                        null, null, null, null,
                        false, 42,
                        LocalDateTime.of(2026, 3, 18, 9, 0))
        );

        for (NewsEvent n : batch) {
            newsEventRepository.save(n);
        }

        log.info("[MockNewsEventsSeeder] Đã INSERT lại {} bản ghi news_events mock (JPA).", batch.size());
        logAccountBanner();
    }

    private void logAccountBanner() {
        log.info("========== MOCK ĐĂNG NHẬP (seed, mật khẩu mặc định) ==========");
        log.info("  Trưởng họ fam-001:        truongho@giapha.vn / 123456");
        log.info("  Trưởng họ fam-nguyen-001: admin@giapha.vn / 123456");
        log.info("  Trưởng họ fam-root:       truongroot@giapha.vn / 123456");
        log.info("  Thành viên:               member@giapha.vn / 123456");
        log.info("  Tin news_events:          9 bản ghi — xóa theo id cố định rồi seed lại (MockNewsEventsSeeder)");
        log.info("================================================================");
    }

    private void ensureCategories() {
        saveCat("cat-mock-001", "Tin nội bộ (mock)");
        saveCat("cat-001", "Thông báo");
        saveCat("cat-002", "Lễ hội");
    }

    private void saveCat(String id, String name) {
        if (categoryRepository.findById(id).isEmpty()) {
            Category c = new Category();
            c.setCategoryId(id);
            c.setName(name);
            categoryRepository.save(c);
        }
    }

    private void ensureMockUsers() {
        if (userRepository.findById("user-001").isEmpty()) {
            userRepository.save(User.builder()
                    .userId("user-001")
                    .fullName("Phùng Văn A (mock seed)")
                    .email("phung.mock.seed@giapha.local")
                    .build());
        }
        if (userRepository.findById("admin-id").isEmpty()) {
            userRepository.save(User.builder()
                    .userId("admin-id")
                    .fullName("Admin mock seed")
                    .email("admin.mock.seed@giapha.local")
                    .gender("MALE")
                    .status(1)
                    .generation(1)
                    .orderInFamily(1)
                    .branch("Hội đồng")
                    .build());
        }
        if (userRepository.findById("user-mock-news-001").isEmpty()) {
            Family fam = familyRepository.findById("fam-mock-001")
                    .orElseThrow(() -> new IllegalStateException("fam-mock-001 chưa có — DataInitializer phải chạy trước"));
            userRepository.save(User.builder()
                    .userId("user-mock-news-001")
                    .fullName("Nguyễn Văn Biên tập (mock)")
                    .email("bientap.mock@giapha.local")
                    .gender("MALE")
                    .status(1)
                    .generation(1)
                    .orderInFamily(1)
                    .branch("Ban thông tin")
                    .family(fam)
                    .build());
        }
    }

    private static NewsEvent site(
            String id, User author, Family fam, String title, String slug, String summary, String html,
            NewsCategory pubCat,
            LocalDateTime start, LocalDateTime end, String location, Integer remind,
            boolean featured, int views, LocalDateTime created
    ) {
        NewsEvent n = NewsEvent.builder()
                .id(id)
                .family(fam)
                .category(null)
                .user(author)
                .title(title)
                .slug(slug)
                .summary(summary)
                .content(html)
                .publicCategory(pubCat)
                .startAt(start)
                .endAt(end)
                .location(location)
                .remindBefore(remind)
                .visibility(NewsVisibility.PUBLIC_SITE)
                .featured(featured)
                .viewCount(views)
                .build();
        n.setCreatedAt(created);
        n.setUpdatedAt(created);
        return n;
    }

    private static NewsEvent family(
            String id, User author, Family fam, Category cat, String title,
            String summary, String html,
            LocalDateTime start, LocalDateTime end, String location, Integer remind,
            int views, LocalDateTime created
    ) {
        NewsEvent n = NewsEvent.builder()
                .id(id)
                .family(fam)
                .category(cat)
                .user(author)
                .title(title)
                .slug(null)
                .summary(summary)
                .content(html)
                .publicCategory(null)
                .startAt(start)
                .endAt(end)
                .location(location)
                .remindBefore(remind)
                .visibility(NewsVisibility.FAMILY_ONLY)
                .featured(false)
                .viewCount(views)
                .build();
        n.setCreatedAt(created);
        n.setUpdatedAt(created);
        return n;
    }

    private static NewsEvent draft(
            String id, User author, Family fam, String title, String slug, String summary, String html,
            NewsCategory pubCat,
            LocalDateTime created
    ) {
        NewsEvent n = NewsEvent.builder()
                .id(id)
                .family(fam)
                .category(null)
                .user(author)
                .title(title)
                .slug(slug)
                .summary(summary)
                .content(html)
                .publicCategory(pubCat)
                .visibility(NewsVisibility.DRAFT)
                .featured(false)
                .viewCount(0)
                .build();
        n.setCreatedAt(created);
        n.setUpdatedAt(created);
        return n;
    }
}
