package com.wordonline.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.wordonline.admin.service.QuestService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class QuestAdminController {

    private final QuestService questService;

    @GetMapping("/admin/quest")
    public String adminQuest(
            @RequestParam(value = "db", defaultValue = "primary") String db,
            Model model
    ) {
        boolean secondary = "secondary".equalsIgnoreCase(db);
        model.addAttribute("quests", questService.findAllQuests(secondary));
        model.addAttribute("selectedDb", secondary ? "secondary" : "primary");
        model.addAttribute("secondaryDatabaseEnabled", questService.hasSecondaryDatabase());
        return "admin-quest";
    }
}
