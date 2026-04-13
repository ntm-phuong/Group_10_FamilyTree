package com.family.app.controller;

import com.family.app.config.AppClanProperties;
import com.family.app.model.Family;
import com.family.app.model.NewsCategory;
import com.family.app.model.NewsEvent;
import com.family.app.model.NewsVisibility;
import com.family.app.model.User;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.UserRepository;
import com.family.app.security.AppPermissions;
import com.family.app.service.FamilyScopeService;
import com.family.app.service.SiteNewsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Tin tức /news — mặc định theo tổ tông người đăng nhập; có thể chọn xem tin công khai của dòng họ gốc khác.
 */
@Controller
public class PublicNewsController {

    /**
     * Khớp {@code FamilyNewsController} — ai gọi API quản tin thì SSR phải render được nút tạo/sửa/xóa.
     */
    private static final Set<String> FAMILY_NEWS_UI_AUTHORITIES = Set.of(
            AppPermissions.MANAGE_FAMILY_NEWS,
            AppPermissions.MANAGE_CLAN,
            AppPermissions.FAMILY_HEAD,
            "ROLE_FAMILY_NEWS_MANAGER",
            "ROLE_FAMILY_BRANCH_MANAGER"
    );

    private final SiteNewsService siteNewsService;
    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;
    private final AppClanProperties clanProperties;
    private final FamilyScopeService familyScopeService;

    public PublicNewsController(
            SiteNewsService siteNewsService,
            FamilyRepository familyRepository,
            UserRepository userRepository,
            AppClanProperties clanProperties,
            FamilyScopeService familyScopeService
    ) {
        this.siteNewsService = siteNewsService;
        this.familyRepository = familyRepository;
        this.userRepository = userRepository;
        this.clanProperties = clanProperties;
        this.familyScopeService = familyScopeService;
    }

    @GetMapping("/news")
    public String list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String familyId,
            Model model,
            Authentication auth
    ) {
        User viewer = resolveViewer(auth);
        NewsCategory cat = SiteNewsService.parseCategory(category);
        String query = q != null ? q.trim() : "";
        NewsVisibility visibilityFilter = SiteNewsService.parseVisibilityFilter(visibility);
        String visibilityParam = null;
        if (visibilityFilter == NewsVisibility.PUBLIC_SITE) {
            visibilityParam = NewsVisibility.PUBLIC_SITE.name();
        } else if (visibilityFilter == NewsVisibility.FAMILY_ONLY) {
            visibilityParam = NewsVisibility.FAMILY_ONLY.name();
        }

        String configRoot = clanProperties.getFamilyId() != null ? clanProperties.getFamilyId().trim() : "";
        String defaultRootId = configRoot;
        if (viewer != null && viewer.getFamily() != null && viewer.getFamily().getFamilyId() != null) {
            defaultRootId = familyScopeService.resolveRootFamilyId(viewer.getFamily().getFamilyId());
        }

        String requested = familyId != null ? familyId.trim() : "";
        String effectiveScopeId = defaultRootId;
        if (!requested.isEmpty() && familyRepository.findById(requested).isPresent()) {
            effectiveScopeId = familyScopeService.resolveRootFamilyId(requested);
        }

        String effectiveScopeForService = effectiveScopeId.isBlank() ? null : effectiveScopeId;
        boolean viewingOtherClanPublic = effectiveScopeForService != null
                && !defaultRootId.isBlank()
                && !effectiveScopeId.equals(defaultRootId);

        if (viewingOtherClanPublic && visibilityFilter == NewsVisibility.FAMILY_ONLY) {
            visibilityFilter = null;
            visibilityParam = null;
        }

        List<NewsEvent> articles = siteNewsService.findPublishedForNewsPage(
                query, cat, effectiveScopeForService, viewer, visibilityFilter, viewingOtherClanPublic);

        boolean showFeatured = cat == null && query.isEmpty() && effectiveScopeForService != null;
        List<NewsEvent> featured = showFeatured
                ? siteNewsService.findFeaturedPublishedForScope(2, effectiveScopeId)
                : Collections.emptyList();

        String filterFamilyName = familyRepository.findById(effectiveScopeId)
                .map(Family::getFamilyName)
                .orElse(clanProperties.getDisplayName());
        String defaultFamilyName = familyRepository.findById(defaultRootId)
                .map(Family::getFamilyName)
                .orElse(clanProperties.getDisplayName());

        model.addAttribute("articles", articles);
        model.addAttribute("featuredArticles", featured);
        model.addAttribute("showFeatured", showFeatured);
        model.addAttribute("categories", NewsCategory.values());
        model.addAttribute("selectedCategory", cat);
        model.addAttribute("q", query);
        model.addAttribute("filterFamilyId", effectiveScopeId);
        model.addAttribute("filterFamilyName", filterFamilyName);
        model.addAttribute("defaultNewsFamilyId", defaultRootId);
        model.addAttribute("defaultNewsFamilyName", defaultFamilyName);
        model.addAttribute("newsUrlFamilyId", effectiveScopeForService);
        model.addAttribute("viewingOtherClanPublic", viewingOtherClanPublic);
        model.addAttribute("rootFamiliesForNewsFilter", familyRepository.findByParentFamilyIsNullOrderByFamilyNameAsc());
        model.addAttribute("visibilityParam", visibilityParam);
        model.addAttribute("clanDisplayName", clanProperties.getDisplayName());
        model.addAttribute("currentUser", viewer);
        model.addAttribute("canManageFamilyNews", canManageFamilyNews(auth));
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
                    model.addAttribute("relatedArticles", siteNewsService.findRelated(article, 3, viewer));
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

    private boolean canManageFamilyNews(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (a != null && FAMILY_NEWS_UI_AUTHORITIES.contains(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
