package com.family.app.controller;

import com.family.app.config.AppClanProperties;
import com.family.app.model.NewsCategory;
import com.family.app.model.NewsEvent;
import com.family.app.model.NewsVisibility;
import com.family.app.model.User;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.UserRepository;
import com.family.app.service.SiteNewsService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

/**
 * Tin tức công khai / nội bộ — một dòng họ ({@link AppClanProperties}).
 */
@Controller
public class PublicNewsController {

    private final SiteNewsService siteNewsService;
    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;
    private final AppClanProperties clanProperties;

    public PublicNewsController(
            SiteNewsService siteNewsService,
            FamilyRepository familyRepository,
            UserRepository userRepository,
            AppClanProperties clanProperties
    ) {
        this.siteNewsService = siteNewsService;
        this.familyRepository = familyRepository;
        this.userRepository = userRepository;
        this.clanProperties = clanProperties;
    }

    @GetMapping("/news")
    public String list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String visibility,
            Model model,
            Authentication auth
    ) {
        User viewer = resolveViewer(auth);
        NewsCategory cat = SiteNewsService.parseCategory(category);
        String query = q != null ? q.trim() : "";
        NewsVisibility visibilityFilter = SiteNewsService.parseVisibilityFilter(visibility);
        String visibilityParam = null;
        if (visibilityFilter == NewsVisibility.PUBLIC_SITE) {
            visibilityParam = "public";
        } else if (visibilityFilter == NewsVisibility.FAMILY_ONLY) {
            visibilityParam = "internal";
        }

        String filterFamilyId = clanProperties.getFamilyId();
        String filterFamilyName = familyRepository.findById(filterFamilyId)
                .map(f -> f.getFamilyName())
                .orElse(clanProperties.getDisplayName());

        List<NewsEvent> articles = siteNewsService.findPublishedForNewsPage(
                query, cat, filterFamilyId, viewer, visibilityFilter);

        boolean showFeatured = cat == null && query.isEmpty();
        List<NewsEvent> featured = showFeatured
                ? siteNewsService.findFeaturedPublished(2, filterFamilyId)
                : Collections.emptyList();

        model.addAttribute("articles", articles);
        model.addAttribute("featuredArticles", featured);
        model.addAttribute("showFeatured", showFeatured);
        model.addAttribute("categories", NewsCategory.values());
        model.addAttribute("selectedCategory", cat);
        model.addAttribute("q", query);
        model.addAttribute("filterFamilyId", filterFamilyId);
        model.addAttribute("filterFamilyName", filterFamilyName);
        model.addAttribute("visibilityParam", visibilityParam);
        model.addAttribute("clanDisplayName", clanProperties.getDisplayName());
        model.addAttribute("activeMenu", "news");
        return "public/news-list";
    }

    @GetMapping("/news/{slug}")
    public String detail(
            @PathVariable String slug,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication auth
    ) {
        User viewer = resolveViewer(auth);
        return siteNewsService.findArticleForViewerBySlug(slug, viewer)
                .map(article -> {
                    siteNewsService.incrementViewCount(article.getId());
                    article.setViewCount(article.getViewCount() + 1);
                    model.addAttribute("article", article);
                    model.addAttribute("relatedArticles", siteNewsService.findRelated(article, 3,
                            article.getFamily() != null ? article.getFamily().getFamilyId() : null, viewer));
                    model.addAttribute("categories", NewsCategory.values());
                    model.addAttribute("activeMenu", "news");
                    return "public/news-detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài viết");
                    return "redirect:/news";
                });
    }

    private User resolveViewer(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            return null;
        }
        return userRepository.findByIdWithFamily(u.getUserId()).orElse(null);
    }
}
