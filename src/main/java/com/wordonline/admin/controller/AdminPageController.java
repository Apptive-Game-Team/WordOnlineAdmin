package com.wordonline.admin.controller;

import com.wordonline.admin.client.GameServerClient;
import com.wordonline.admin.entity.parameter.GameObject;
import com.wordonline.admin.repository.parameter.GameObjectRepository;
import com.wordonline.admin.repository.parameter.ParameterRepository;
import com.wordonline.admin.repository.tag.TagRepository;
import com.wordonline.admin.service.ParameterService;
import com.wordonline.admin.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AdminPageController {

    private final GameObjectRepository gameObjectRepository;
    private final ParameterRepository parameterRepository;
    private final GameServerClient gameServerClient;
    private final TagRepository tagRepository;
    private final ServerService serverService;
    private final ParameterService parameterService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("servers", serverService.getAllServers());
        return "index";
    }

    @PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
    @GetMapping("/admin/game-object")
    public String adminGameObject(Model model) {
        model.addAttribute("gameObjects", gameObjectRepository.findAll(Sort.by("id")));
        return "admin-game-object";
    }

    @PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
    @GetMapping("/admin/game-object/{gameObjectId}")
    public String adminParameterValue(
            @PathVariable Long gameObjectId,
            @RequestParam(value = "db", defaultValue = "primary") String db,
            Model model
    ) {
        boolean secondary = "secondary".equalsIgnoreCase(db);
        model.addAttribute("gameObject", parameterService.getGameObjectDto(gameObjectId, secondary));
        model.addAttribute("parameters", parameterService.getParameterDtos(secondary));
        model.addAttribute("tags", tagRepository.findAll());
        model.addAttribute("gameObjectTags", secondary ? java.util.List.of() : gameObjectRepository.findById(gameObjectId).orElseThrow().getGameObjectTags());
        model.addAttribute("selectedDb", secondary ? "secondary" : "primary");
        model.addAttribute("secondaryDatabaseEnabled", parameterService.hasSecondaryDatabase());
        return "admin-parameter-value";
    }

    @PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
    @GetMapping("/admin/parameter")
    public String adminParameter(
            @RequestParam(value = "db", defaultValue = "primary") String db,
            Model model
    ) {
        boolean secondary = "secondary".equalsIgnoreCase(db);
        model.addAttribute("parameters", parameterService.getParameterDtos(secondary));
        model.addAttribute("selectedDb", secondary ? "secondary" : "primary");
        model.addAttribute("secondaryDatabaseEnabled", parameterService.hasSecondaryDatabase());
        return "admin-parameter";
    }

    @PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
    @GetMapping("/admin/invalidate-cache")
    public String invalidateCache() {
        gameServerClient.invalidateCache();
        return "redirect:/";
    }
}
