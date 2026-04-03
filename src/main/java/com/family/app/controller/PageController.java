package com.family.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {
    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    @GetMapping("/home")
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
        model.addAttribute("activeMenu", "family-tree");
        return "public/family-tree";
    }

    @GetMapping("/news")
    public String newsList(@RequestParam(required = false) String category,
                           @RequestParam(required = false) String q) {
        return "public/news-list";
    }

    @GetMapping("/news/{slug}")
    public String newsDetail(@PathVariable String slug) {
        return "public/news-detail";
    }

    @GetMapping("/family-head")
    public String showDashboard() {
        return "familyhead/dashboard";
    }
}
