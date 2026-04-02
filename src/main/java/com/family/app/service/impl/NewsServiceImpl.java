package com.family.app.service.impl;

import com.family.app.dto.NewsRequest;
import com.family.app.dto.NewsResponse;
import com.family.app.model.NewsEvent;
import com.family.app.model.NewsVisibility;
import com.family.app.repository.CategoryRepository;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.NewsEventRepository;
import com.family.app.repository.UserRepository;
import com.family.app.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsServiceImpl extends NewsService {

    @Autowired private NewsEventRepository newsRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private FamilyRepository familyRepository;
    @Autowired private UserRepository userRepository;

    @Override
    public List<NewsResponse> getNewsByFamily(String familyId) {
        return newsRepository.findByFamily_FamilyIdOrderByCreatedAtDesc(familyId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public NewsResponse createNews(NewsRequest request) {
        NewsEvent news = NewsEvent.builder()
                .title(request.getTitle())
                .summary(request.getSummary())
                .content(request.getContent())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .location(request.getLocation())
                .remindBefore(request.getRemindBefore())
                .visibility(parseVisibility(request.getVisibility()))
                .family(familyRepository.findById(request.getFamilyId()).orElse(null))
                .category(categoryRepository.findById(request.getCategoryId()).orElse(null))
                .user(userRepository.findById(request.getAuthorId()).orElse(null))
                .build();

        return mapToResponse(newsRepository.save(news));
    }

    @Override
    @Transactional
    public NewsResponse updateNews(String id, NewsRequest request) {
        NewsEvent news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tin tức/sự kiện!"));

        news.setTitle(request.getTitle());
        news.setSummary(request.getSummary());
        news.setContent(request.getContent());
        news.setLocation(request.getLocation());
        news.setStartAt(request.getStartAt());
        news.setEndAt(request.getEndAt());
        news.setRemindBefore(request.getRemindBefore());
        if (request.getVisibility() != null) {
            news.setVisibility(parseVisibility(request.getVisibility()));
        }

        if (request.getCategoryId() != null) {
            news.setCategory(categoryRepository.findById(request.getCategoryId()).orElse(null));
        }

        return mapToResponse(newsRepository.save(news));
    }

    @Override
    public void deleteNews(String id) {
        newsRepository.deleteById(id);
    }

    private NewsResponse mapToResponse(NewsEvent news) {
        NewsResponse response = new NewsResponse();
        response.setId(news.getId());
        response.setTitle(news.getTitle());
        response.setSummary(news.getSummary());
        response.setContent(news.getContent());
        response.setCreatedAt(news.getCreatedAt());
        response.setStartAt(news.getStartAt());

        if (news.getCategory() != null) {
            response.setCategoryId(news.getCategory().getCategoryId());
            response.setCategoryName(news.getCategory().getName());
        }

        if (news.getUser() != null) {
            response.setUserId(news.getUser().getUserId());
            response.setUserName(news.getUser().getFullName());
        }

        if (news.getVisibility() != null) {
            response.setVisibility(news.getVisibility().name());
        }
        response.setSlug(news.getSlug());
        response.setFeatured(news.isFeatured());
        response.setViewCount(news.getViewCount());
        response.setCoverImage(news.getCoverImage());
        if (news.getPublicCategory() != null) {
            response.setPublicCategory(news.getPublicCategory().name());
        }

        return response;
    }

    private static NewsVisibility parseVisibility(String raw) {
        if (raw == null || raw.isBlank()) {
            return NewsVisibility.FAMILY_ONLY;
        }
        try {
            return NewsVisibility.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NewsVisibility.FAMILY_ONLY;
        }
    }
}
