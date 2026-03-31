package com.family.app.repository;

import com.family.app.model.NewsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NewsEventRepository extends JpaRepository<NewsEvent, String> {

    // 1. Lọc theo dòng họ
    List<NewsEvent> findByFamily_FamilyIdOrderByCreatedAtDesc(String familyId);

    // 2. Tìm kiếm trong dòng họ
    List<NewsEvent> findByFamily_FamilyIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(String familyId, String title);

    // 3. Lọc theo danh mục trong dòng họ
    List<NewsEvent> findByFamily_FamilyIdAndCategory_CategoryIdOrderByCreatedAtDesc(String familyId, String categoryId);

    // 4. THÊM MỚI: Lọc đồng thời Danh mục + Tìm kiếm trong dòng họ
    List<NewsEvent> findByFamily_FamilyIdAndCategory_CategoryIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
            String familyId,
            String categoryId,
            String title
    );

    // 5. Lấy 5 tin mới nhất
    List<NewsEvent> findTop5ByFamily_FamilyIdOrderByCreatedAtDesc(String familyId);
}