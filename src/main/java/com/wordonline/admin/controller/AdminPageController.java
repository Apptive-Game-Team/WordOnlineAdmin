package com.wordonline.admin.controller;

import com.wordonline.admin.client.GameServerClient;
import com.wordonline.admin.entity.parameter.GameObject;
import com.wordonline.admin.repository.parameter.GameObjectRepository;
import com.wordonline.admin.repository.parameter.ParameterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class AdminPageController {

    private final GameObjectRepository gameObjectRepository;
    private final ParameterRepository parameterRepository;
    private final GameServerClient gameServerClient;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/admin/game-object")
    public String adminGameObject(Model model) {
        model.addAttribute("gameObjects", gameObjectRepository.findAll());
        return "admin-game-object";
    }

    @GetMapping("/admin/game-object/{gameObjectId}")
    public String adminParameterValue(@PathVariable Long gameObjectId, Model model) {
        GameObject gameObject = gameObjectRepository.findById(gameObjectId).orElseThrow();
        model.addAttribute("gameObject", gameObject);
        model.addAttribute("parameters", parameterRepository.findAll());
        return "admin-parameter-value";
    }

    @GetMapping("/admin/parameter")
    public String adminParameter(Model model) {
        model.addAttribute("parameters", parameterRepository.findAll());
        return "admin-parameter";
    }

    @GetMapping("/admin/invalidate-cache")
    public String invalidateCache() {
        gameServerClient.invalidateCache();
        return "redirect:/";
    }
}
