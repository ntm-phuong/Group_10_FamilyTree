package com.family.app.service;

import com.family.app.dto.NewsRequest;
import com.family.app.dto.NewsResponse;
import com.family.app.model.NewsEvent;
import com.family.app.repository.NewsEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public abstract class NewsService {

    @Autowired
    private NewsEventRepository newsRepository;

    @Transactional(readOnly = true)
    public List<NewsResponse> getNewsList(String familyId, String search, String categoryId) {
        List<NewsEvent> newsList;

        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasCategory = categoryId != null && !categoryId.trim().isEmpty();

        if (hasSearch && hasCategory) {
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
                .stream()
                .limit(5)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NewsResponse getNewsById(String id) {
        return newsRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<NewsResponse> getUpcomingEvents(String familyId, String categoryId) {
        return newsRepository.findTop4ByFamily_FamilyIdAndCategory_CategoryIdAndStartAtAfterOrderByStartAtAsc(
                        familyId,
                        categoryId,
                        LocalDateTime.now()
                )
                .stream()
                .limit(4)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NewsResponse> getLatestNewsForHome(String familyId) {
        return newsRepository.findTop4ByFamily_FamilyIdOrderByCreatedAtDesc(familyId)
                .stream()
                .limit(4)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private NewsResponse convertToDTO(NewsEvent news) {
        NewsResponse dto = new NewsResponse();
        dto.setId(news.getId());
        dto.setTitle(news.getTitle());
        dto.setSummary(news.getSummary());
        dto.setContent(news.getContent());
        dto.setCreatedAt(news.getCreatedAt());

        dto.setStartAt(news.getStartAt());
        dto.setEndAt(news.getEndAt());
        dto.setLocation(news.getLocation());
        dto.setRemindBefore(news.getRemindBefore());

        if (news.getCategory() != null) {
            dto.setCategoryId(news.getCategory().getCategoryId());
            dto.setCategoryName(news.getCategory().getName());
        }

        if (news.getUser() != null) {
            dto.setUserId(news.getUser().getUserId());
            dto.setUserName(news.getUser().getFullName());
        }

        if (news.getVisibility() != null) {
            dto.setVisibility(news.getVisibility().name());
        }
        dto.setSlug(news.getSlug());
        dto.setFeatured(news.isFeatured());
        dto.setViewCount(news.getViewCount());
        dto.setCoverImage(news.getCoverImage());
        if (news.getPublicCategory() != null) {
            dto.setPublicCategory(news.getPublicCategory().name());
        }

        if (news.getFamily() != null) {
            dto.setFamilyId(news.getFamily().getFamilyId());
            dto.setFamilyName(news.getFamily().getFamilyName());
        }

        return dto;
    }

    public abstract List<NewsResponse> getNewsByFamily(String familyId);

    public abstract NewsResponse createNews(NewsRequest request);

    public abstract NewsResponse updateNews(String id, NewsRequest request);

    public abstract void deleteNews(String id);
}