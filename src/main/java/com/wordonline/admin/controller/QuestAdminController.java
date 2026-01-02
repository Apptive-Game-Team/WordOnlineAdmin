package com.wordonline.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.wordonline.admin.service.QuestService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class QuestAdminController {

    private final QuestService questService;

    @GetMapping("/admin/quest")
    public String adminQuest(Model model) {
        model.addAttribute("quests", questService.findAllQuests());
        return "admin-quest";
    }
}
