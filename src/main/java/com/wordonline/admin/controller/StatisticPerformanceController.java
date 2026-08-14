package com.wordonline.admin.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordonline.admin.dto.statistic.GameFrameSummaryDto;
import com.wordonline.admin.dto.statistic.SystemTimingDto;
import com.wordonline.admin.dto.statistic.TimeSeriesPointDto;
import com.wordonline.admin.entity.statistic.GameType;
import com.wordonline.admin.service.StatisticPerformanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 프레임 타이밍 통계 페이지.
 * <p>
 * 필터의 형태({@code gameType}, {@code days})는 기존 {@link StatisticController}와 맞췄다. 두 페이지를
 * 오가며 같은 조건을 다시 이해할 필요가 없게 하기 위해서다.
 */
@Controller
@RequestMapping("/admin/statistics/performance")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('WORDONLINE_ADMIN')")
@Slf4j
public class StatisticPerformanceController {

    private static final int DEFAULT_DAYS = 7;
    private static final int MAX_DAYS = 365;

    private final StatisticPerformanceService statisticPerformanceService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public String getPerformance(
            @RequestParam(required = false) String gameType,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer page,
            Model model) {

        GameType type = parseGameType(gameType);
        int daysFilter = parseDays(days);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fromDate = now.minusDays(daysFilter);
        int pageIndex = page == null || page < 0 ? 0 : page;

        List<SystemTimingDto> timings = statisticPerformanceService.findSystemTimings(type, fromDate, now);
        String selectedName = statisticPerformanceService.selectName(name, timings);
        List<TimeSeriesPointDto> series = selectedName == null
                ? List.of()
                : statisticPerformanceService.findTimeSeries(selectedName, type, fromDate, daysFilter);
        List<GameFrameSummaryDto> games = statisticPerformanceService.findRecentGames(
                type, fromDate, pageIndex, StatisticPerformanceService.DEFAULT_PAGE_SIZE);
        long totalCount = statisticPerformanceService.countRecentGames(type, fromDate);

        model.addAttribute("selectedGameType", gameType != null ? gameType : "ALL");
        model.addAttribute("gameTypeForUrl", gameTypeForUrl(gameType));
        model.addAttribute("selectedDays", daysFilter);
        model.addAttribute("timings", timings);
        model.addAttribute("selectedName", selectedName);
        model.addAttribute("frameTiming", statisticPerformanceService.frameTiming(timings));
        model.addAttribute("frameBudgetMs", StatisticPerformanceService.FRAME_BUDGET_NS / 1_000_000.0);
        model.addAttribute("games", games);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("page", pageIndex);
        model.addAttribute("totalPages", statisticPerformanceService.totalPages(
                totalCount, StatisticPerformanceService.DEFAULT_PAGE_SIZE));
        model.addAttribute("bucketLabel", statisticPerformanceService.bucketLabel(daysFilter));
        model.addAttribute("timingsJson", toJson(timings.stream().map(ChartPoint::of).toList()));
        model.addAttribute("seriesJson", toJson(series.stream().map(SeriesPoint::of).toList()));
        return "admin-statistics-performance";
    }

    @GetMapping("/games/{gameId}")
    public String getGameDetail(@PathVariable long gameId, Model model) {
        GameFrameSummaryDto game = statisticPerformanceService.findGame(gameId).orElse(null);
        model.addAttribute("game", game);
        model.addAttribute("timings", game == null
                ? List.of()
                : statisticPerformanceService.findGameTimings(gameId));
        model.addAttribute("frameBudgetMs", StatisticPerformanceService.FRAME_BUDGET_NS / 1_000_000.0);
        return "admin-statistics-game";
    }

    /** 차트에 넘기는 값은 밀리초로 변환해 둔다. 화면에서 나노초를 다룰 이유가 없다. */
    private record ChartPoint(String name, double median, double p95) {
        static ChartPoint of(SystemTimingDto timing) {
            return new ChartPoint(timing.name(), timing.medianMeanIntervalMs(), timing.p95MeanIntervalMs());
        }
    }

    private record SeriesPoint(String at, double median, long games) {
        static SeriesPoint of(TimeSeriesPointDto point) {
            return new SeriesPoint(
                    point.bucketStart().toString(), point.medianMeanIntervalMs(), point.gameCount());
        }
    }

    /**
     * {@code th:utext}로 출력하므로 {@code <}가 그대로 나가면 측정 이름이 script 요소를 먼저 닫을 수
     * 있다. 유니코드 이스케이프로 바꾸면 JSON은 그대로 유효하고 요소도 깨지지 않는다.
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value).replace("<", "\\u003C");
        } catch (JsonProcessingException e) {
            // 차트는 보조 수단이다. 직렬화가 실패해도 표는 그대로 보여야 한다.
            log.warn("차트 데이터를 직렬화하지 못했습니다", e);
            return "[]";
        }
    }

    private String gameTypeForUrl(String gameType) {
        return (gameType != null && !"ALL".equalsIgnoreCase(gameType)) ? gameType : null;
    }

    private int parseDays(Integer days) {
        int daysFilter = (days != null && days > 0) ? days : DEFAULT_DAYS;
        return Math.min(daysFilter, MAX_DAYS);
    }

    /**
     * 대소문자를 가리지 않고 맞춘다. {@code GameType}은 {@code PVP}와 {@code Practice}로 표기가
     * 섞여 있어 {@code valueOf(name.toUpperCase())} 형태로는 {@code Practice}를 찾지 못한다.
     */
    private GameType parseGameType(String gameType) {
        if (gameType == null || "ALL".equalsIgnoreCase(gameType)) {
            return null;
        }
        return Arrays.stream(GameType.values())
                .filter(type -> type.name().equalsIgnoreCase(gameType))
                .findFirst()
                .orElse(null);
    }
}
