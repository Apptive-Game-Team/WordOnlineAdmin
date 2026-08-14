package com.wordonline.admin.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordonline.admin.dto.statistic.GameFrameSummaryDto;
import com.wordonline.admin.dto.statistic.GameTimingDetailDto;
import com.wordonline.admin.dto.statistic.SystemTimingDto;
import com.wordonline.admin.dto.statistic.TimeSeriesPointDto;
import com.wordonline.admin.entity.statistic.GameType;
import com.wordonline.admin.service.StatisticPerformanceService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StatisticPerformanceControllerTest {

    @Mock
    private StatisticPerformanceService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private StatisticPerformanceController controller() {
        return new StatisticPerformanceController(service, objectMapper);
    }

    private final SystemTimingDto frame =
            new SystemTimingDto("Frame", 5_000_000L, 90_000_000L, 30_000_000.0, 48_000_000.0, 5,
                    40_000_000.0, 56_000_000.0, 12, 9);

    private Model render(String gameType, Integer days, String name, Integer page) {
        Model model = new ExtendedModelMap();
        String view = controller().getPerformance(gameType, days, name, page, model);
        assertEquals("admin-statistics-performance", view);
        return model;
    }

    @Test
    void passesTheParsedGameTypeAndDateRangeToTheService() {
        when(service.findSystemTimings(any(), any(), any())).thenReturn(List.of(frame));
        when(service.selectName(any(), any())).thenReturn("Frame");

        render("PVP", 30, null, null);

        ArgumentCaptor<LocalDateTime> fromDate = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(service).findSystemTimings(org.mockito.ArgumentMatchers.eq(GameType.PVP), fromDate.capture(), any());
        // 30일 필터이므로 기준 시각은 대략 30일 전이어야 한다.
        long days = java.time.Duration.between(fromDate.getValue(), LocalDateTime.now()).toDays();
        assertEquals(30, days);
    }

    /**
     * {@code GameType}은 {@code PVP}와 {@code Practice}로 표기가 섞여 있다. 대소문자를 무시하지 않고
     * {@code valueOf(name.toUpperCase())}로 파싱하면 Practice 필터가 조용히 전체 조회로 바뀐다.
     */
    @Test
    void parsesPracticeDespiteItsMixedCaseEnumName() {
        when(service.findSystemTimings(any(), any(), any())).thenReturn(List.of());

        render("Practice", 7, null, null);
        verify(service).findSystemTimings(org.mockito.ArgumentMatchers.eq(GameType.Practice), any(), any());

        render("practice", 7, null, null);
        verify(service, org.mockito.Mockito.times(2))
                .findSystemTimings(org.mockito.ArgumentMatchers.eq(GameType.Practice), any(), any());
    }

    @Test
    void treatsAllAndUnknownValuesAsNoGameTypeFilter() {
        when(service.findSystemTimings(any(), any(), any())).thenReturn(List.of());

        render("ALL", 7, null, null);
        render("nonsense", 7, null, null);
        render(null, 7, null, null);

        verify(service, org.mockito.Mockito.times(3))
                .findSystemTimings(org.mockito.ArgumentMatchers.eq(null), any(), any());
    }

    @Test
    void clampsTheDayRangeAndDefaultsIt() {
        when(service.findSystemTimings(any(), any(), any())).thenReturn(List.of());

        assertEquals(7, render(null, null, null, null).getAttribute("selectedDays"));
        assertEquals(7, render(null, 0, null, null).getAttribute("selectedDays"));
        assertEquals(7, render(null, -5, null, null).getAttribute("selectedDays"));
        assertEquals(365, render(null, 10_000, null, null).getAttribute("selectedDays"));
    }

    @Test
    void putsTheTimingsSummaryAndPagingIntoTheModel() {
        GameFrameSummaryDto game = new GameFrameSummaryDto(
                7L, LocalDateTime.of(2026, 8, 3, 9, 30), "PVP", 300L, 30_000_000.0, 90_000_000L);
        when(service.findSystemTimings(any(), any(), any())).thenReturn(List.of(frame));
        when(service.selectName(any(), any())).thenReturn("Frame");
        when(service.frameTiming(any())).thenReturn(frame);
        when(service.findRecentGames(any(), any(), anyInt(), anyInt())).thenReturn(List.of(game));
        when(service.countRecentGames(any(), any())).thenReturn(45L);
        when(service.totalPages(anyLong(), anyInt())).thenReturn(3);

        Model model = render("PVP", 7, null, 1);

        assertEquals(List.of(frame), model.getAttribute("timings"));
        assertEquals(frame, model.getAttribute("frameTiming"));
        assertEquals(List.of(game), model.getAttribute("games"));
        assertEquals(45L, model.getAttribute("totalCount"));
        assertEquals(1, model.getAttribute("page"));
        assertEquals(3, model.getAttribute("totalPages"));
        assertEquals("PVP", model.getAttribute("selectedGameType"));
        assertEquals("PVP", model.getAttribute("gameTypeForUrl"));
        assertEquals(50.0, model.getAttribute("frameBudgetMs"));
    }

    @Test
    void gameTypeForUrlIsNullForAllSoTheLinkOmitsIt() {
        when(service.findSystemTimings(any(), any(), any())).thenReturn(List.of());

        assertNull(render("ALL", 7, null, null).getAttribute("gameTypeForUrl"));
        assertNull(render(null, 7, null, null).getAttribute("gameTypeForUrl"));
    }

    @Test
    void serialisesChartDataAsMillisecondsForTheBrowser() {
        when(service.findSystemTimings(any(), any(), any())).thenReturn(List.of(frame));
        when(service.selectName(any(), any())).thenReturn("Frame");
        when(service.findTimeSeries(any(), any(), any(), anyInt())).thenReturn(List.of(
                new TimeSeriesPointDto(LocalDateTime.of(2026, 8, 2, 10, 0), 20_000_000.0, 3L)));

        Model model = render(null, 7, null, null);

        String timingsJson = (String) model.getAttribute("timingsJson");
        String seriesJson = (String) model.getAttribute("seriesJson");
        assertTrue(timingsJson.contains("\"median\":30.0"), timingsJson);
        assertTrue(timingsJson.contains("\"p95\":48.0"), timingsJson);
        assertTrue(seriesJson.contains("\"median\":20.0"), seriesJson);
        assertTrue(seriesJson.contains("\"games\":3"), seriesJson);
        assertTrue(seriesJson.contains("2026-08-02T10:00"), seriesJson);
    }

    /** {@code th:utext}로 나가므로 이름에 {@code <}가 들어가도 script 요소가 먼저 닫히면 안 된다. */
    @Test
    void escapesAngleBracketsSoAMeasuredNameCannotCloseTheScriptElement() {
        when(service.findSystemTimings(any(), any(), any())).thenReturn(List.of(
                new SystemTimingDto("</script><script>x", 1L, 2L, 1.0, 2.0, 1, null, null, 0, 0)));
        when(service.selectName(any(), any())).thenReturn(null);

        String timingsJson = (String) render(null, 7, null, null).getAttribute("timingsJson");

        assertTrue(timingsJson.contains("\\u003C"), timingsJson);
        assertTrue(!timingsJson.contains("</script>"), timingsJson);
    }

    @Test
    void skipsTheSeriesQueryEntirelyWhenThereIsNoNameToPlot() {
        when(service.findSystemTimings(any(), any(), any())).thenReturn(List.of());
        when(service.selectName(any(), any())).thenReturn(null);

        Model model = render(null, 7, null, null);

        verify(service, org.mockito.Mockito.never()).findTimeSeries(any(), any(), any(), anyInt());
        assertEquals("[]", model.getAttribute("seriesJson"));
    }

    @Test
    void gameDetailExposesTheGameAndItsTimingRows() {
        GameFrameSummaryDto game = new GameFrameSummaryDto(
                7L, LocalDateTime.of(2026, 8, 3, 9, 30), "PVP", 300L, 30_000_000.0, 90_000_000L);
        GameTimingDetailDto timing = new GameTimingDetailDto("Frame", 5_000_000L, 90_000_000L, 30_000_000.0);
        when(service.findGame(7L)).thenReturn(Optional.of(game));
        when(service.findGameTimings(7L)).thenReturn(List.of(timing));

        Model model = new ExtendedModelMap();
        assertEquals("admin-statistics-game", controller().getGameDetail(7L, model));

        assertEquals(game, model.getAttribute("game"));
        assertEquals(List.of(timing), model.getAttribute("timings"));
    }

    @Test
    void gameDetailRendersTheNotFoundStateWithoutQueryingTimings() {
        when(service.findGame(anyLong())).thenReturn(Optional.empty());

        Model model = new ExtendedModelMap();
        assertEquals("admin-statistics-game", controller().getGameDetail(404L, model));

        assertNull(model.getAttribute("game"));
        assertEquals(List.of(), model.getAttribute("timings"));
        verify(service, org.mockito.Mockito.never()).findGameTimings(anyLong());
    }
}
