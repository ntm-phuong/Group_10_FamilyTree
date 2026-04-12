package com.family.app.repository;

import com.family.app.model.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyRepository extends JpaRepository<Family, String> {

    @Query("SELECT f FROM Family f LEFT JOIN FETCH f.parentFamily WHERE f.familyId = :id")
    Optional<Family> findByIdWithParentFamily(@Param("id") String id);

    List<Family> findByFamilyNameContainingIgnoreCase(String familyName);

    List<Family> findByParentFamily_FamilyId(String parentFamilyId);

    long countByParentFamily_FamilyId(String parentFamilyId);

    /**
     * Trả về danh sách id của family gồm family gốc và tất cả các chi/nhánh con (đệ quy).
     * Sử dụng Recursive CTE (yêu cầu DB hỗ trợ CTE, ví dụ MySQL8+, PostgreSQL).
     */
    @Query(value = "WITH RECURSIVE descendants AS (SELECT family_id FROM families WHERE family_id = :rootId UNION ALL SELECT f.family_id FROM families f JOIN descendants d ON f.parent_family_id = d.family_id) SELECT family_id FROM descendants", nativeQuery = true)
    List<String> findDescendantFamilyIds(@Param("rootId") String rootId);
}

