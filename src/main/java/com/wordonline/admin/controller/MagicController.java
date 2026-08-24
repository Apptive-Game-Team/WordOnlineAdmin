package com.wordonline.admin.controller;

import com.wordonline.admin.entity.magic.MagicAccessType;
import com.wordonline.admin.entity.magic.MagicCastType;
import com.wordonline.admin.service.MagicService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class MagicController {

    private final MagicService magicService;

    @GetMapping("/admin/magic")
    public String adminMagic(
            Model model
    ) {
        model.addAttribute("magics", magicService.getMagicComparisons());
        model.addAttribute("primaryCardNames", magicService.getCardNames(false));
        model.addAttribute(
                "secondaryCardNames",
                magicService.hasSecondaryDatabase() ? magicService.getCardNames(true) : List.of()
        );
        model.addAttribute("secondaryDatabaseEnabled", magicService.hasSecondaryDatabase());
        model.addAttribute("castTypes", MagicCastType.storedValues());
        model.addAttribute("knownAccessTypes", MagicAccessType.knownValues());
        model.addAttribute("defaultAccessType", MagicAccessType.DEFAULT);
        return "admin-magic";
    }

    @PostMapping("/admin/magic/by-name/create")
    public String createMagicByName(
            @RequestParam String name,
            @RequestParam(required = false) String castType,
            @RequestParam(required = false) String accessType,
            @RequestParam String db
    ) {
        magicService.createMagic(name, castType, accessType, isSecondary(db));
        return redirect();
    }

    @PostMapping("/admin/magic/by-name/update")
    public String updateMagicByName(
            @RequestParam String currentName,
            @RequestParam String name,
            @RequestParam(required = false) String castType,
            @RequestParam(required = false) String accessType,
            @RequestParam String db
    ) {
        magicService.updateMagic(currentName, name, castType, accessType, isSecondary(db));
        return redirect();
    }

    @PostMapping("/admin/magic/by-name/delete")
    public String deleteMagicByName(
            @RequestParam String name,
            @RequestParam String db
    ) {
        magicService.removeMagic(name, isSecondary(db));
        return redirect();
    }

    @PostMapping("/admin/magic/by-name/add-card")
    public String addCardToMagicByName(
            @RequestParam String magicName,
            @RequestParam String cardName,
            @RequestParam String db
    ) {
        magicService.addCardToMagic(magicName, cardName, isSecondary(db));
        return redirect();
    }

    @PostMapping("/admin/magic/by-name/remove-card")
    public String removeCardFromMagicByName(
            @RequestParam String magicName,
            @RequestParam String cardName,
            @RequestParam String db
    ) {
        magicService.removeCardFromMagic(magicName, cardName, isSecondary(db));
        return redirect();
    }

    @PostMapping("/admin/magic/by-name/bulk-delete")
    public String bulkDeleteByName(
            @RequestParam(required = false) List<String> magicNames,
            @RequestParam String db
    ) {
        if (magicNames != null) {
            for (String magicName : magicNames) {
                magicService.removeMagic(magicName, isSecondary(db));
            }
        }
        return redirect();
    }

    @PostMapping("/admin/magic/by-name/bulk-add-card")
    public String bulkAddCardByName(
            @RequestParam(required = false) List<String> magicNames,
            @RequestParam String cardName,
            @RequestParam String db
    ) {
        if (magicNames != null) {
            for (String magicName : magicNames) {
                magicService.addCardToMagic(magicName, cardName, isSecondary(db));
            }
        }
        return redirect();
    }

    @PostMapping("/admin/magic/create")
    public String createMagic(
            @RequestParam String name,
            @RequestParam(required = false) String castType,
            @RequestParam(required = false) String accessType,
            @RequestParam(value = "db", defaultValue = "primary") String db
    ) {
        magicService.createMagic(name, castType, accessType, isSecondary(db));
        return redirect(db);
    }

    @PostMapping("/admin/magic/update")
    public String updateMagic(
            @RequestParam long id,
            @RequestParam String name,
            @RequestParam(required = false) String castType,
            @RequestParam(required = false) String accessType,
            @RequestParam(value = "db", defaultValue = "primary") String db
    ) {
        magicService.updateMagic(id, name, castType, accessType, isSecondary(db));
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
        return respond(() -> magicService.syncToSecondary().toMessage("Magic: Deploy -> Dev"));
    }

    @PostMapping("/admin/magic/sync-to-primary")
    public org.springframework.http.ResponseEntity<String> syncToPrimary() {
        return respond(() -> magicService.syncToPrimary().toMessage("Magic: Dev -> Deploy"));
    }

    // These two are called by fetch(), so they must answer with a status and a body. The redirecting
    // handler below would send them the page instead.
    private org.springframework.http.ResponseEntity<String> respond(java.util.function.Supplier<String> sync) {
        try {
            return org.springframework.http.ResponseEntity.ok(sync.get());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return org.springframework.http.ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    // The form endpoints redirect, so a rejected cast_type or access_type has to come back as a
    // flash message; the sync endpoints answer fetch() and keep their own bodies.
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String reportInvalidRequest(RuntimeException exception, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", exception.getMessage());
        return redirect();
    }

    private boolean isSecondary(String db) {
        return "secondary".equalsIgnoreCase(db);
    }

    private String redirect(String db) {
        return "redirect:/admin/magic?db=" + (isSecondary(db) ? "secondary" : "primary");
    }

    private String redirect() {
        return "redirect:/admin/magic";
    }
}
