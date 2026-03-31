package com.family.app.service;

import com.family.app.dto.NewsResponse;
import com.family.app.model.NewsEvent;
import com.family.app.repository.NewsEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsService {

    @Autowired
    private NewsEventRepository newsRepository;

    @Transactional(readOnly = true)
    public List<NewsResponse> getNewsList(String familyId, String search, String categoryId) {
        List<NewsEvent> newsList;

        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasCategory = categoryId != null && !categoryId.trim().isEmpty();

        if (hasSearch && hasCategory) {
            // Lọc theo cả dòng họ, cả danh mục VÀ cả từ khóa
            newsList = newsRepository.findByFamily_FamilyIdAndCategory_CategoryIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(familyId, categoryId, search);
        } else if (hasSearch) {
            newsList = newsRepository.findByFamily_FamilyIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(familyId, search);
        } else if (hasCategory) {
            newsList = newsRepository.findByFamily_FamilyIdAndCategory_CategoryIdOrderByCreatedAtDesc(familyId, categoryId);
        } else {
            newsList = newsRepository.findByFamily_FamilyIdOrderByCreatedAtDesc(familyId);
        }

        return newsList.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NewsResponse> getLatestNews(String familyId) {
        return newsRepository.findTop5ByFamily_FamilyIdOrderByCreatedAtDesc(familyId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NewsResponse getNewsById(String id) {
        return newsRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    // Hàm chuyển đổi Model -> DTO (Khớp hoàn toàn với NewsResponse của bạn)
    private NewsResponse convertToDTO(NewsEvent news) {
        NewsResponse dto = new NewsResponse();
        dto.setId(news.getId());
        dto.setTitle(news.getTitle());
        dto.setSummary(news.getSummary());
        dto.setContent(news.getContent());
        dto.setCreatedAt(news.getCreatedAt());

        // Lấy thông tin Category (Tên danh mục)
        if (news.getCategory() != null) {
            dto.setCategoryId(news.getCategory().getCategoryId());
            dto.setCategoryName(news.getCategory().getName());
        }

        // Lấy thông tin User (Tên người đăng)
        if (news.getUser() != null) {
            dto.setUserId(news.getUser().getUserId());
            dto.setUserName(news.getUser().getFullName()); // Dùng fullName từ User model
        }

        return dto;
    }
}