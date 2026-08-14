package com.wordonline.admin.dto.statistic;

import java.time.LocalDateTime;

/**
 * 시계열의 한 점. 게임 하나가 아니라 <b>시간 구간 하나</b>를 나타낸다.
 * <p>
 * 게임마다 점을 하나씩 찍으면 1년 범위에서 15,000점이 넘어가 페이지가 900KB에 달하고 차트가
 * 사실상 멈춘다. 구간별로 묶고 그 구간에 속한 게임별 평균들의 중앙값을 쓰면, 추이를 읽는 목적은
 * 그대로 두면서 점 개수가 범위와 무관하게 수십 개로 유지된다. 개별 게임 값은 아래 최근 게임
 * 표에서 볼 수 있다.
 *
 * @param bucketStart          구간의 시작 시각
 * @param medianMeanIntervalNs 구간에 속한 게임별 평균들의 중앙값
 * @param gameCount            구간에 속한 게임 수
 */
public record TimeSeriesPointDto(
        LocalDateTime bucketStart,
        double medianMeanIntervalNs,
        long gameCount
) {

    public double medianMeanIntervalMs() {
        return medianMeanIntervalNs / 1_000_000.0;
    }
}
