package com.wordonline.admin.dto.statistic;

import java.time.LocalDateTime;

/**
 * 최근 게임 목록의 한 행. 게임 정보와 그 게임의 {@code Frame} 타이밍을 함께 담는다.
 * <p>
 * 프레임 관련 필드가 nullable인 것은 left join이기 때문이다. 프레임 간격 수집이 생기기 전에 기록된
 * 게임이나 통계가 저장되지 않은 게임도 목록에서 사라지지 않고 값 없이 표시된다.
 */
public record GameFrameSummaryDto(
        long gameId,
        LocalDateTime createdAt,
        String gameType,
        long durationSeconds,
        Double frameMeanIntervalNs,
        Long frameMaxIntervalNs
) {

    public boolean hasFrameData() {
        return frameMeanIntervalNs != null;
    }

    public Double frameMeanIntervalMs() {
        return frameMeanIntervalNs == null ? null : frameMeanIntervalNs / 1_000_000.0;
    }

    public Double frameMaxIntervalMs() {
        return frameMaxIntervalNs == null ? null : frameMaxIntervalNs / 1_000_000.0;
    }
}
