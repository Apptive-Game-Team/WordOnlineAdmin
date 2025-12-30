package com.wordonline.admin.controller;

import com.wordonline.admin.entity.magic.Card;
import com.wordonline.admin.entity.magic.Magic;
import com.wordonline.admin.entity.statistic.GameType;
import com.wordonline.admin.service.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
public class StatisticController {

    private final StatisticService statisticService;

    @GetMapping
    public String getStatistics(
            @RequestParam(required = false) String gameType,
            @RequestParam(required = false) Integer days,
            Model model) {
        
        GameType type = parseGameType(gameType);
        
        // Default to 7 days if not specified
        int daysFilter = (days != null && days > 0) ? days : 7;
        LocalDateTime fromDate = LocalDateTime.now().minusDays(daysFilter);
        
        model.addAttribute("selectedGameType", gameType != null ? gameType : "ALL");
        model.addAttribute("selectedDays", daysFilter);
        model.addAttribute("cardWinCounts", toCardNameMap(statisticService.calculateCardWinCounts(type, fromDate)));
        model.addAttribute("magicWinCounts", toMagicNameMap(statisticService.calculateMagicWinCounts(type, fromDate)));
        model.addAttribute("cardGameCounts", toCardNameMap(statisticService.calculateCardGameCounts(type, fromDate)));
        model.addAttribute("magicGameCounts", toMagicNameMap(statisticService.calculateMagicGameCounts(type, fromDate)));
        model.addAttribute("cardUseCounts", toCardNameMap(statisticService.calculateCardUseCounts(type, fromDate)));
        model.addAttribute("magicUseCounts", toMagicNameMap(statisticService.calculateMagicUseCounts(type, fromDate)));
        
        // Per-player statistics
        model.addAttribute("playerWinCounts", statisticService.calculatePlayerWinCounts(type, fromDate));
        model.addAttribute("playerCardUsage", convertPlayerCardUsage(statisticService.calculatePlayerCardUsage(type, fromDate)));
        model.addAttribute("playerMagicUsage", convertPlayerMagicUsage(statisticService.calculatePlayerMagicUsage(type, fromDate)));
        
        return "admin-statistics";
    }

    private GameType parseGameType(String gameType) {
        if (gameType == null || "ALL".equalsIgnoreCase(gameType)) {
            return null;
        }
        try {
            return GameType.valueOf(gameType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Map<String, Integer> toCardNameMap(Map<Card, Integer> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getName(), Map.Entry::getValue));
    }

    private Map<String, Integer> toMagicNameMap(Map<Magic, Integer> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getName(), Map.Entry::getValue));
    }
    
    private Map<Long, Map<String, Integer>> convertPlayerCardUsage(Map<Long, Map<Card, Integer>> playerCardUsage) {
        return playerCardUsage.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().entrySet().stream()
                                .collect(Collectors.toMap(
                                        cardEntry -> cardEntry.getKey().getName(),
                                        Map.Entry::getValue
                                ))
                ));
    }
    
    private Map<Long, Map<String, Integer>> convertPlayerMagicUsage(Map<Long, Map<Magic, Integer>> playerMagicUsage) {
        return playerMagicUsage.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().entrySet().stream()
                                .collect(Collectors.toMap(
                                        magicEntry -> magicEntry.getKey().getName(),
                                        Map.Entry::getValue
                                ))
                ));
    }
}
