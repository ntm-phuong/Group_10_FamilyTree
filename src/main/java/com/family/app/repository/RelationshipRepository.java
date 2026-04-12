package com.family.app.repository;

import com.family.app.model.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;

@Repository
public interface RelationshipRepository extends JpaRepository<Relationship, String> {

    List<Relationship> findByPerson1_UserIdAndRelType(String person1Id, String relType);

    List<Relationship> findByPerson2_UserIdAndRelType(String person2Id, String relType);

    @Query("SELECT r FROM Relationship r WHERE (r.person1.userId = :userId OR r.person2.userId = :userId) " +
            "AND r.relType = 'SPOUSE'")
    List<Relationship> findSpouses(@Param("userId") String userId);

    @Query("SELECT r FROM Relationship r " +
            "JOIN FETCH r.person1 " + // FETCH dữ liệu User 1
            "JOIN FETCH r.person2 " + // FETCH dữ liệu User 2
            "WHERE r.person1.family.familyId = :familyId")
    List<Relationship> findAllByFamilyId(@Param("familyId") String familyId);

    @Query("SELECT DISTINCT r FROM Relationship r " +
            "JOIN FETCH r.person1 p1 JOIN FETCH p1.family " +
            "JOIN FETCH r.person2 p2 JOIN FETCH p2.family " +
            "WHERE p1.family.familyId IN :ids AND p2.family.familyId IN :ids")
    List<Relationship> findAllWhereBothPersonsInFamilyIds(@Param("ids") Collection<String> ids);

    boolean existsByRelTypeAndPerson1_UserIdAndPerson2_UserId(String relType, String person1UserId, String person2UserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Relationship r WHERE r.relType = 'PARENT_CHILD' "
            + "AND r.person1.userId = :p1 AND r.person2.userId = :p2")
    void deleteParentChildBetween(@Param("p1") String parentUserId, @Param("p2") String childUserId);

    /** Mọi cạnh cha/mẹ → con (person2 = con), dùng khi đổi parent_id hoặc đồng bộ từ parent_id. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Relationship r WHERE r.relType = 'PARENT_CHILD' AND r.person2.userId = :childId")
    void deleteParentChildLinksToChild(@Param("childId") String childId);

    /** Xóa mọi quan hệ có tham chiếu tới user (vợ/chồng, cha–con, …) trước khi xóa user. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Relationship r WHERE r.person1.userId = :userId OR r.person2.userId = :userId")
    void deleteAllInvolvingUserId(@Param("userId") String userId);

}