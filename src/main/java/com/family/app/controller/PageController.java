package com.family.app.controller;

import com.family.app.config.AppClanProperties;
import com.family.app.model.Family;
import com.family.app.model.User;
import com.family.app.repository.FamilyRepository;
import com.family.app.repository.UserRepository;
import com.family.app.service.FamilyScopeService;
import com.family.app.service.PublicMemberProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final AppClanProperties clanProperties;
    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;
    private final FamilyScopeService familyScopeService;
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
    public String about(Model model, Authentication auth) {
        String rootId = clanProperties.getFamilyId();
        if (rootId == null || rootId.isBlank()) {
            rootId = "";
        } else {
            rootId = rootId.trim();
        }
        if (auth != null && auth.getPrincipal() instanceof User u) {
            User hydrated = userRepository.findByIdWithFamily(u.getUserId()).orElse(u);
            if (hydrated.getFamily() != null && hydrated.getFamily().getFamilyId() != null) {
                rootId = familyScopeService.resolveRootFamilyId(hydrated.getFamily().getFamilyId());
            }
        }
        model.addAttribute("aboutScopeFamilyId", rootId);
        if (!rootId.isEmpty()) {
            familyRepository.findById(rootId).ifPresent(f -> {
                model.addAttribute("aboutFamilyName", f.getFamilyName());
                if (f.getDescription() != null && !f.getDescription().isBlank()) {
                    model.addAttribute("aboutFamilyDescription", f.getDescription().trim());
                }
            });
        }
        if (!model.containsAttribute("aboutFamilyName")) {
            model.addAttribute("aboutFamilyName", clanProperties.getDisplayName());
        }
        model.addAttribute("activeMenu", "about");
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
    public String familyTreePage(Model model, @AuthenticationPrincipal User principal) {
        String fid = clanProperties.getFamilyId();
        String name = familyRepository.findById(fid)
                .map(Family::getFamilyName)
                .orElse(clanProperties.getDisplayName());
        if (principal != null) {
            User hydrated = userRepository.findByIdWithFamily(principal.getUserId()).orElse(principal);
            if (hydrated.getFamily() != null) {
                fid = familyScopeService.resolveRootFamilyId(hydrated.getFamily().getFamilyId());
                name = familyRepository.findById(fid).map(Family::getFamilyName).orElse(name);
            }
        }
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
