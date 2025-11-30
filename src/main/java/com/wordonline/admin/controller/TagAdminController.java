package com.wordonline.admin.controller;

import com.wordonline.admin.repository.tag.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class TagAdminController {

    private final TagRepository tagRepository;

    @GetMapping("/admin/tag")
    public String adminTag(Model model) {
        model.addAttribute("tags", tagRepository.findAll());
        return "admin-tag";
    }
}
