package com.family.app.repository;

import com.family.app.model.NewsCategory;
import com.family.app.model.NewsEvent;
import com.family.app.model.NewsVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsEventRepository extends JpaRepository<NewsEvent, String> {

    boolean existsBySlug(String slug);

    @Query("SELECT DISTINCT n FROM NewsEvent n LEFT JOIN FETCH n.user WHERE n.slug = :slug AND n.visibility = :vis AND n.family IS NULL")
    Optional<NewsEvent> findBySlugAndVisibility(@Param("slug") String slug, @Param("vis") NewsVisibility vis);

    @Query("SELECT DISTINCT n FROM NewsEvent n LEFT JOIN FETCH n.user WHERE n.visibility = :vis AND n.family IS NULL ORDER BY n.createdAt DESC")
    List<NewsEvent> findByVisibilityOrderByCreatedAtDesc(@Param("vis") NewsVisibility vis);

    @Query("SELECT DISTINCT n FROM NewsEvent n LEFT JOIN FETCH n.user WHERE n.visibility = :vis AND n.featured = true AND n.family IS NULL ORDER BY n.createdAt DESC")
    List<NewsEvent> findByVisibilityAndFeaturedTrueOrderByCreatedAtDesc(@Param("vis") NewsVisibility vis);

    @Query("SELECT n FROM NewsEvent n WHERE n.visibility = :vis AND n.publicCategory = :cat AND n.id <> :id AND n.family IS NULL ORDER BY n.createdAt DESC")
    List<NewsEvent> findRelatedPublic(
            @Param("vis") NewsVisibility vis,
            @Param("cat") NewsCategory publicCategory,
            @Param("id") String id
    );

    @Query("SELECT DISTINCT n FROM NewsEvent n LEFT JOIN FETCH n.user WHERE " +
            "(:search IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(n.summary) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:category IS NULL OR n.publicCategory = :category) AND " +
            "(:vis IS NULL OR n.visibility = :vis) AND n.family IS NULL " +
            "ORDER BY n.createdAt DESC")
    List<NewsEvent> searchSiteNewsForAdmin(
            @Param("search") String search,
            @Param("category") NewsCategory category,
            @Param("vis") NewsVisibility vis
    );

    /** Tin trong dòng họ: không hiển thị bản nháp (visibility = DRAFT). */
    @Query("SELECT n FROM NewsEvent n WHERE n.family.familyId = :familyId AND (n.visibility IS NULL OR n.visibility <> com.family.app.model.NewsVisibility.DRAFT) ORDER BY n.createdAt DESC")
    List<NewsEvent> findByFamily_FamilyIdOrderByCreatedAtDesc(@Param("familyId") String familyId);

    @Query("SELECT n FROM NewsEvent n WHERE n.family.familyId = :familyId AND (n.visibility IS NULL OR n.visibility <> com.family.app.model.NewsVisibility.DRAFT) AND LOWER(n.title) LIKE LOWER(CONCAT('%', :title, '%')) ORDER BY n.createdAt DESC")
    List<NewsEvent> findByFamily_FamilyIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
            @Param("familyId") String familyId,
            @Param("title") String title
    );

    @Query("SELECT n FROM NewsEvent n WHERE n.family.familyId = :familyId AND (n.visibility IS NULL OR n.visibility <> com.family.app.model.NewsVisibility.DRAFT) AND n.category.categoryId = :categoryId ORDER BY n.createdAt DESC")
    List<NewsEvent> findByFamily_FamilyIdAndCategory_CategoryIdOrderByCreatedAtDesc(
            @Param("familyId") String familyId,
            @Param("categoryId") String categoryId
    );

    @Query("SELECT n FROM NewsEvent n WHERE n.family.familyId = :familyId AND (n.visibility IS NULL OR n.visibility <> com.family.app.model.NewsVisibility.DRAFT) AND n.category.categoryId = :categoryId AND LOWER(n.title) LIKE LOWER(CONCAT('%', :title, '%')) ORDER BY n.createdAt DESC")
    List<NewsEvent> findByFamily_FamilyIdAndCategory_CategoryIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
            @Param("familyId") String familyId,
            @Param("categoryId") String categoryId,
            @Param("title") String title
    );

    @Query("SELECT n FROM NewsEvent n WHERE n.family.familyId = :familyId AND (n.visibility IS NULL OR n.visibility <> com.family.app.model.NewsVisibility.DRAFT) ORDER BY n.createdAt DESC")
    List<NewsEvent> findTop5ByFamily_FamilyIdOrderByCreatedAtDesc(@Param("familyId") String familyId);

    @Query("SELECT n FROM NewsEvent n WHERE n.family.familyId = :familyId AND n.category.categoryId = :categoryId AND n.startAt > :now AND (n.visibility IS NULL OR n.visibility <> com.family.app.model.NewsVisibility.DRAFT) ORDER BY n.startAt ASC")
    List<NewsEvent> findTop4ByFamily_FamilyIdAndCategory_CategoryIdAndStartAtAfterOrderByStartAtAsc(
            @Param("familyId") String familyId,
            @Param("categoryId") String categoryId,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT n FROM NewsEvent n WHERE n.family.familyId = :familyId AND (n.visibility IS NULL OR n.visibility <> com.family.app.model.NewsVisibility.DRAFT) ORDER BY n.createdAt DESC")
    List<NewsEvent> findTop4ByFamily_FamilyIdOrderByCreatedAtDesc(@Param("familyId") String familyId);
}
