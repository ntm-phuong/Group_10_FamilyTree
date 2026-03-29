package com.family.app.repository;

import com.family.app.model.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RelationshipRepository extends JpaRepository<Relationship, String> {

    List<Relationship> findByPerson1_UserIdAndRelType(String person1Id, String relType);

    List<Relationship> findByPerson2_UserIdAndRelType(String person2Id, String relType);

    @Query("SELECT r FROM Relationship r WHERE (r.person1.userId = :userId OR r.person2.userId = :userId) " +
            "AND r.relType = 'SPOUSE'")
    List<Relationship> findSpouses(@Param("userId") String userId);

    @Query("SELECT r FROM Relationship r WHERE r.person1.family.familyId = :familyId OR r.person2.family.familyId = :familyId")
    List<Relationship> findAllByFamilyId(@Param("familyId") String familyId);
}