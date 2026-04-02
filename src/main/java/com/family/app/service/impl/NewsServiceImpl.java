package com.family.app.service.impl;

import com.family.app.dto.NewsRequest;
import com.family.app.dto.NewsResponse;
import com.family.app.model.NewsEvent;
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
                .visibility(request.getVisibility())
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

        if (request.getCategoryId() != null) {
            news.setCategory(categoryRepository.findById(request.getCategoryId()).orElse(null));
        }

        return mapToResponse(newsRepository.save(news));
    }

    @Override
    public void deleteNews(String id) {
        newsRepository.deleteById(id);
    }

    // Mapping thủ công để đảm bảo khớp chính xác NewsResponse của bạn
    private NewsResponse mapToResponse(NewsEvent news) {
        NewsResponse response = new NewsResponse();
        response.setId(news.getId());
        response.setTitle(news.getTitle());
        response.setSummary(news.getSummary());
        response.setContent(news.getContent());
        response.setCreatedAt(news.getCreatedAt());
        // Giả sử bạn thêm @UpdateTimestamp vào Entity, nếu chưa có nó sẽ trả về null
        // response.setUpdatedAt(news.getUpdatedAt());

        if (news.getCategory() != null) {
            response.setCategoryId(news.getCategory().getCategoryId());
            response.setCategoryName(news.getCategory().getName());
        }

        if (news.getUser() != null) {
            response.setUserId(news.getUser().getUserId());
            response.setUserName(news.getUser().getFullName());
        }

        return response;
    }
}