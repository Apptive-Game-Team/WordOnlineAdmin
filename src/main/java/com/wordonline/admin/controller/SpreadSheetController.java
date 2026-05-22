package com.wordonline.admin.controller;

import com.wordonline.admin.repository.tag.TagRepository;
import com.wordonline.admin.service.SpreadSheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/spread-sheets")
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class SpreadSheetController {

    private final SpreadSheetService spreadSheetService;
    private final TagRepository tagRepository;

    @GetMapping("/game-objects")
    public String getGameObjects(
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "db", defaultValue = "primary") String db,
            Model model
    ) {
        boolean secondary = "secondary".equalsIgnoreCase(db);
        model.addAttribute("gameObjects", spreadSheetService.getGameObjects(tags, secondary));
        model.addAttribute("allTags", tagRepository.findAll());
        model.addAttribute("selectedDb", secondary ? "secondary" : "primary");
        model.addAttribute("secondaryDatabaseEnabled", spreadSheetService.hasSecondaryDatabase());
        return "admin-spreadsheet";
    }
}
