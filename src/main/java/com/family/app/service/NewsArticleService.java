package com.family.app.service;

import com.family.app.model.NewsArticle;
import com.family.app.model.NewsCategory;
import com.family.app.model.User;
import com.family.app.repository.NewsArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class NewsArticleService {

    private final NewsArticleRepository newsArticleRepository;

    public NewsArticleService(NewsArticleRepository newsArticleRepository) {
        this.newsArticleRepository = newsArticleRepository;
    }

    public Optional<NewsArticle> findPublishedBySlug(String slug) {
        return newsArticleRepository.findBySlugAndPublishedTrue(slug);
    }

    @Transactional(readOnly = true)
    public List<NewsArticle> findPublishedForPublic(String q, NewsCategory category) {
        List<NewsArticle> base = newsArticleRepository.findByPublishedTrueOrderByCreatedAtDesc();
        String query = q != null ? q.trim().toLowerCase(Locale.ROOT) : "";
        return base.stream()
                .filter(a -> category == null || a.getCategory() == category)
                .filter(a -> query.isEmpty()
                        || a.getTitle().toLowerCase(Locale.ROOT).contains(query)
                        || (a.getSummary() != null && a.getSummary().toLowerCase(Locale.ROOT).contains(query))
                        || (a.getContent() != null && a.getContent().toLowerCase(Locale.ROOT).contains(query)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NewsArticle> findFeaturedPublished(int limit) {
        return newsArticleRepository.findByPublishedTrueAndFeaturedTrueOrderByCreatedAtDesc().stream()
                .limit(limit)
                .toList();
    }

    @Transactional
    public void incrementViewCount(Long id) {
        newsArticleRepository.findById(id).ifPresent(a -> a.setViewCount(a.getViewCount() + 1));
    }

    @Transactional(readOnly = true)
    public List<NewsArticle> findRelated(NewsArticle article, int limit) {
        return newsArticleRepository
                .findByPublishedTrueAndCategoryAndIdNotOrderByCreatedAtDesc(article.getCategory(), article.getId())
                .stream()
                .limit(limit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NewsArticle> findAdminList(String search, String categoryName, String status) {
        String s = search != null && !search.isBlank() ? search.trim() : null;
        NewsCategory cat = parseCategory(categoryName);
        Boolean published = parsePublished(status);
        return newsArticleRepository.searchForAdmin(s, cat, published);
    }

    @Transactional
    public NewsArticle createArticle(
            String title,
            NewsCategory category,
            String summary,
            String content,
            boolean published,
            boolean featured,
            User author
    ) {
        NewsArticle article = NewsArticle.builder()
                .title(title != null ? title.trim() : "")
                .slug(generateUniqueSlug(title))
                .summary(summary != null ? summary.trim() : "")
                .content(content != null ? content : "")
                .coverImage(null)
                .category(category)
                .published(published)
                .featured(featured)
                .viewCount(0)
                .author(author)
                .build();
        return newsArticleRepository.save(article);
    }

    @Transactional
    public void updateArticle(Long id, NewsArticle incoming) {
        NewsArticle existing = newsArticleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết"));
        existing.setTitle(incoming.getTitle());
        existing.setCategory(incoming.getCategory());
        existing.setSummary(incoming.getSummary());
        existing.setContent(incoming.getContent());
        existing.setCoverImage(incoming.getCoverImage());
        existing.setPublished(incoming.isPublished());
        existing.setFeatured(incoming.isFeatured());
    }

    @Transactional
    public void togglePublish(Long id) {
        newsArticleRepository.findById(id).ifPresent(a -> a.setPublished(!a.isPublished()));
    }

    @Transactional
    public void delete(Long id) {
        newsArticleRepository.deleteById(id);
    }

    public NewsArticle emptyDraft() {
        return NewsArticle.builder()
                .title("")
                .slug("")
                .summary("")
                .content("")
                .category(NewsCategory.GENERAL)
                .published(false)
                .featured(false)
                .viewCount(0)
                .build();
    }

    public Optional<NewsArticle> findByIdForAdmin(Long id) {
        return newsArticleRepository.findById(id);
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

    private static Boolean parsePublished(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if ("published".equalsIgnoreCase(status)) {
            return true;
        }
        if ("draft".equalsIgnoreCase(status)) {
            return false;
        }
        return null;
    }

    private String generateUniqueSlug(String title) {
        String base = slugify(title);
        String candidate = base;
        int i = 2;
        while (newsArticleRepository.existsBySlug(candidate)) {
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
