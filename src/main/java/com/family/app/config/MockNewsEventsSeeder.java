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
 * Tin tức mẫu gắn một dòng họ ({@link AppClanProperties#getFamilyId()}).
 */
@Component
@Order(2)
public class MockNewsEventsSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MockNewsEventsSeeder.class);

    public static final List<String> MOCK_NEWS_IDS = List.of(
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0001",
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0002",
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0003",
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0004",
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0005",
            "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0006"
    );

    private final NewsEventRepository newsEventRepository;
    private final FamilyRepository familyRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AppClanProperties clanProperties;

    public MockNewsEventsSeeder(
            NewsEventRepository newsEventRepository,
            FamilyRepository familyRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            AppClanProperties clanProperties
    ) {
        this.newsEventRepository = newsEventRepository;
        this.familyRepository = familyRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.clanProperties = clanProperties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("[MockNewsEventsSeeder] Xóa {} bản ghi mock (nếu có)...", MOCK_NEWS_IDS.size());
        newsEventRepository.deleteAllById(MOCK_NEWS_IDS);

        ensureCategories();
        String fid = clanProperties.getFamilyId();
        Family fam = familyRepository.findById(fid)
                .orElseThrow(() -> new IllegalStateException("Thiếu dòng họ " + fid + " — DataInitializer phải chạy trước."));
        User author = userRepository.findByEmail("truongho@giapha.vn")
                .orElseGet(() -> userRepository.findById("seed-clan-m-07").orElseThrow());

        Category catTb = categoryRepository.findById("cat-001").orElseThrow();
        Category catLe = categoryRepository.findById("cat-002").orElseThrow();

        LocalDateTime t0 = LocalDateTime.of(2026, 3, 20, 10, 0);
        List<NewsEvent> batch = List.of(
                site("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0001", author, fam,
                        "Lễ giỗ cụ tổ xuân 2026", "le-gio-to-xuan-2026",
                        clanProperties.getDisplayName()
                                + " \u2014 l\u1ecbch l\u1ec5 v\u00e0 ph\u00e2n c\u00f4ng n\u1ea5u c\u1ed5, ti\u1ebfp kh\u00e1ch.",
                        "<p>6h sáng làm lễ tại nhà thờ họ; trưa cỗ chung. Liên hệ ban tổ chức: <strong>0903000007</strong>.</p>",
                        NewsCategory.HISTORY,
                        LocalDateTime.of(2026, 4, 5, 6, 0),
                        LocalDateTime.of(2026, 4, 5, 14, 0),
                        "Nhà thờ họ, thôn Đông Anh", 1440,
                        true, 210, t0),
                site("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0002", author, fam,
                        "Thông báo họp họ định kỳ tháng 4", "hop-ho-dinh-ky-thang-4",
                        "Chốt quỹ khuyến học và kế hoạch sửa nhà thờ.",
                        "<p>9h sáng Chủ nhật 20/04/2026, hội trường UBND xã. Thành phần: các gia đình thế hệ từ 5 trở lên.</p>",
                        NewsCategory.ANNOUNCEMENT,
                        LocalDateTime.of(2026, 4, 20, 9, 0),
                        null,
                        "UBND xã Đông Anh", null,
                        true, 98, t0.plusDays(1)),
                site("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0003", author, fam,
                        "Giải bóng chuyền mừng xuân", "giai-bong-chuyen-mung-xuan",
                        "Đăng ký theo tổ — thi đấu ngày 30/4.",
                        "<p>Sân trường THCS; mỗi đội 6 người. Hạn đăng ký: 25/4.</p>",
                        NewsCategory.EVENT,
                        LocalDateTime.of(2026, 4, 30, 7, 30),
                        LocalDateTime.of(2026, 4, 30, 17, 0),
                        "THCS thôn Đông Anh", 720,
                        false, 76, t0.plusDays(2)),
                site("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0004", author, fam,
                        "Cập nhật website gia phả", "cap-nhat-website-gia-pha",
                        "Phiên bản mới: tin tức và quản lý nội dung trên trang /news.",
                        "<p>Mời đăng nhập bằng tài khoản được cấp để xem tin nội bộ và chỉnh sửa theo vai trò.</p>",
                        NewsCategory.GENERAL,
                        null, null, null, null,
                        false, 340, t0.plusDays(3)),
                family("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0005", author, fam, catTb,
                        "Họp nội bộ — quỹ khuyến học 2026",
                        "Chỉ thành viên đã đăng nhập thuộc dòng họ.",
                        "<p>Dự kiến chi 120 triệu đồng; danh sách học sinh đạt giải kèm theo biên bản.</p>",
                        LocalDateTime.of(2026, 5, 10, 19, 30), null, "Nhà văn hóa thôn", 120,
                        15, t0.plusDays(4)),
                family("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaa0006", author, fam, catLe,
                        "Lễ hội đền làng — nhắc lịch",
                        "Nhắc trước 1 ngày qua Zalo nhóm họ.",
                        "<p>Đền làng Đông Anh; mời các gia đình sắp xếp thời gian tham gia nghi lễ.</p>",
                        LocalDateTime.of(2026, 6, 1, 6, 0),
                        LocalDateTime.of(2026, 6, 1, 22, 0),
                        "Đền làng", 1440,
                        42, t0.plusDays(5))
        );

        for (NewsEvent n : batch) {
            newsEventRepository.save(n);
        }

        log.info("[MockNewsEventsSeeder] Đã seed {} tin (family {}).", batch.size(), fid);
        log.info("  Đăng nhập demo: truongho@giapha.vn / 123456 (trưởng họ) | member@giapha.vn / 123456 (thành viên)");
    }

    private void ensureCategories() {
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
}
