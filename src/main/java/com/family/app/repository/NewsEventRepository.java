package com.family.app.repository;

import com.family.app.model.NewsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NewsEventRepository extends JpaRepository<NewsEvent, String> {

    List<NewsEvent> findByFamily_FamilyIdOrderByCreatedAtDesc(String familyId);

    List<NewsEvent> findByCategory_CategoryId(String categoryId);
}