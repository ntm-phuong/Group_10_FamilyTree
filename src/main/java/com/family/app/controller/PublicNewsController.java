package com.family.app.controller;

import com.family.app.model.NewsCategory;
import com.family.app.model.NewsEvent;
import com.family.app.service.SiteNewsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Controller
public class PublicNewsController {

    private final SiteNewsService siteNewsService;

    public PublicNewsController(SiteNewsService siteNewsService) {
        this.siteNewsService = siteNewsService;
    }

    @GetMapping("/news")
    public String list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            Model model
    ) {
        NewsCategory cat = SiteNewsService.parseCategory(category);
        String query = q != null ? q.trim() : "";
        List<NewsEvent> articles = siteNewsService.findPublishedForPublic(query, cat);

        boolean showFeatured = cat == null && query.isEmpty();
        List<NewsEvent> featured = showFeatured
                ? siteNewsService.findFeaturedPublished(2)
                : Collections.emptyList();

        model.addAttribute("articles", articles);
        model.addAttribute("featuredArticles", featured);
        model.addAttribute("showFeatured", showFeatured);
        model.addAttribute("categories", NewsCategory.values());
        model.addAttribute("selectedCategory", cat);
        model.addAttribute("q", query);
        model.addAttribute("activeMenu", "news");
        return "public/news-list";
    }

    @GetMapping("/news/{slug}")
    public String detail(
            @PathVariable String slug,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        return siteNewsService.findPublicBySlug(slug)
                .map(article -> {
                    siteNewsService.incrementViewCount(article.getId());
                    article.setViewCount(article.getViewCount() + 1);
                    model.addAttribute("article", article);
                    model.addAttribute("relatedArticles", siteNewsService.findRelated(article, 3));
                    model.addAttribute("categories", NewsCategory.values());
                    model.addAttribute("activeMenu", "news");
                    return "public/news-detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài viết");
                    return "redirect:/news";
                });
    }
}
