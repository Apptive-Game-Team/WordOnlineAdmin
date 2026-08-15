package com.wordonline.admin.dto.statistic;

import java.time.LocalDateTime;

/**
 * 한 구간에 끝난 게임 수와 그것을 시간당으로 환산한 값.
 * <p>
 * 구간 길이가 조회 범위에 따라 달라지므로(시간/일/주) 개수만으로는 범위를 바꿔가며 비교할 수 없다.
 * 시간당으로 환산하면 어느 범위에서도 같은 축으로 읽힌다.
 * <p>
 * {@code partial}은 구간이 조회 범위에 잘렸다는 뜻이다. 진행 중인 마지막 구간이 대표적이다. 표시하지
 * 않으면 "지금 트래픽이 급감했다"로 잘못 읽힌다.
 */
public record SessionRatePointDto(
        LocalDateTime bucketStart,
        long gameCount,
        double gamesPerHour,
        boolean partial
) {
}
