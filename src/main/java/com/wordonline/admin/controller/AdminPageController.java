package com.wordonline.admin.controller;

import com.wordonline.admin.entity.parameter.GameObject;
import com.wordonline.admin.repository.parameter.GameObjectRepository;
import com.wordonline.admin.repository.parameter.ParameterRepository;
import com.wordonline.admin.repository.tag.TagRepository;
import com.wordonline.admin.service.ParameterService;
import com.wordonline.admin.service.ServerService;
import com.wordonline.admin.service.SpreadSheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminPageController {

    private final GameObjectRepository gameObjectRepository;
    private final ParameterRepository parameterRepository;
    private final TagRepository tagRepository;
    private final ServerService serverService;
    private final ParameterService parameterService;
    private final SpreadSheetService spreadSheetService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("primaryServers", serverService.getPrimaryServers());
        model.addAttribute("secondaryServers", serverService.getSecondaryServers());
        model.addAttribute("secondaryDatabaseEnabled", parameterService.hasSecondaryDatabase());
        return "index";
    }

    @PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
    @GetMapping("/admin/game-object")
    public String adminGameObject(Model model) {
        model.addAttribute("gameObjects", parameterService.getGameObjectComparisons());
        model.addAttribute("secondaryDatabaseEnabled", parameterService.hasSecondaryDatabase());
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
        String name = parameterService.getGameObjectDto(gameObjectId, secondary).name();
        return "redirect:/admin/game-object/by-name/"
                + org.springframework.web.util.UriUtils.encodePathSegment(
                        name,
                        java.nio.charset.StandardCharsets.UTF_8
                );
    }

    @PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
    @GetMapping("/admin/game-object/by-name/{gameObjectName}")
    public String adminParameterValueByName(
            @PathVariable String gameObjectName,
            Model model
    ) {
        var comparison = spreadSheetService.getGameObjectComparison(gameObjectName);
        var primaryGameObject = gameObjectRepository.findByName(gameObjectName);
        model.addAttribute("gameObject", comparison);
        model.addAttribute("tags", tagRepository.findAll());
        model.addAttribute(
                "gameObjectTags",
                primaryGameObject.map(GameObject::getGameObjectTags).orElseGet(java.util.List::of)
        );
        model.addAttribute("primaryGameObjectId", primaryGameObject.map(GameObject::getId).orElse(null));
        model.addAttribute("secondaryDatabaseEnabled", parameterService.hasSecondaryDatabase());
        model.addAttribute("parameters", parameterRepository.findAllByOrderByNameAsc());
        return "admin-parameter-value";
    }

    @PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
    @GetMapping("/admin/parameter")
    public String adminParameter(
            Model model
    ) {
        model.addAttribute("parameters", parameterService.getParameterComparisons());
        model.addAttribute("secondaryDatabaseEnabled", parameterService.hasSecondaryDatabase());
        return "admin-parameter";
    }

    @PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
    @GetMapping("/admin/invalidate-cache")
    public String invalidateCache(RedirectAttributes redirectAttributes) {
        int failed = serverService.invalidateGameServerCaches();
        if (failed > 0) {
            redirectAttributes.addFlashAttribute("invalidateCacheError", failed);
        }
        return "redirect:/";
    }
}
