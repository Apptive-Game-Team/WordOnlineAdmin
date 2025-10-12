package com.wordonline.admin.controller;

import com.wordonline.admin.entity.statistic.Card;
import com.wordonline.admin.entity.statistic.Magic;
import com.wordonline.admin.service.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class StatisticController {

    private final StatisticService statisticService;

    @GetMapping
    public String getStatistics(Model model) {
        model.addAttribute("cardWinCounts", toCardNameMap(statisticService.calculateCardWinCounts()));
        model.addAttribute("magicWinCounts", toMagicNameMap(statisticService.calculateMagicWinCounts()));
        model.addAttribute("cardGameCounts", toCardNameMap(statisticService.calculateCardGameCounts()));
        model.addAttribute("magicGameCounts", toMagicNameMap(statisticService.calculateMagicGameCounts()));
        model.addAttribute("cardUseCounts", toCardNameMap(statisticService.calculateCardUseCounts()));
        model.addAttribute("magicUseCounts", toMagicNameMap(statisticService.calculateMagicUseCounts()));
        return "admin-statistics";
    }

    private Map<String, Integer> toCardNameMap(Map<Card, Integer> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getName(), Map.Entry::getValue));
    }

    private Map<String, Integer> toMagicNameMap(Map<Magic, Integer> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getName(), Map.Entry::getValue));
    }
}
