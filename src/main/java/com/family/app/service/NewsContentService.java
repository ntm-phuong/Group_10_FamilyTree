package com.family.app.service;

import com.family.app.model.NewsArticle;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class NewsContentService {

    private final List<NewsArticle> articles = List.of(
        NewsArticle.builder()
            .id(1L)
            .slug("le-gio-to-ho-2026")
            .title("Thong bao le gio to dong ho nam 2026")
            .summary("Thong bao lich to chuc le gio to va cac hoat dong ket noi con chau trong dong ho.")
            .content("<p>Le gio to dong ho nam 2026 se duoc to chuc vao ngay 14/04 am lich tai nha tho to.</p><p>Kinh moi ba con sap xep thoi gian tham du day du de buoi le duoc trang trong va am cung.</p>")
            .category(NewsArticle.Category.ANNOUNCEMENT)
            .authorName("Ban quan tri")
            .featured(true)
            .viewCount(152)
            .publishedDate(LocalDate.now().minusDays(7))
            .build(),
        NewsArticle.builder()
            .id(2L)
            .slug("tong-ket-hoat-dong-thang-2")
            .title("Tong ket hoat dong dong ho thang 2")
            .summary("Tong hop cac hoat dong noi bat cua dong ho trong thang 2, bao gom khuyen hoc va tri an.")
            .content("<p>Trong thang 2, dong ho da to chuc chuong trinh trao hoc bong cho 12 hoc sinh va tham hoi cac cu cao nien.</p>")
            .category(NewsArticle.Category.EVENT)
            .authorName("Ban truyen thong")
            .featured(true)
            .viewCount(98)
            .publishedDate(LocalDate.now().minusDays(14))
            .build(),
        NewsArticle.builder()
            .id(3L)
            .slug("tu-lieu-lich-su-ho")
            .title("Bo sung tu lieu lich su dong ho")
            .summary("Cap nhat them hinh anh, gia pha va cac tai lieu co gia tri lich su cua dong ho.")
            .content("<p>Ban bien tap dang thu thap them tu lieu gia pha de chinh ly va so hoa theo tung doi.</p>")
            .category(NewsArticle.Category.HISTORY)
            .authorName("To so hoa")
            .featured(false)
            .viewCount(61)
            .publishedDate(LocalDate.now().minusDays(20))
            .build()
    );

    public List<NewsArticle> findArticles(NewsArticle.Category category) {
        return articles.stream()
            .filter(article -> category == null || article.getCategory() == category)
            .sorted(Comparator.comparing(NewsArticle::getPublishedDate).reversed())
            .toList();
    }

    public List<NewsArticle> findFeaturedArticles() {
        return articles.stream().filter(NewsArticle::isFeatured).toList();
    }

    public Optional<NewsArticle> findBySlug(String slug) {
        if (slug == null) {
            return Optional.empty();
        }
        String normalized = slug.toLowerCase(Locale.ROOT);
        return articles.stream()
            .filter(article -> Objects.equals(article.getSlug().toLowerCase(Locale.ROOT), normalized))
            .findFirst();
    }

    public List<NewsArticle> findRelatedArticles(NewsArticle source, int limit) {
        return articles.stream()
            .filter(article -> !Objects.equals(article.getId(), source.getId()))
            .filter(article -> article.getCategory() == source.getCategory())
            .limit(limit)
            .toList();
    }
}
