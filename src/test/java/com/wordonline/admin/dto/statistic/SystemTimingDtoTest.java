package com.wordonline.admin.dto.statistic;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 추세는 화면에서 회귀를 알아채는 유일한 단서이므로, 표본이 부족할 때 조용히 방향을 지어내지 않는
 * 것이 무엇보다 중요하다.
 */
class SystemTimingDtoTest {

    private SystemTimingDto trend(Double earlier, Double later, long earlierGames, long laterGames) {
        return new SystemTimingDto("Frame", 1L, 2L, 1.0, 2.0, earlierGames + laterGames,
                earlier, later, earlierGames, laterGames);
    }

    @Test
    void reportsGettingSlowerWhenTheLaterHalfIsWorse() {
        SystemTimingDto timing = trend(40_000_000.0, 56_000_000.0, 12, 9);

        assertTrue(timing.hasTrend());
        assertEquals(40.0, timing.trendPercent(), 0.001);
        assertEquals("worse", timing.trendDirection());
    }

    @Test
    void reportsGettingFasterWhenTheLaterHalfIsBetter() {
        SystemTimingDto timing = trend(50_000_000.0, 25_000_000.0, 10, 10);

        assertEquals(-50.0, timing.trendPercent(), 0.001);
        assertEquals("better", timing.trendDirection());
    }

    @Test
    void treatsSmallMovementAsFlatBecauseTimingsAlwaysWobble() {
        assertEquals("flat", trend(50_000_000.0, 52_000_000.0, 10, 10).trendDirection());
        assertEquals("flat", trend(50_000_000.0, 48_000_000.0, 10, 10).trendDirection());
        // 경계 바로 바깥은 방향을 말한다.
        assertEquals("worse", trend(50_000_000.0, 55_500_000.0, 10, 10).trendDirection());
    }

    @Test
    void refusesToStateADirectionWhenEitherHalfIsTooSmall() {
        assertFalse(trend(40_000_000.0, 56_000_000.0, 4, 100).hasTrend());
        assertFalse(trend(40_000_000.0, 56_000_000.0, 100, 4).hasTrend());
        assertEquals("unknown", trend(40_000_000.0, 56_000_000.0, 4, 100).trendDirection());
        assertEquals(0.0, trend(40_000_000.0, 56_000_000.0, 4, 100).trendPercent(), 0.001);
    }

    /** 한쪽 절반에 아무 게임도 없으면 백분위수가 null로 나온다. 새로 등장한 이름이 그렇다. */
    @Test
    void refusesToStateADirectionWhenAHalfHasNoRowsAtAll() {
        assertFalse(trend(null, 56_000_000.0, 0, 20).hasTrend());
        assertFalse(trend(40_000_000.0, null, 20, 0).hasTrend());
        assertEquals("unknown", trend(null, null, 0, 0).trendDirection());
    }

    /** 0으로 나누면 무한대가 나온다. 기준값이 0이면 비율을 말할 수 없다. */
    @Test
    void refusesToStateADirectionWhenTheBaselineIsZero() {
        assertFalse(trend(0.0, 56_000_000.0, 20, 20).hasTrend());
        assertEquals(0.0, trend(0.0, 56_000_000.0, 20, 20).trendPercent(), 0.001);
    }

    @Test
    void midpointSplitsTheWindowInHalfByTime() {
        var service = new com.wordonline.admin.service.StatisticPerformanceService(null);
        LocalDateTime from = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 0, 0);

        assertEquals(LocalDateTime.of(2026, 8, 6, 0, 0), service.midpoint(from, now));
    }
}
