package com.wordonline.admin.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.wordonline.admin.entity.statistic.GameSessionStatus;
import com.wordonline.admin.service.GameSessionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/game-sessions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class GameSessionController {

    private final GameSessionService gameSessionService;

    @GetMapping
    public String getGameSessions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer days,
            Model model) {

        GameSessionStatus statusFilter = parseStatus(status);

        int daysFilter = (days != null && days > 0) ? days : 7;
        if (daysFilter > 365) {
            daysFilter = 365;
        }
        Instant from = Instant.now().minus(daysFilter, ChronoUnit.DAYS);

        Map<String, Long> statusCounts = gameSessionService.countByStatus(from);

        model.addAttribute("selectedStatus", statusFilter != null ? statusFilter.name() : "ALL");
        model.addAttribute("statusForUrl", statusFilter != null ? statusFilter.name() : null);
        model.addAttribute("selectedDays", daysFilter);
        model.addAttribute("statusValues", GameSessionStatus.values());
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("sessions", gameSessionService.getSessions(statusFilter, from));

        return "admin-game-sessions";
    }

    private GameSessionStatus parseStatus(String status) {
        if (status == null || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        try {
            return GameSessionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
