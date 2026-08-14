package com.wordonline.admin.dto.statistic;

import java.time.LocalDateTime;

/**
 * 측정 이름 하나에 대한 게임별 평균. 시간에 따른 추이를 그리기 위한 점이다.
 */
public record TimeSeriesPointDto(
        long gameId,
        LocalDateTime createdAt,
        double meanIntervalNs
) {

    public double meanIntervalMs() {
        return meanIntervalNs / 1_000_000.0;
    }
}
