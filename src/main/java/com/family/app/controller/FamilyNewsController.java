package com.family.app.controller;

import com.family.app.dto.NewsRequest;
import com.family.app.dto.NewsResponse;
import com.family.app.service.NewsService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family-head/news")
@PreAuthorize("hasRole('FAMILY_HEAD')")
public class FamilyNewsController {

    @Autowired private NewsService newsService;

    @GetMapping("/family/{familyId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<NewsResponse>> getByFamily(@PathVariable String familyId) {
        return ResponseEntity.ok(newsService.getNewsByFamily(familyId));
    }

    @PostMapping
    public ResponseEntity<NewsResponse> create(@RequestBody NewsRequest request) {
        return ResponseEntity.ok(newsService.createNews(request));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<NewsResponse> update(@PathVariable String id, @RequestBody NewsRequest request) {
        return ResponseEntity.ok(newsService.updateNews(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id) {
        newsService.deleteNews(id);
        return ResponseEntity.ok("Xóa tin tức thành công.");
    }
}