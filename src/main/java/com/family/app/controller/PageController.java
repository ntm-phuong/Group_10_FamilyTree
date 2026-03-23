package com.family.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "pages/home";
    }

    @GetMapping("/about")
    public String about() {
        return "pages/about";
    }

    @GetMapping("/login")
    public String login() {
        return "pages/login";
    }

    @GetMapping("/family-tree")
    public String familyTree() {
        return "pages/family-tree/index";
    }

    @GetMapping("/news")
    public String newsList() {
        return "pages/news/index";
    }
}