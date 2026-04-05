package com.family.app.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller // Đảm bảo có Annotation này
public class AdminController {

    @GetMapping("/admin")
    public String adminPage() {
        return "admin/dashboard";
    }
}
