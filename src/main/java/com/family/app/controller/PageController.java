package com.family.app.controller;

import com.family.app.config.AppClanProperties;
import com.family.app.repository.FamilyRepository;
import com.family.app.service.PublicMemberProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final AppClanProperties clanProperties;
    private final FamilyRepository familyRepository;
    private final PublicMemberProfileService publicMemberProfileService;

    @GetMapping("/")
    public String root() {
        return "public/home-guest"; // Mở file home-guest.html
    }


    @GetMapping("/home")
    public String home() {
        return "public/home"; // Mở file home.html
    }

    @GetMapping("/about")
    public String about() {
        return "public/about";
    }

    @GetMapping("/login")
    public String login() {
        return "public/login";
    }

    @GetMapping("/family-head")
    public String showDashboard() {
        return "admin/dashboard";
    }

    /**
     * Trang gia phả (Thymeleaf). Đặt cùng {@link PageController} như các trang public khác
     * để luôn khớp {@code RequestMappingHandlerMapping} (tránh rơi vào static resource 404).
     */
    @GetMapping("/family-tree")
    public String familyTreePage(Model model) {
        String fid = clanProperties.getFamilyId();
        String name = familyRepository.findById(fid)
                .map(f -> f.getFamilyName())
                .orElse(clanProperties.getDisplayName());
        model.addAttribute("clanFamilyId", fid);
        model.addAttribute("clanFamilyName", name);
        model.addAttribute("activeMenu", "family-tree");
        return "public/family-tree";
    }

    /** Phải khai báo trước {@code /member/{id}} để {@code /member/edit/...} không bị coi id = "edit". */
    @GetMapping("/member/edit/{id}")
    public String editMemberDetail(@PathVariable String id, Model model) {
        publicMemberProfileService.addMemberEditToModel(id, model);
        return "public/edit-member-detail";
    }

    @GetMapping("/member/{id}")
    public String memberDetail(@PathVariable String id, Model model) {
        publicMemberProfileService.addMemberDetailToModel(id, model);
        return "public/member-detail";
    }
}
