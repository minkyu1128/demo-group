package com.example.fcm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class IndexController {
    @GetMapping( value = {""})
    public String init() {
        return "redirect:/index";
    }
    @GetMapping( value = {"/index"})
    public String index() {
        return "index";
    }
}
