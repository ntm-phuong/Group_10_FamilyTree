package com.family.app.repository;

import com.family.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    /**
     * Chỉ FETCH {@code family} + {@code roles}. Quyền từng role tải qua {@link com.family.app.model.Role#permissions}
     * (EAGER) — tránh JOIN FETCH hai collection (roles + permissions) gây nhân bản hàng và mất quyền trên một số role.
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.family "
            + "LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithFamily(@Param("email") String email);

    /** Dùng cho phạm vi quản trị, JWT — load family + roles; permissions của role hydrate trong cùng transaction. */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.family "
            + "LEFT JOIN FETCH u.roles WHERE u.userId = :userId")
    Optional<User> findByIdWithFamily(@Param("userId") String userId);

    List<User> findByFamily_FamilyIdOrderByOrderInFamilyAsc(String familyId);

    List<User> findByFamily_FamilyIdInOrderByGenerationAscOrderInFamilyAsc(Collection<String> familyIds);

    List<User> findByFullNameContainingIgnoreCase(String fullName);

    boolean existsByParentId(String parentId);

    List<User> findByParentId(String parentId);

    List<User> findByFamily_FamilyIdAndGenerationOrderByOrderInFamilyAsc(String familyId, Integer generation);
    long countByFamily_FamilyId(String familyId);

    long countByFamily_FamilyIdIn(Collection<String> familyIds);

    // 2. Đếm Nam (Lưu ý: "Nam" phải khớp với dữ liệu bạn lưu trong DB)
    long countByFamily_FamilyIdAndGender(String familyId, String gender);

    long countByFamily_FamilyIdInAndGender(Collection<String> familyIds, String gender);

    // 3. Tính tổng số đời (Lấy số lớn nhất trong cột generation)
    @Query("SELECT MAX(u.generation) FROM User u WHERE u.family.familyId = :familyId")
    Integer findMaxGenerationByFamilyId(@Param("familyId") String familyId);

    @Query("SELECT MAX(u.generation) FROM User u WHERE u.family.familyId IN :familyIds")
    Integer findMaxGenerationByFamilyIdIn(@Param("familyIds") Collection<String> familyIds);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.roleName = :roleName")
    long countDistinctByRoleName(@Param("roleName") String roleName);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.roleName = :roleName AND u.userId <> :userId")
    long countDistinctByRoleNameExcludingUser(@Param("roleName") String roleName, @Param("userId") String userId);

    /**
     * Trưởng họ hiển thị công khai: ưu tiên {@code ADMIN} (quyền trưởng họ), sau đó {@code FAMILY_BRANCH_MANAGER}.
     * Phạm vi {@code familyIds} = gốc + mọi chi con (caller truyền từ CTE / cây).
     */
    @Query(value = "SELECT u.* FROM users u "
            + "INNER JOIN user_roles ur ON u.user_id = ur.user_id "
            + "INNER JOIN roles r ON ur.role_id = r.role_id "
            + "WHERE u.family_id IN (:familyIds) AND r.role_name IN ('ADMIN', 'FAMILY_BRANCH_MANAGER') "
            + "ORDER BY CASE r.role_name WHEN 'ADMIN' THEN 0 ELSE 1 END, u.user_id "
            + "LIMIT 1",
            nativeQuery = true)
    Optional<User> findFamilyHeadByFamilyIds(@Param("familyIds") Collection<String> familyIds);
}