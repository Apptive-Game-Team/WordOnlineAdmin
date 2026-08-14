package com.wordonline.admin.dto.statistic;

/**
 * 측정 이름 하나에 대한, 필터된 기간 전체의 집계와 그 기간 안에서의 추세.
 * <p>
 * 두 백분위수는 게임별 {@code mean_interval_ns} 값들의 분포다. 평균이 아닌 이유가 있다. 각 행의
 * mean은 이미 서로 다른, 그리고 저장되지 않은 프레임 수에 대한 평균이므로 그것을 다시 평균내면
 * 게임 길이와 무관하게 모든 게임에 같은 가중치를 주게 된다. 중앙값과 95 백분위수는 그런 가중치가
 * 필요 없다.
 * <p>
 * 추세는 조회 구간을 절반으로 갈라 <b>p95</b>를 비교한다. median이 아니라 p95인 이유는
 * {@code Frame} 때문이다. 루프는 남는 시간을 자므로({@code GameLoop.runLoop}) 프레임 간격은 작업량과
 * 무관하게 예산 50ms에 붙박여 있고, 예산을 넘긴 프레임만 그 위로 튄다. 즉 median 기준 추세는 이
 * 화면이 잡아야 할 바로 그 회귀에 눈이 먼다.
 *
 * @param earlierP95Ns 구간 전반부의 p95. 표본이 없으면 null
 * @param laterP95Ns   구간 후반부의 p95. 표본이 없으면 null
 */
public record SystemTimingDto(
        String name,
        long minIntervalNs,
        long maxIntervalNs,
        double medianMeanIntervalNs,
        double p95MeanIntervalNs,
        long gameCount,
        Double earlierP95Ns,
        Double laterP95Ns,
        long earlierGames,
        long laterGames
) {

    /** 양쪽 절반에 이만큼은 있어야 추세를 말한다. 게임 두세 건의 차이는 추세가 아니라 잡음이다. */
    public static final long MIN_GAMES_PER_HALF = 5;

    /** 이 미만의 변화는 방향을 말하지 않는다. 타이밍 값은 원래 이 정도로는 흔들린다. */
    public static final double FLAT_THRESHOLD_PERCENT = 10.0;

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

    /** 양쪽 절반 모두 표본이 충분하고 기준값이 0이 아닐 때만 추세를 말할 수 있다. */
    public boolean hasTrend() {
        return earlierP95Ns != null
                && laterP95Ns != null
                && earlierP95Ns > 0
                && earlierGames >= MIN_GAMES_PER_HALF
                && laterGames >= MIN_GAMES_PER_HALF;
    }

    /** 후반부가 전반부보다 몇 퍼센트 느려졌는지. 음수면 빨라진 것이다. */
    public double trendPercent() {
        if (!hasTrend()) {
            return 0.0;
        }
        return (laterP95Ns - earlierP95Ns) / earlierP95Ns * 100.0;
    }

    /** 화면에서 색과 기호를 고르기 위한 값. {@code worse} / {@code better} / {@code flat}. */
    public String trendDirection() {
        if (!hasTrend()) {
            return "unknown";
        }
        double percent = trendPercent();
        if (percent > FLAT_THRESHOLD_PERCENT) {
            return "worse";
        }
        if (percent < -FLAT_THRESHOLD_PERCENT) {
            return "better";
        }
        return "flat";
    }
}
