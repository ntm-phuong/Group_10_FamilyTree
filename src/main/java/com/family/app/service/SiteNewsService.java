package com.family.app.service;

import com.family.app.model.NewsCategory;
import com.family.app.model.NewsEvent;
import com.family.app.model.NewsVisibility;
import com.family.app.model.User;
import com.family.app.repository.NewsEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class SiteNewsService {

    private final NewsEventRepository newsEventRepository;

    public SiteNewsService(NewsEventRepository newsEventRepository) {
        this.newsEventRepository = newsEventRepository;
    }

    public Optional<NewsEvent> findPublicBySlug(String slug) {
        return newsEventRepository.findBySlugAndVisibility(slug, NewsVisibility.PUBLIC_SITE);
    }

    @Transactional(readOnly = true)
    public List<NewsEvent> findPublishedForPublic(String q, NewsCategory category) {
        List<NewsEvent> base = newsEventRepository.findByVisibilityOrderByCreatedAtDesc(NewsVisibility.PUBLIC_SITE);
        String query = q != null ? q.trim().toLowerCase(Locale.ROOT) : "";
        return base.stream()
                .filter(a -> category == null || a.getPublicCategory() == category)
                .filter(a -> query.isEmpty()
                        || a.getTitle().toLowerCase(Locale.ROOT).contains(query)
                        || (a.getSummary() != null && a.getSummary().toLowerCase(Locale.ROOT).contains(query))
                        || (a.getContent() != null && a.getContent().toLowerCase(Locale.ROOT).contains(query)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NewsEvent> findFeaturedPublished(int limit) {
        return newsEventRepository.findByVisibilityAndFeaturedTrueOrderByCreatedAtDesc(NewsVisibility.PUBLIC_SITE).stream()
                .limit(limit)
                .toList();
    }

    @Transactional
    public void incrementViewCount(String id) {
        newsEventRepository.findById(id).ifPresent(a -> {
            a.setViewCount(a.getViewCount() + 1);
            newsEventRepository.save(a);
        });
    }

    /**
     * Bài liên quan: ưu tiên cùng {@code publicCategory}; nếu không đủ thì lấy thêm tin PUBLIC_SITE khác
     * (tránh trống khi chỉ có một bài trong danh mục).
     */
    @Transactional(readOnly = true)
    public List<NewsEvent> findRelated(NewsEvent article, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<NewsEvent> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        seen.add(article.getId());

        if (article.getPublicCategory() != null) {
            for (NewsEvent n : newsEventRepository.findRelatedPublic(
                    NewsVisibility.PUBLIC_SITE, article.getPublicCategory(), article.getId())) {
                if (out.size() >= limit) {
                    break;
                }
                if (seen.add(n.getId())) {
                    out.add(n);
                }
            }
        }
        if (out.size() < limit) {
            for (NewsEvent n : newsEventRepository.findByVisibilityOrderByCreatedAtDesc(NewsVisibility.PUBLIC_SITE)) {
                if (out.size() >= limit) {
                    break;
                }
                if (seen.add(n.getId())) {
                    out.add(n);
                }
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<NewsEvent> findAdminList(String search, String categoryName, String status) {
        String s = search != null && !search.isBlank() ? search.trim() : null;
        NewsCategory cat = parseCategory(categoryName);
        NewsVisibility vis = parseVisibilityStatus(status);
        return newsEventRepository.searchSiteNewsForAdmin(s, cat, vis);
    }

    @Transactional
    public NewsEvent createSiteArticle(
            String title,
            NewsCategory category,
            String summary,
            String content,
            boolean published,
            boolean featured,
            User author
    ) {
        NewsVisibility vis = published ? NewsVisibility.PUBLIC_SITE : NewsVisibility.DRAFT;
        NewsEvent article = NewsEvent.builder()
                .title(title != null ? title.trim() : "")
                .slug(generateUniqueSlug(title))
                .summary(summary != null ? summary.trim() : "")
                .content(content != null ? content : "")
                .coverImage(null)
                .publicCategory(category)
                .visibility(vis)
                .featured(featured)
                .viewCount(0)
                .user(author)
                .family(null)
                .build();
        return newsEventRepository.save(article);
    }

    @Transactional
    public void updateSiteArticle(String id, NewsEvent incoming) {
        NewsEvent existing = newsEventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết"));
        existing.setTitle(incoming.getTitle());
        existing.setPublicCategory(incoming.getPublicCategory());
        existing.setSummary(incoming.getSummary());
        existing.setContent(incoming.getContent());
        existing.setCoverImage(incoming.getCoverImage());
        existing.setFeatured(incoming.isFeatured());
        if (incoming.getVisibility() != null) {
            existing.setVisibility(incoming.getVisibility());
        }
        if (incoming.getSlug() != null && !incoming.getSlug().isBlank()) {
            String s = incoming.getSlug().trim();
            if (!s.equals(existing.getSlug()) && newsEventRepository.existsBySlug(s)) {
                throw new IllegalArgumentException("Slug đã tồn tại");
            }
            existing.setSlug(s);
        }
    }

    @Transactional
    public void togglePublish(String id) {
        newsEventRepository.findById(id).ifPresent(a -> {
            if (a.getVisibility() == NewsVisibility.PUBLIC_SITE) {
                a.setVisibility(NewsVisibility.DRAFT);
            } else {
                a.setVisibility(NewsVisibility.PUBLIC_SITE);
                if (a.getSlug() == null || a.getSlug().isBlank()) {
                    a.setSlug(generateUniqueSlug(a.getTitle()));
                }
            }
        });
    }

    @Transactional
    public void delete(String id) {
        newsEventRepository.deleteById(id);
    }

    public NewsEvent emptyDraft() {
        return NewsEvent.builder()
                .title("")
                .slug("")
                .summary("")
                .content("")
                .publicCategory(NewsCategory.GENERAL)
                .visibility(NewsVisibility.DRAFT)
                .featured(false)
                .viewCount(0)
                .build();
    }

    public Optional<NewsEvent> findByIdForAdmin(String id) {
        return newsEventRepository.findById(id);
    }

    public static NewsCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return NewsCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static NewsVisibility parseVisibilityStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if ("published".equalsIgnoreCase(status)) {
            return NewsVisibility.PUBLIC_SITE;
        }
        if ("draft".equalsIgnoreCase(status)) {
            return NewsVisibility.DRAFT;
        }
        return null;
    }

    private String generateUniqueSlug(String title) {
        String base = slugify(title);
        String candidate = base;
        int i = 2;
        while (newsEventRepository.existsBySlug(candidate)) {
            candidate = base + "-" + i;
            i++;
        }
        return candidate;
    }

    private static String slugify(String title) {
        if (title == null || title.isBlank()) {
            return "bai-viet";
        }
        String n = Normalizer.normalize(title.trim(), Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}+", "");
        n = n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return n.isEmpty() ? "bai-viet" : n;
    }
}
