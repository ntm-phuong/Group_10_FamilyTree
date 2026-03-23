package com.family.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

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
}