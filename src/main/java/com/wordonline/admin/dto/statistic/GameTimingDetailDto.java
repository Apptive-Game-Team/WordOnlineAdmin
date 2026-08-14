package com.wordonline.admin.dto.statistic;

/**
 * 게임 상세 화면에 표시되는 {@code statistic_update_time} 한 행.
 */
public record GameTimingDetailDto(
        String name,
        long minIntervalNs,
        long maxIntervalNs,
        double meanIntervalNs
) {

    public double minIntervalMs() {
        return minIntervalNs / 1_000_000.0;
    }

    public double maxIntervalMs() {
        return maxIntervalNs / 1_000_000.0;
    }

    public double meanIntervalMs() {
        return meanIntervalNs / 1_000_000.0;
    }
}
