package com.family.app.service.impl;

import com.family.app.dto.NewsRequest;
import com.family.app.dto.NewsResponse;
import com.family.app.model.Category;
import com.family.app.model.Family;
import com.family.app.model.NewsCategory;
import com.family.app.model.NewsEvent;
import com.family.app.model.NewsVisibility;
import com.family.app.model.User;
import com.family.app.repository.CategoryRepository;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.NewsEventRepository;
import com.family.app.repository.UserRepository;
import com.family.app.service.NewsService;
import com.family.app.util.NewsSlugUtil;
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
        return newsRepository.findByFamilyIdForAdminOrderByCreatedAtDesc(familyId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NewsResponse createNews(NewsRequest request) {
        Family family = familyRepository.findById(request.getFamilyId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dòng họ"));
        User author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tác giả"));

        NewsVisibility vis = parseVisibility(request.getVisibility());
        Category internalCat = request.getCategoryId() != null
                ? categoryRepository.findById(request.getCategoryId()).orElse(null)
                : null;

        NewsEvent.NewsEventBuilder b = NewsEvent.builder()
                .title(request.getTitle())
                .summary(request.getSummary())
                .content(request.getContent())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .location(request.getLocation())
                .remindBefore(request.getRemindBefore())
                .visibility(vis)
                .family(family)
                .category(internalCat)
                .user(author);

        if (vis == NewsVisibility.PUBLIC_SITE || vis == NewsVisibility.DRAFT) {
            NewsCategory pub = parsePublicCategory(request.getPublicCategory());
            if (vis == NewsVisibility.PUBLIC_SITE && pub == null) {
                throw new RuntimeException("Tin công khai (PUBLIC_SITE) cần publicCategory (tab trang /news)");
            }
            b.publicCategory(pub);
            b.featured(Boolean.TRUE.equals(request.getFeatured()));
            b.viewCount(0);
            if (vis == NewsVisibility.PUBLIC_SITE) {
                b.slug(NewsSlugUtil.uniqueSlug(request.getTitle(), newsRepository::existsBySlug));
            } else if (vis == NewsVisibility.DRAFT && pub != null) {
                b.slug(NewsSlugUtil.uniqueSlug(request.getTitle(), newsRepository::existsBySlug));
            }
        } else if (vis == NewsVisibility.FAMILY_ONLY) {
            b.featured(false);
            b.viewCount(0);
            b.slug(NewsSlugUtil.uniqueSlug(request.getTitle(), newsRepository::existsBySlug));
        } else {
            b.featured(false);
            b.viewCount(0);
        }

        return mapToResponse(newsRepository.save(b.build()));
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

        NewsVisibility vis = news.getVisibility();
        if (vis == NewsVisibility.PUBLIC_SITE || vis == NewsVisibility.DRAFT) {
            NewsCategory pub = parsePublicCategory(request.getPublicCategory());
            if (pub != null) {
                news.setPublicCategory(pub);
            }
            if (request.getFeatured() != null) {
                news.setFeatured(request.getFeatured());
            }
            if (vis == NewsVisibility.PUBLIC_SITE && (news.getSlug() == null || news.getSlug().isBlank())) {
                news.setSlug(NewsSlugUtil.uniqueSlug(news.getTitle(), newsRepository::existsBySlug));
            }
        } else if (vis == NewsVisibility.FAMILY_ONLY && (news.getSlug() == null || news.getSlug().isBlank())) {
            news.setSlug(NewsSlugUtil.uniqueSlug(news.getTitle(), newsRepository::existsBySlug));
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
        response.setEndAt(news.getEndAt());
        response.setLocation(news.getLocation());
        response.setRemindBefore(news.getRemindBefore());

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

        if (news.getFamily() != null) {
            response.setFamilyId(news.getFamily().getFamilyId());
            response.setFamilyName(news.getFamily().getFamilyName());
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

    private static NewsCategory parsePublicCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return NewsCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
