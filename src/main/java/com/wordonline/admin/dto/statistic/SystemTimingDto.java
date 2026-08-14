package com.wordonline.admin.dto.statistic;

/**
 * 측정 이름 하나에 대한, 필터된 기간 전체의 집계.
 * <p>
 * 두 백분위수는 게임별 {@code mean_interval_ns} 값들의 분포다. 평균이 아닌 이유가 있다. 각 행의
 * mean은 이미 서로 다른, 그리고 저장되지 않은 프레임 수에 대한 평균이므로 그것을 다시 평균내면
 * 게임 길이와 무관하게 모든 게임에 같은 가중치를 주게 된다. 중앙값과 95 백분위수는 그런 가중치가
 * 필요 없다.
 */
public record SystemTimingDto(
        String name,
        long minIntervalNs,
        long maxIntervalNs,
        double medianMeanIntervalNs,
        double p95MeanIntervalNs,
        long gameCount
) {

    public double minIntervalMs() {
        return minIntervalNs / 1_000_000.0;
    }

    public double maxIntervalMs() {
        return maxIntervalNs / 1_000_000.0;
    }

    public double medianMeanIntervalMs() {
        return medianMeanIntervalNs / 1_000_000.0;
    }

    public double p95MeanIntervalMs() {
        return p95MeanIntervalNs / 1_000_000.0;
    }
}
