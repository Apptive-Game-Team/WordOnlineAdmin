package com.wordonline.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.wordonline.admin.service.AdventureService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class AdventureAdminController {

    private final AdventureService adventureService;

    @GetMapping("/admin/adventure")
    public String adminAdventure(
            @RequestParam(value = "db", defaultValue = "primary") String db,
            Model model
    ) {
        boolean secondary = "secondary".equalsIgnoreCase(db);
        model.addAttribute("adventures", adventureService.findAllAdventures(secondary));
        model.addAttribute("selectedDb", secondary ? "secondary" : "primary");
        model.addAttribute("secondaryDatabaseEnabled", adventureService.hasSecondaryDatabase());
        return "admin-adventure";
    }
}
