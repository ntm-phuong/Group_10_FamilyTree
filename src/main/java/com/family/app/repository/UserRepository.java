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

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.family WHERE u.email = :email")
    Optional<User> findByEmailWithFamily(@Param("email") String email);

    /** Dùng cho phạm vi quản trị — load luôn family để tránh lazy ngoài transaction. */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.family WHERE u.userId = :userId")
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

    // 3. Tính tổng số đời (Lấy số lớn nhất trong cột generation)
    @Query("SELECT MAX(u.generation) FROM User u WHERE u.family.familyId = :familyId")
    Integer findMaxGenerationByFamilyId(@Param("familyId") String familyId);
}