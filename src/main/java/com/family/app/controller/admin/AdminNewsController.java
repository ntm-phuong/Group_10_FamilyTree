package com.family.app.controller.admin;

import com.family.app.model.NewsCategory;
import com.family.app.model.NewsEvent;
import com.family.app.model.NewsVisibility;
import com.family.app.model.User;
import com.family.app.service.SiteNewsService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/news")
public class AdminNewsController {

    private final SiteNewsService siteNewsService;

    public AdminNewsController(SiteNewsService siteNewsService) {
        this.siteNewsService = siteNewsService;
    }

    @ModelAttribute
    public void global(Model model) {
        model.addAttribute("currentUser", currentUser());
        model.addAttribute("activeMenu", "news");
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            Model model
    ) {
        List<NewsEvent> articles = siteNewsService.findAdminList(search, category, status);
        model.addAttribute("articles", articles);
        model.addAttribute("categories", NewsCategory.values());
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("categoryFilter", category != null ? category : "");
        model.addAttribute("statusFilter", status != null ? status : "");
        model.addAttribute("article", siteNewsService.emptyDraft());
        return "admin/news/list";
    }

    @PostMapping("/new")
    public String create(
            @ModelAttribute NewsEvent article,
            @RequestParam(required = false) String _draft,
            RedirectAttributes ra
    ) {
        if (article.getPublicCategory() == null) {
            ra.addFlashAttribute("errorMessage", "Vui lòng chọn danh mục");
            return "redirect:/admin/news";
        }
        boolean isDraft = _draft != null;
        boolean published = !isDraft;
        siteNewsService.createSiteArticle(
                article.getTitle(),
                article.getPublicCategory(),
                article.getSummary(),
                article.getContent(),
                published,
                article.isFeatured(),
                currentUser()
        );
        ra.addFlashAttribute("successMessage", isDraft ? "Đã lưu nháp" : "Đã tạo bài viết");
        return "redirect:/admin/news";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model, RedirectAttributes ra) {
        return siteNewsService.findByIdForAdmin(id)
                .map(article -> {
                    model.addAttribute("article", article);
                    model.addAttribute("categories", NewsCategory.values());
                    model.addAttribute("visibilities", NewsVisibility.values());
                    model.addAttribute("formMode", "edit");
                    return "admin/news/form";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("errorMessage", "Không tìm thấy bài viết");
                    return "redirect:/admin/news";
                });
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable String id,
            @ModelAttribute NewsEvent article,
            RedirectAttributes ra
    ) {
        siteNewsService.updateSiteArticle(id, article);
        ra.addFlashAttribute("successMessage", "Đã cập nhật bài viết");
        return "redirect:/admin/news";
    }

    @PostMapping("/{id}/toggle-publish")
    public String togglePublish(@PathVariable String id, RedirectAttributes ra) {
        siteNewsService.togglePublish(id);
        ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái đăng bài");
        return "redirect:/admin/news";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes ra) {
        siteNewsService.delete(id);
        ra.addFlashAttribute("successMessage", "Đã xóa bài viết");
        return "redirect:/admin/news";
    }

    private static User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
