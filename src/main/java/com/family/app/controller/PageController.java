package com.family.app.controller;

import com.family.app.model.NewsArticle;
import com.family.app.service.FamilyTreeContentService;
import com.family.app.service.NewsContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequiredArgsConstructor
public class PageController {
    private final FamilyTreeContentService familyTreeContentService;
    private final NewsContentService newsContentService;

    @GetMapping("/")
    public String home() {
        return "public/home";
    }

    @GetMapping("/about")
    public String about() {
        return "public/about";
    }

    @GetMapping("/login")
    public String login() {
        return "public/login";
    }

    @GetMapping("/family-tree")
    public String familyTree(Model model) {
        model.addAttribute("members", familyTreeContentService.findAllMembers());
        model.addAttribute("stats", familyTreeContentService.buildStats());
        model.addAttribute("activeMenu", "family-tree");
        return "public/family-tree";
    }

    @GetMapping("/news")
    public String newsList(@RequestParam(required = false) String category, Model model) {
        NewsArticle.Category selectedCategory = NewsArticle.Category.fromParam(category);
        model.addAttribute("articles", newsContentService.findArticles(selectedCategory));
        model.addAttribute("categories", NewsArticle.Category.values());
        model.addAttribute("selectedCategory", selectedCategory != null ? selectedCategory.name() : null);
        model.addAttribute("featuredArticles", selectedCategory == null ? newsContentService.findFeaturedArticles() : java.util.List.of());
        model.addAttribute("activeMenu", "news");
        return "public/news-list";
    }

    @GetMapping("/news/{slug}")
    public String newsDetail(@PathVariable String slug, Model model) {
        NewsArticle article = newsContentService.findBySlug(slug)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "News article not found"));
        model.addAttribute("article", article);
        model.addAttribute("relatedArticles", newsContentService.findRelatedArticles(article, 3));
        model.addAttribute("activeMenu", "news");
        return "public/news-detail";
    }
}