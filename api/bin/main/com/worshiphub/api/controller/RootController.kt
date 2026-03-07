package com.worshiphub.api.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class RootController {

    @GetMapping("/")
    fun root(): String = "redirect:/swagger-ui/index.html"
}