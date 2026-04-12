package com.family.app.repository;

import com.family.app.model.NewsCategory;
import com.family.app.model.NewsEvent;
import com.family.app.model.NewsVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsEventRepository extends JpaRepository<NewsEvent, String> {

    boolean existsBySlug(String slug);

    @Query("SELECT DISTINCT n FROM NewsEvent n LEFT JOIN FETCH n.user LEFT JOIN FETCH n.family WHERE n.slug = :slug AND n.visibility = :vis")
    Optional<NewsEvent> findBySlugAndVisibility(@Param("slug") String slug, @Param("vis") NewsVisibility vis);

    /** Slug dùng cho /news/{slug}: PUBLIC_SITE hoặc FAMILY_ONLY (đã có slug). */
    @Query("SELECT DISTINCT n FROM NewsEvent n LEFT JOIN FETCH n.user LEFT JOIN FETCH n.family WHERE n.slug = :slug AND "
            + "(n.visibility = com.family.app.model.NewsVisibility.PUBLIC_SITE OR n.visibility = com.family.app.model.NewsVisibility.FAMILY_ONLY)")
    Optional<NewsEvent> findBySlugPublicOrFamily(@Param("slug") String slug);

    @Query("SELECT DISTINCT n FROM NewsEvent n LEFT JOIN FETCH n.user LEFT JOIN FETCH n.family WHERE n.visibility = :vis AND n.family.familyId IN :familyIds ORDER BY n.createdAt DESC")
    List<NewsEvent> findByVisibilityAndFamilyFamilyIdIn(
            @Param("vis") NewsVisibility vis,
            @Param("familyIds") Collection<String> familyIds);

    @Query("SELECT DISTINCT n FROM NewsEvent n LEFT JOIN FETCH n.user LEFT JOIN FETCH n.family WHERE n.visibility = :vis ORDER BY n.createdAt DESC")
    List<NewsEvent> findByVisibilityOrderByCreatedAtDesc(@Param("vis") NewsVisibility vis);

    @Query("SELECT DISTINCT n FROM NewsEvent n LEFT JOIN FETCH n.user LEFT JOIN FETCH n.family WHERE n.visibility = :vis AND n.featured = true ORDER BY n.createdAt DESC")
    List<NewsEvent> findByVisibilityAndFeaturedTrueOrderByCreatedAtDesc(@Param("vis") NewsVisibility vis);

    @Query("SELECT DISTINCT n FROM NewsEvent n LEFT JOIN FETCH n.user LEFT JOIN FETCH n.family WHERE n.visibility = :vis AND n.family.familyId = :familyId ORDER BY n.createdAt DESC")
    List<NewsEvent> findByVisibilityAndFamilyFamilyIdOrderByCreatedAtDesc(
            @Param("vis") NewsVisibility vis,
            @Param("familyId") String familyId);

    @Query("SELECT DISTINCT n FROM NewsEvent n LEFT JOIN FETCH n.user LEFT JOIN FETCH n.family WHERE n.visibility = :vis AND n.featured = true AND n.family.familyId = :familyId ORDER BY n.createdAt DESC")
    List<NewsEvent> findByVisibilityAndFeaturedTrueAndFamilyFamilyIdOrderByCreatedAtDesc(
            @Param("vis") NewsVisibility vis,
            @Param("familyId") String familyId);

    @Query("SELECT n FROM NewsEvent n WHERE n.visibility = :vis AND n.publicCategory = :cat AND n.id <> :id ORDER BY n.createdAt DESC")
    List<NewsEvent> findRelatedPublic(
            @Param("vis") NewsVisibility vis,
            @Param("cat") NewsCategory publicCategory,
            @Param("id") String id
    );

    @Query("SELECT n FROM NewsEvent n WHERE n.visibility = :vis AND n.publicCategory = :cat AND n.id <> :id AND n.family.familyId = :familyId ORDER BY n.createdAt DESC")
    List<NewsEvent> findRelatedPublicForFamily(
            @Param("vis") NewsVisibility vis,
            @Param("cat") NewsCategory publicCategory,
            @Param("id") String id,
            @Param("familyId") String familyId
    );

    @Query("SELECT DISTINCT n FROM NewsEvent n LEFT JOIN FETCH n.user LEFT JOIN FETCH n.family WHERE " +
            "(:search IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(n.summary) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:category IS NULL OR n.publicCategory = :category) AND " +
            "(:vis IS NULL OR n.visibility = :vis) AND " +
            "(:familyId IS NULL OR n.family.familyId = :familyId) " +
            "ORDER BY n.createdAt DESC")
    List<NewsEvent> searchSiteNewsForAdmin(
            @Param("search") String search,
            @Param("category") NewsCategory category,
            @Param("vis") NewsVisibility vis,
            @Param("familyId") String familyId
    );

    /** Danh sách đầy đủ cho màn quản trị (gồm DRAFT). */
    @Query("SELECT n FROM NewsEvent n WHERE n.family.familyId = :familyId ORDER BY n.createdAt DESC")
    List<NewsEvent> findByFamilyIdForAdminOrderByCreatedAtDesc(@Param("familyId") String familyId);

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

    // --- Phiên bản hỗ trợ nhiều chi (familyIds) ---
    /** Chỉ lấy các tin công khai (PUBLIC_SITE) trong các familyIds */
    @Query("SELECT n FROM NewsEvent n WHERE n.family.familyId IN :familyIds AND n.category.categoryId = :categoryId AND n.startAt > :now AND n.visibility = com.family.app.model.NewsVisibility.PUBLIC_SITE ORDER BY n.startAt ASC")
    List<NewsEvent> findUpcomingEventsByFamilyIdsAndCategory(@Param("familyIds") Collection<String> familyIds,
                                                               @Param("categoryId") String categoryId,
                                                               @Param("now") LocalDateTime now);

    /** Chỉ lấy các tin công khai (PUBLIC_SITE) trong các familyIds */
    @Query("SELECT n FROM NewsEvent n WHERE n.family.familyId IN :familyIds AND n.visibility = com.family.app.model.NewsVisibility.PUBLIC_SITE ORDER BY n.createdAt DESC")
    List<NewsEvent> findLatestNewsByFamilyIds(@Param("familyIds") Collection<String> familyIds);

    long countByFamily_FamilyId(String familyId);

    /** Tin theo nhiều chi (dashboard / thống kê phạm vi cây). */
    @Query("SELECT COUNT(n) FROM NewsEvent n WHERE n.family.familyId IN :familyIds "
            + "AND (n.visibility IS NULL OR n.visibility <> com.family.app.model.NewsVisibility.DRAFT)")
    long countVisibleByFamilyFamilyIdIn(@Param("familyIds") Collection<String> familyIds);
}
