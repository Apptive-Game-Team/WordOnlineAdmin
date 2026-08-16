package com.wordonline.admin.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.wordonline.admin.dto.statistic.SystemTimingDto;
import com.wordonline.admin.dto.statistic.StatisticDataSource;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticPerformanceServiceTest {

    @Mock
    private StatisticUpdateTimeRepository repository;

    private final LocalDateTime from = LocalDateTime.of(2026, 8, 7, 0, 0);

    private StatisticPerformanceService service() {
        return new StatisticPerformanceService(repository);
    }

    private SystemTimingDto timing(String name, double median) {
        return new SystemTimingDto(name, 1_000_000L, 9_000_000L, median, median * 1.2, 5,
                null, null, 0, 0);
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

    /**
     * 게임 서버가 프레임 간격을 기록하기 전에 끝난 게임만 범위에 들어오면 {@code Frame} 행이 없다.
     * 그때 가장 느린 시스템 하나를 뽑으면 전체 부하를 대표하지 못하는 값이 기본 지표처럼 보인다.
     */
    @Test
    void fallsBackToTheCombinedRowWhenNoFrameRowExists() {
        List<SystemTimingDto> timings = List.of(
                timing(SystemTimingDto.COMBINED_SYSTEMS_NAME, 4e6),
                timing("PhysicSystem", 2e6));

        assertEquals(SystemTimingDto.COMBINED_SYSTEMS_NAME, service().selectName(null, timings));
    }

    @Test
    void fallsBackToTheFirstNameWhenNeitherFrameNorTheCombinedRowExists() {
        List<SystemTimingDto> timings = List.of(timing("PhysicSystem", 2e6), timing("RenderSystem", 1e6));

        assertEquals("PhysicSystem", service().selectName(null, timings));
    }

    /**
     * 합계는 저장된 이름이 아니라 조회 때 만들어지는 행이므로 이름별 시계열 쿼리로는 한 점도 나오지
     * 않는다. 조용히 빈 차트가 되는 것이 가장 그럴듯한 실패다.
     */
    @Test
    void routesTheCombinedSeriesToItsOwnQuery() {
        service().findTimeSeries(StatisticDataSource.PRIMARY,
                SystemTimingDto.COMBINED_SYSTEMS_NAME, GameType.PVP, from, 7);

        verify(repository).findCombinedTimeSeries(
                eq(StatisticDataSource.PRIMARY), eq(GameType.PVP), any(), eq("day"));
        verify(repository, never()).findTimeSeries(any(), any(), any(), any(), any());
    }

    /**
     * 두 값은 따로 보면 반쪽이다. {@code Frame}은 예산을 넘겼는지만, 합계는 남은 여유만 말한다.
     */
    @Test
    void pairsFrameWithTheCombinedRowOnTheSameChart() {
        List<SystemTimingDto> timings = List.of(
                timing("Frame", 5e7),
                timing(SystemTimingDto.COMBINED_SYSTEMS_NAME, 4e6),
                timing("PhysicSystem", 2e6));

        assertEquals(SystemTimingDto.COMBINED_SYSTEMS_NAME, service().companionName("Frame", timings));
        assertEquals("Frame", service().companionName(SystemTimingDto.COMBINED_SYSTEMS_NAME, timings));
        // 시스템 하나를 골라 봐도 비교 대상은 전체 부하다.
        assertEquals(SystemTimingDto.COMBINED_SYSTEMS_NAME, service().companionName("PhysicSystem", timings));
    }

    @Test
    void hasNoCompanionWhenTheCounterpartRowIsMissing() {
        List<SystemTimingDto> onlySystems = List.of(timing("PhysicSystem", 2e6));

        assertNull(service().companionName("PhysicSystem", onlySystems));
        assertNull(service().companionName(null, onlySystems));
        assertNull(service().companionName("Frame", List.of(timing("Frame", 5e7))));
    }

    @Test
    void putsTheCombinedRowIntoTheTimingsInSlowestFirstOrder() {
        LocalDateTime now = from.plusDays(2);
        SystemTimingDto combined = timing(SystemTimingDto.COMBINED_SYSTEMS_NAME, 4e6);
        when(repository.findSystemTimings(any(), any(), any(), any()))
                .thenReturn(List.of(timing("PhysicSystem", 2e6), timing("RenderSystem", 1e6)));
        when(repository.findCombinedSystemTiming(any(), any(), any(), any())).thenReturn(Optional.of(combined));

        List<SystemTimingDto> timings = service()
                .findSystemTimings(StatisticDataSource.PRIMARY, null, from, now);

        assertEquals(
                List.of(SystemTimingDto.COMBINED_SYSTEMS_NAME, "PhysicSystem", "RenderSystem"),
                timings.stream().map(SystemTimingDto::name).toList());
    }

    @Test
    void leavesTheTimingsAloneWhenThereIsNoCombinedRow() {
        List<SystemTimingDto> stored = List.of(timing("PhysicSystem", 2e6));
        when(repository.findSystemTimings(any(), any(), any(), any())).thenReturn(stored);
        when(repository.findCombinedSystemTiming(any(), any(), any(), any())).thenReturn(Optional.empty());

        assertEquals(stored, service().findSystemTimings(StatisticDataSource.PRIMARY, null, from, from.plusDays(2)));
    }

    /** 전·후반을 가르는 지점은 이름별 집계와 합계가 같아야 두 행의 추세를 나란히 읽을 수 있다. */
    @Test
    void splitsBothAggregatesAtTheSameMidpoint() {
        LocalDateTime now = from.plusDays(2);
        when(repository.findSystemTimings(any(), any(), any(), any())).thenReturn(List.of());
        when(repository.findCombinedSystemTiming(any(), any(), any(), any())).thenReturn(Optional.empty());

        service().findSystemTimings(StatisticDataSource.PRIMARY, null, from, now);

        LocalDateTime midpoint = from.plusDays(1);
        verify(repository).findSystemTimings(any(), any(), eq(from), eq(midpoint));
        verify(repository).findCombinedSystemTiming(any(), any(), eq(from), eq(midpoint));
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
        service().findRecentGames(StatisticDataSource.PRIMARY, GameType.PVP, from, 0, 10_000);

        verify(repository).findRecentGames(eq(StatisticDataSource.PRIMARY), eq(GameType.PVP), any(), eq(0), eq(200));
    }

    @Test
    void treatsANonPositivePageSizeAsTheDefault() {
        service().findRecentGames(StatisticDataSource.PRIMARY, null, from, 0, 0);

        verify(repository).findRecentGames(eq(StatisticDataSource.PRIMARY), eq(null), any(), eq(0), eq(20));
    }

    @Test
    void neverRequestsANegativePage() {
        service().findRecentGames(StatisticDataSource.PRIMARY, null, from, -3, 20);

        verify(repository).findRecentGames(eq(StatisticDataSource.PRIMARY), eq(null), any(), eq(0), eq(20));
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
        service().findTimeSeries(StatisticDataSource.PRIMARY, "Frame", GameType.PVP, from, 365);

        verify(repository).findTimeSeries(eq(StatisticDataSource.PRIMARY), eq("Frame"), eq(GameType.PVP), any(), eq("week"));
    }

    @Test
    void totalPagesRoundsUpAndHandlesAnEmptyResult() {
        assertEquals(3, service().totalPages(45, 20));
        assertEquals(2, service().totalPages(40, 20));
        assertEquals(0, service().totalPages(0, 20));
        assertEquals(1, service().totalPages(1, 20));
    }
}
