package com.wordonline.admin.controller;

import com.wordonline.admin.dto.counter.CounterRuleForm;
import com.wordonline.admin.service.CounterRuleDualDatabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.function.Supplier;

@Controller
@RequestMapping("/admin/counter-rule")
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
@RequiredArgsConstructor
public class CounterRuleAdminController {

    private final CounterRuleDualDatabaseService counterRuleService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rules", counterRuleService.comparisons());
        model.addAttribute("tagNames", counterRuleService.tagNames());
        model.addAttribute("magics", counterRuleService.magicTagComparisons());
        model.addAttribute("untaggedPrimaryMagics", counterRuleService.untaggedPrimaryMagics());
        model.addAttribute("untaggedSecondaryMagics", counterRuleService.untaggedSecondaryMagics());
        model.addAttribute("secondaryDatabaseEnabled", counterRuleService.hasSecondary());
        model.addAttribute("counterRuleForm", new CounterRuleForm());
        return "admin-counter-rule";
    }

    @PostMapping
    public String create(@ModelAttribute CounterRuleForm form,
                         @RequestParam(defaultValue = "primary") String db,
                         RedirectAttributes redirectAttributes) {
        counterRuleService.create(form, counterRuleService.target(db));
        redirectAttributes.addFlashAttribute("message", "Counter rule created");
        return "redirect:/admin/counter-rule";
    }

    @PostMapping("/weight")
    public String updateWeight(@ModelAttribute CounterRuleForm form,
                               @RequestParam(defaultValue = "primary") String db,
                               RedirectAttributes redirectAttributes) {
        counterRuleService.updateWeight(form, counterRuleService.target(db));
        redirectAttributes.addFlashAttribute("message", "Counter rule weight updated");
        return "redirect:/admin/counter-rule";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam String attackerTagName,
                         @RequestParam String targetTagName,
                         @RequestParam(defaultValue = "primary") String db,
                         RedirectAttributes redirectAttributes) {
        counterRuleService.delete(attackerTagName, targetTagName, counterRuleService.target(db));
        redirectAttributes.addFlashAttribute("message", "Counter rule deleted");
        return "redirect:/admin/counter-rule";
    }

    @PostMapping("/sync-to-secondary")
    public ResponseEntity<String> syncToSecondary() {
        return respond(() -> counterRuleService.syncToSecondary().message("Counter rules: Deploy -> Dev"));
    }

    @PostMapping("/sync-to-primary")
    public ResponseEntity<String> syncToPrimary() {
        return respond(() -> counterRuleService.syncToPrimary().message("Counter rules: Dev -> Deploy"));
    }

    @PostMapping("/sync-magic-tags")
    public ResponseEntity<String> syncMagicTags(@RequestParam(defaultValue = "primary") String db) {
        return respond(() -> counterRuleService.syncMagicTags(counterRuleService.target(db)).message());
    }

    // The page-rendering endpoints redirect with a readable message; the sync endpoints are called
    // by fetch() and must answer with a failing status, so a redirecting handler cannot cover them.
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String reportInvalidRequest(RuntimeException exception, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", exception.getMessage());
        return "redirect:/admin/counter-rule";
    }

    private ResponseEntity<String> respond(Supplier<String> action) {
        try {
            return ResponseEntity.ok(action.get());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }
}
