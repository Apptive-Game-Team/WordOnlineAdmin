package com.wordonline.admin.dto.statistic;

import java.time.LocalDateTime;

/**
 * 한 구간에 끝난 게임 수. 시간당 환산과 구간이 잘렸는지 여부는 구간 길이와 조회 범위를 아는
 * 서비스 계층에서 붙인다.
 */
public record SessionCountDto(
        LocalDateTime bucketStart,
        long gameCount
) {
}
