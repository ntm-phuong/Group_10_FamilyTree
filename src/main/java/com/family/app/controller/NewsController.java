package com.family.app.controller;

import com.family.app.dto.NewsResponse;
import com.family.app.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Autowired
    private NewsService newsService;

    /**
     * API chính cho danh sách tin tức
     * FE gọi: /api/news?familyId=... (bắt buộc) &search=... &categoryId=...
     */
    @GetMapping
    public ResponseEntity<?> getNews(
            @RequestParam String familyId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoryId) {
        try {
            List<NewsResponse> news = newsService.getNewsList(familyId, search, categoryId);
            return ResponseEntity.ok(news);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi lấy dữ liệu: " + e.getMessage());
        }
    }

    // Lấy tin mới nhất cho Widget/Sidebar
    @GetMapping("/latest")
    public ResponseEntity<?> getLatest(@RequestParam String familyId) {
        try {
            return ResponseEntity.ok(newsService.getLatestNews(familyId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    // Lấy chi tiết bài viết (Khi bấm vào xem chi tiết)
    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable String id) {
        NewsResponse news = newsService.getNewsById(id);
        if (news == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(news);
    }
}