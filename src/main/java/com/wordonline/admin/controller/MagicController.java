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
    public String adminMagic(
            @RequestParam(value = "db", defaultValue = "primary") String db,
            Model model
    ) {
        boolean secondary = isSecondary(db);
        model.addAttribute("magics", magicService.getAllMagic(secondary));
        model.addAttribute("cards", magicService.getAllCards(secondary));
        model.addAttribute("selectedDb", secondary ? "secondary" : "primary");
        model.addAttribute("secondaryDatabaseEnabled", magicService.hasSecondaryDatabase());
        return "admin-magic";
    }

    @PostMapping("/admin/magic/create")
    public String createMagic(@RequestParam String name, @RequestParam(value = "db", defaultValue = "primary") String db) {
        magicService.createMagic(name, isSecondary(db));
        return redirect(db);
    }

    @PostMapping("/admin/magic/update")
    public String updateMagic(@RequestParam long id, @RequestParam String name, @RequestParam(value = "db", defaultValue = "primary") String db) {
        magicService.updateMagicName(id, name, isSecondary(db));
        return redirect(db);
    }

    @PostMapping("/admin/magic/delete")
    public String deleteMagic(@RequestParam long id, @RequestParam(value = "db", defaultValue = "primary") String db) {
        magicService.removeMagic(id, isSecondary(db));
        return redirect(db);
    }

    @PostMapping("/admin/magic/add-card")
    public String addCardToMagic(@RequestParam long magicId, @RequestParam long cardId, @RequestParam(value = "db", defaultValue = "primary") String db) {
        magicService.addCardToMagic(magicId, cardId, isSecondary(db));
        return redirect(db);
    }

    @PostMapping("/admin/magic/remove-card")
    public String removeCardFromMagic(@RequestParam long magicId, @RequestParam long cardId, @RequestParam(value = "db", defaultValue = "primary") String db) {
        magicService.removeCardFromMagic(magicId, cardId, isSecondary(db));
        return redirect(db);
    }

    @PostMapping("/admin/magic/bulk-delete")
    public String bulkDelete(
            @RequestParam(required = false) List<Long> selectedMagicIds,
            @RequestParam(value = "db", defaultValue = "primary") String db
    ) {
        if (selectedMagicIds != null) {
            for (Long magicId : selectedMagicIds) {
                magicService.removeMagic(magicId, isSecondary(db));
            }
        }
        return redirect(db);
    }

    @PostMapping("/admin/magic/bulk-add-card")
    public String bulkAddCard(
            @RequestParam(required = false) List<Long> selectedMagicIds,
            @RequestParam("bulkCardId") Long cardId,
            @RequestParam(value = "db", defaultValue = "primary") String db
    ) {
        if (selectedMagicIds != null && cardId != null) {
            for (Long magicId : selectedMagicIds) {
                magicService.addCardToMagic(magicId, cardId, isSecondary(db));
            }
        }
        return redirect(db);
    }

    @PostMapping("/admin/magic/sync-to-secondary")
    public org.springframework.http.ResponseEntity<String> syncToSecondary() {
        return org.springframework.http.ResponseEntity.ok(magicService.syncToSecondary().toMessage("Magic: Prod -> Dev"));
    }

    @PostMapping("/admin/magic/sync-to-primary")
    public org.springframework.http.ResponseEntity<String> syncToPrimary() {
        return org.springframework.http.ResponseEntity.ok(magicService.syncToPrimary().toMessage("Magic: Dev -> Prod"));
    }

    private boolean isSecondary(String db) {
        return "secondary".equalsIgnoreCase(db);
    }

    private String redirect(String db) {
        return "redirect:/admin/magic?db=" + (isSecondary(db) ? "secondary" : "primary");
    }
}
