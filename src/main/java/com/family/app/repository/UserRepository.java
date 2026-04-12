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

    // 2. Đếm Nam (Lưu ý: "Nam" phải khớp với dữ liệu bạn lưu trong DB)
    long countByFamily_FamilyIdAndGender(String familyId, String gender);

    // --- Các phương thức hỗ trợ thống kê theo phạm vi nhiều chi (familyIds) ---
    long countByFamily_FamilyIdIn(Collection<String> familyIds);

    long countByFamily_FamilyIdInAndGender(Collection<String> familyIds, String gender);

    // 3. Tính tổng số đời (Lấy số lớn nhất trong cột generation)
    @Query("SELECT MAX(u.generation) FROM User u WHERE u.family.familyId = :familyId")
    Integer findMaxGenerationByFamilyId(@Param("familyId") String familyId);

    @Query("SELECT MAX(u.generation) FROM User u WHERE u.family.familyId IN :familyIds")
    Integer findMaxGenerationByFamilyIds(@Param("familyIds") Collection<String> familyIds);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.roleName = :roleName")
    long countDistinctByRoleName(@Param("roleName") String roleName);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.roleName = :roleName AND u.userId <> :userId")
    long countDistinctByRoleNameExcludingUser(@Param("roleName") String roleName, @Param("userId") String userId);

    @Query(value = "SELECT u.* FROM users u "
            + "JOIN user_roles ur ON u.user_id = ur.user_id "
            + "JOIN roles r ON ur.role_id = r.role_id "
            + "WHERE u.family_id = :familyId AND r.role_name = 'FAMILY_BRANCH_MANAGER' "
            + "LIMIT 1",
            nativeQuery = true)
    Optional<User> findFamilyHeadByFamilyId(@Param("familyId") String familyId);
}