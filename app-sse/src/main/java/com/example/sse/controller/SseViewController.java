package com.example.sse.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SseViewController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}