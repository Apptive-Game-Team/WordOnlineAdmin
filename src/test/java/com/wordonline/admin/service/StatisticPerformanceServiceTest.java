package com.wordonline.admin.service;

import java.time.LocalDateTime;
import java.util.List;

import com.wordonline.admin.dto.statistic.SystemTimingDto;
import com.wordonline.admin.entity.statistic.GameType;
import com.wordonline.admin.repository.statistic.StatisticUpdateTimeRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatisticPerformanceServiceTest {

    @Mock
    private StatisticUpdateTimeRepository repository;

    private final LocalDateTime from = LocalDateTime.of(2026, 8, 7, 0, 0);

    private StatisticPerformanceService service() {
        return new StatisticPerformanceService(repository);
    }

    private SystemTimingDto timing(String name, double median) {
        return new SystemTimingDto(name, 1_000_000L, 9_000_000L, median, median * 1.2, 5);
    }

    @Test
    void defaultsTheSeriesToFrameBecauseThatIsWhyThePageExists() {
        List<SystemTimingDto> timings = List.of(timing("PhysicSystem", 2e6), timing("Frame", 3e7));

        assertEquals("Frame", service().selectName(null, timings));
        assertEquals("Frame", service().selectName("  ", timings));
    }

    @Test
    void anExplicitlyRequestedNameWinsOverTheFrameDefault() {
        List<SystemTimingDto> timings = List.of(timing("Frame", 3e7), timing("PhysicSystem", 2e6));

        assertEquals("PhysicSystem", service().selectName("PhysicSystem", timings));
    }

    @Test
    void fallsBackToTheFirstNameWhenNoFrameRowExists() {
        List<SystemTimingDto> timings = List.of(timing("PhysicSystem", 2e6), timing("RenderSystem", 1e6));

        assertEquals("PhysicSystem", service().selectName(null, timings));
    }

    @Test
    void selectsNothingWhenThereIsNoDataAtAll() {
        assertNull(service().selectName(null, List.of()));
        assertNull(service().frameTiming(List.of()));
    }

    @Test
    void frameTimingPicksTheFrameRowOutOfTheAggregate() {
        SystemTimingDto frame = timing("Frame", 3e7);
        List<SystemTimingDto> timings = List.of(timing("PhysicSystem", 2e6), frame);

        assertEquals(frame, service().frameTiming(timings));
    }

    @Test
    void clampsPageSizeSoAHandEditedQueryCannotRequestTheWholeTable() {
        service().findRecentGames(GameType.PVP, from, 0, 10_000);

        verify(repository).findRecentGames(eq(GameType.PVP), any(), eq(0), eq(200));
    }

    @Test
    void treatsANonPositivePageSizeAsTheDefault() {
        service().findRecentGames(null, from, 0, 0);

        verify(repository).findRecentGames(eq(null), any(), eq(0), eq(20));
    }

    @Test
    void neverRequestsANegativePage() {
        service().findRecentGames(null, from, -3, 20);

        verify(repository).findRecentGames(eq(null), any(), eq(0), eq(20));
    }

    /**
     * 게임당 한 점을 찍으면 1년 범위에서 15,000점이 넘어 페이지가 900KB가 되고 차트가 멈춘다.
     * 구간 단위는 어느 범위에서도 점 개수가 수십 개에 머물도록 골라야 한다.
     */
    @Test
    void picksABucketThatKeepsThePointCountBounded() {
        assertEquals("hour", service().bucketUnit(1));
        assertEquals("hour", service().bucketUnit(2));
        assertEquals("day", service().bucketUnit(7));
        assertEquals("day", service().bucketUnit(30));
        assertEquals("day", service().bucketUnit(90));
        assertEquals("week", service().bucketUnit(365));
    }

    @Test
    void passesTheBucketDerivedFromTheDayRangeToTheRepository() {
        service().findTimeSeries("Frame", GameType.PVP, from, 365);

        verify(repository).findTimeSeries(eq("Frame"), eq(GameType.PVP), any(), eq("week"));
    }

    @Test
    void totalPagesRoundsUpAndHandlesAnEmptyResult() {
        assertEquals(3, service().totalPages(45, 20));
        assertEquals(2, service().totalPages(40, 20));
        assertEquals(0, service().totalPages(0, 20));
        assertEquals(1, service().totalPages(1, 20));
    }
}
