package com.wordonline.admin.controller;

import com.wordonline.admin.repository.tag.TagRepository;
import com.wordonline.admin.service.SpreadSheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/spread-sheets")
public class SpreadSheetController {

    private final SpreadSheetService spreadSheetService;
    private final TagRepository tagRepository;

    @GetMapping("/game-objects")
    public String getGameObjects(
            @RequestParam(value = "tags", required = false) List<String> tags,
            Model model
    ) {
        model.addAttribute("gameObjects", spreadSheetService.getGameObjects(tags));
        model.addAttribute("allTags", tagRepository.findAll());
        return "admin-spreadsheet";
    }
}
