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
}

