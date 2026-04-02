package com.family.app.repository;

import com.family.app.model.NewsArticle;
import com.family.app.model.NewsCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    @Query("SELECT n FROM NewsArticle n LEFT JOIN FETCH n.author WHERE n.slug = :slug AND n.published = true")
    Optional<NewsArticle> findBySlugAndPublishedTrue(@Param("slug") String slug);

    Optional<NewsArticle> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT DISTINCT n FROM NewsArticle n LEFT JOIN FETCH n.author WHERE n.published = true ORDER BY n.createdAt DESC")
    List<NewsArticle> findByPublishedTrueOrderByCreatedAtDesc();

    @Query("SELECT DISTINCT n FROM NewsArticle n LEFT JOIN FETCH n.author WHERE n.published = true AND n.featured = true ORDER BY n.createdAt DESC")
    List<NewsArticle> findByPublishedTrueAndFeaturedTrueOrderByCreatedAtDesc();

    @Query("SELECT n FROM NewsArticle n WHERE " +
            "(:search IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(n.summary) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:category IS NULL OR n.category = :category) AND " +
            "(:published IS NULL OR n.published = :published) " +
            "ORDER BY n.createdAt DESC")
    List<NewsArticle> searchForAdmin(
            @Param("search") String search,
            @Param("category") NewsCategory category,
            @Param("published") Boolean published
    );

    List<NewsArticle> findByPublishedTrueAndCategoryAndIdNotOrderByCreatedAtDesc(
            NewsCategory category,
            Long id
    );
}
