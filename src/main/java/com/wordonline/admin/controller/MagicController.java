package com.wordonline.admin.controller;

import com.wordonline.admin.service.MagicService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class MagicController {

    private final MagicService magicService;

    @GetMapping("/admin/magic")
    public String adminMagic(Model model) {
        model.addAttribute("magics", magicService.getAllMagic());
        model.addAttribute("cards", magicService.getAllCards());
        return "admin-magic";
    }

    @PostMapping("/admin/magic/create")
    public String createMagic(@RequestParam String name) {
        magicService.createMagic(name);
        return "redirect:/admin/magic";
    }

    @PostMapping("/admin/magic/update")
    public String updateMagic(@RequestParam long id, @RequestParam String name) {
        magicService.updateMagicName(id, name);
        return "redirect:/admin/magic";
    }

    @PostMapping("/admin/magic/delete")
    public String deleteMagic(@RequestParam long id) {
        magicService.removeMagic(id);
        return "redirect:/admin/magic";
    }

    @PostMapping("/admin/magic/add-card")
    public String addCardToMagic(@RequestParam long magicId, @RequestParam long cardId) {
        magicService.addCardToMagic(magicId, cardId);
        return "redirect:/admin/magic";
    }

    @PostMapping("/admin/magic/remove-card")
    public String removeCardFromMagic(@RequestParam long magicId, @RequestParam long cardId) {
        magicService.removeCardFromMagic(magicId, cardId);
        return "redirect:/admin/magic";
    }

    @PostMapping("/admin/magic/bulk-delete")
    public String bulkDelete(@RequestParam(required = false) List<Long> selectedMagicIds) {
        if (selectedMagicIds != null) {
            for (Long magicId : selectedMagicIds) {
                magicService.removeMagic(magicId);
            }
        }
        return "redirect:/admin/magic";
    }

    @PostMapping("/admin/magic/bulk-add-card")
    public String bulkAddCard(@RequestParam(required = false) List<Long> selectedMagicIds, @RequestParam("bulkCardId") Long cardId) {
        if (selectedMagicIds != null && cardId != null) {
            for (Long magicId : selectedMagicIds) {
                magicService.addCardToMagic(magicId, cardId);
            }
        }
        return "redirect:/admin/magic";
    }
}