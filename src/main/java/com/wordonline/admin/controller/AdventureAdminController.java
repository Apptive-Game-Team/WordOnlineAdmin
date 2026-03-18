package com.wordonline.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.wordonline.admin.service.AdventureService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class AdventureAdminController {

    private final AdventureService adventureService;

    @GetMapping("/admin/adventure")
    public String adminAdventure(Model model) {
        model.addAttribute("adventures", adventureService.findAllAdventures());
        return "admin-adventure";
    }
}
