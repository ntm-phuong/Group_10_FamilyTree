package com.family.app.repository;

import com.family.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    List<User> findByFamily_FamilyIdOrderByOrderInFamilyAsc(String familyId);

    List<User> findByFullNameContainingIgnoreCase(String fullName);

    boolean existsByParentId(String parentId);

    List<User> findByParentId(String parentId);

    List<User> findByFamily_FamilyIdAndGenerationOrderByOrderInFamilyAsc(String familyId, Integer generation);
}