package com.family.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {


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
}
