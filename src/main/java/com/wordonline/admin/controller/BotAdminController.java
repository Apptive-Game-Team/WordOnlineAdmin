package com.wordonline.admin.controller;

import com.wordonline.admin.dto.bot.BotDeckForm;
import com.wordonline.admin.dto.bot.BotForm;
import com.wordonline.admin.repository.magic.CardRepository;
import com.wordonline.admin.service.BotAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/bot")
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
@RequiredArgsConstructor
public class BotAdminController {

    private final BotAdminService botAdminService;
    private final CardRepository cardRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("bots", botAdminService.findAll());
        model.addAttribute("botForm", new BotForm());
        return "admin-bot";
    }

    @GetMapping("/{userId}")
    public String detail(@PathVariable long userId, Model model) {
        model.addAttribute("bot", botAdminService.find(userId));
        model.addAttribute("cards", cardRepository.findAll());
        model.addAttribute("botForm", new BotForm());
        model.addAttribute("deckForm", new BotDeckForm());
        return "admin-bot-detail";
    }

    @PostMapping
    public String create(@ModelAttribute BotForm form, RedirectAttributes redirectAttributes) {
        long userId = botAdminService.create(form);
        redirectAttributes.addFlashAttribute("message", "Bot created");
        return "redirect:/admin/bot/" + userId;
    }

    @PostMapping("/{userId}")
    public String update(@PathVariable long userId, @ModelAttribute BotForm form,
                         RedirectAttributes redirectAttributes) {
        botAdminService.update(userId, form);
        redirectAttributes.addFlashAttribute("message", "Bot updated");
        return "redirect:/admin/bot/" + userId;
    }

    @PostMapping("/{userId}/deck")
    public String updateDeck(@PathVariable long userId, @ModelAttribute BotDeckForm form,
                             RedirectAttributes redirectAttributes) {
        botAdminService.replaceDeck(userId, form);
        redirectAttributes.addFlashAttribute("message", "Deck updated");
        return "redirect:/admin/bot/" + userId;
    }

    @PostMapping("/{userId}/enabled/{enabled}")
    public String setEnabled(@PathVariable long userId, @PathVariable boolean enabled) {
        botAdminService.setEnabled(userId, enabled);
        return "redirect:/admin/bot/" + userId;
    }

    @PostMapping("/{userId}/delete")
    public String delete(@PathVariable long userId) {
        botAdminService.delete(userId);
        return "redirect:/admin/bot";
    }
}
