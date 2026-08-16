package com.wordonline.admin.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.wordonline.admin.dto.statistic.GameFrameSummaryDto;
import com.wordonline.admin.dto.statistic.GameTimingDetailDto;
import com.wordonline.admin.dto.statistic.SessionRatePointDto;
import com.wordonline.admin.dto.statistic.StatisticDataSource;
import com.wordonline.admin.dto.statistic.SystemTimingDto;
import com.wordonline.admin.dto.statistic.TimeSeriesPointDto;
import com.wordonline.admin.entity.statistic.GameType;
import com.wordonline.admin.repository.statistic.StatisticUpdateTimeRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

/**
 * 성능 통계 조회. 밸런스 지표를 다루는 {@link StatisticService}와 대상 독자가 달라 분리했다.
 */
@Service
@RequiredArgsConstructor
public class StatisticPerformanceService {

    /** 루프 목표가 20 FPS이므로 프레임 하나의 예산은 50ms다. */
    public static final long FRAME_BUDGET_NS = 50_000_000L;

    public static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 200;

    private final StatisticUpdateTimeRepository repository;

    /**
     * 이름별 집계에 {@code Frame}을 뺀 합계 한 행을 더해 느린 순으로 돌려준다.
     * <p>
     * 합계가 따로 필요한 이유는 {@code Frame}이 대답하지 못하는 질문이 있어서다. 루프는 남는 예산을
     * 자므로 프레임 간격은 50ms에 붙박여 있고 예산을 넘기기 전까지는 작업량이 늘어도 움직이지 않는다.
     * 시스템 시간의 합은 그 작업량 자체라 예산까지 얼마나 남았는지를 바로 보여준다.
     */
    public List<SystemTimingDto> findSystemTimings(StatisticDataSource dataSource, GameType gameType,
                                                   LocalDateTime fromDate, LocalDateTime now) {
        LocalDateTime midpoint = midpoint(fromDate, now);
        List<SystemTimingDto> timings = repository.findSystemTimings(dataSource, gameType, fromDate, midpoint);
        return repository.findCombinedSystemTiming(dataSource, gameType, fromDate, midpoint)
                .map(combined -> sortedBySlowest(timings, combined))
                .orElse(timings);
    }

    /** 리포지토리가 이름별로 이미 쓰는 순서와 같다. 합계를 끼워 넣어도 표의 순서 규칙은 하나로 남는다. */
    private List<SystemTimingDto> sortedBySlowest(List<SystemTimingDto> timings, SystemTimingDto combined) {
        return Stream.concat(timings.stream(), Stream.of(combined))
                .sorted(Comparator.comparingDouble(SystemTimingDto::p95MeanIntervalNs).reversed()
                        .thenComparing(SystemTimingDto::name))
                .toList();
    }

    /** 보조 데이터베이스가 설정되어 있을 때만 화면에 토글을 노출한다. */
    public boolean isSecondaryAvailable() {
        return repository.isSecondaryAvailable();
    }

    /**
     * 추세를 낼 때 구간을 가르는 지점. 게임 수가 아니라 시간으로 반을 나눈다.
     * <p>
     * 게임 수로 나누면 최근에 트래픽이 몰린 구간이 "후반부" 전체를 차지해 비교 대상이 사실상 같은
     * 시점이 된다. 시간으로 나누면 양쪽이 같은 길이의 기간을 대표한다.
     */
    public LocalDateTime midpoint(LocalDateTime fromDate, LocalDateTime now) {
        return fromDate.plus(java.time.Duration.between(fromDate, now).dividedBy(2));
    }

    /** 합계는 저장된 이름이 아니라 계산해서 만드는 행이므로 조회 경로가 다르다. */
    public List<TimeSeriesPointDto> findTimeSeries(StatisticDataSource dataSource, String name, GameType gameType,
                                                  LocalDateTime fromDate, int days) {
        String bucket = bucketUnit(days);
        if (SystemTimingDto.COMBINED_SYSTEMS_NAME.equals(name)) {
            return repository.findCombinedTimeSeries(dataSource, gameType, fromDate, bucket);
        }
        return repository.findTimeSeries(dataSource, name, gameType, fromDate, bucket);
    }

    /**
     * 조회 범위에 맞춰 시계열의 구간 단위를 고른다.
     * <p>
     * 어느 범위에서도 점이 대략 24~90개가 되도록 맞춘 값이다. 하루를 하루 단위로 묶으면 점이
     * 하나뿐이라 추이가 보이지 않고, 1년을 시간 단위로 묶으면 8,760점이라 차트가 멈춘다.
     */
    public String bucketUnit(int days) {
        if (days <= 2) {
            return "hour";
        }
        if (days <= 90) {
            return "day";
        }
        return "week";
    }

    /**
     * 구간별 세션 수를 시간당으로 환산해 돌려준다.
     * <p>
     * 구간 길이가 범위에 따라 달라지므로 개수 그대로는 범위를 바꿔가며 비교할 수 없다. 시간당으로
     * 나누면 어느 범위에서도 같은 축이 된다.
     * <p>
     * 범위에 잘린 구간은 {@code partial}로 표시한다. 특히 진행 중인 마지막 구간은 아직 다 차지
     * 않았을 뿐인데, 표시하지 않으면 트래픽 급감으로 잘못 읽힌다.
     */
    public List<SessionRatePointDto> findSessionRate(StatisticDataSource dataSource, GameType gameType,
                                                     LocalDateTime fromDate, LocalDateTime now, int days) {
        String bucket = bucketUnit(days);
        long hours = bucketHours(bucket);
        return repository.findSessionCounts(dataSource, gameType, fromDate, bucket).stream()
                .map(count -> {
                    LocalDateTime end = count.bucketStart().plusHours(hours);
                    boolean partial = count.bucketStart().isBefore(fromDate) || end.isAfter(now);
                    return new SessionRatePointDto(
                            count.bucketStart(),
                            count.gameCount(),
                            count.gameCount() / (double) hours,
                            partial);
                })
                .toList();
    }

    long bucketHours(String bucket) {
        return switch (bucket) {
            case "hour" -> 1;
            case "day" -> 24;
            default -> 24 * 7;
        };
    }

    /** 화면에 구간 단위를 알려 주기 위한 표시용 문자열. */
    public String bucketLabel(int days) {
        return switch (bucketUnit(days)) {
            case "hour" -> "시간";
            case "day" -> "일";
            default -> "주";
        };
    }

    public List<GameFrameSummaryDto> findRecentGames(StatisticDataSource dataSource, GameType gameType,
                                                     LocalDateTime fromDate, int page, int size) {
        return repository.findRecentGames(dataSource, gameType, fromDate, Math.max(page, 0), clampSize(size));
    }

    public long countRecentGames(StatisticDataSource dataSource, GameType gameType, LocalDateTime fromDate) {
        return repository.countRecentGames(dataSource, gameType, fromDate);
    }

    public Optional<GameFrameSummaryDto> findGame(StatisticDataSource dataSource, long gameId) {
        return repository.findGame(dataSource, gameId);
    }

    public List<GameTimingDetailDto> findGameTimings(StatisticDataSource dataSource, long gameId) {
        return repository.findGameTimings(dataSource, gameId);
    }

    public int totalPages(long totalCount, int size) {
        int pageSize = clampSize(size);
        return (int) ((totalCount + pageSize - 1) / pageSize);
    }

    /**
     * 시계열로 그릴 이름을 고른다. 이 페이지가 존재하는 이유가 프레임 간격이므로 {@code Frame}을
     * 기본값으로 쓴다.
     * <p>
     * {@code Frame} 행이 없으면 합계로 넘어간다. 게임 서버가 프레임 간격을 기록하기 전에 끝난 게임만
     * 조회 범위에 들어오면 이름별 목록에서 가장 느린 시스템 하나가 뽑히는데, 그 시스템은 전체 부하를
     * 대표하지 못하면서도 마치 기본 지표인 것처럼 보인다.
     */
    public String selectName(String requested, List<SystemTimingDto> timings) {
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        return firstPresent(timings, StatisticUpdateTimeRepository.FRAME_STATISTIC_NAME)
                .or(() -> firstPresent(timings, SystemTimingDto.COMBINED_SYSTEMS_NAME))
                .or(() -> timings.stream().map(SystemTimingDto::name).findFirst())
                .orElse(null);
    }

    private Optional<String> firstPresent(List<SystemTimingDto> timings, String name) {
        return timings.stream()
                .map(SystemTimingDto::name)
                .filter(name::equals)
                .findFirst();
    }

    public SystemTimingDto frameTiming(List<SystemTimingDto> timings) {
        return timings.stream()
                .filter(timing -> StatisticUpdateTimeRepository.FRAME_STATISTIC_NAME.equals(timing.name()))
                .findFirst()
                .orElse(null);
    }

    /** 주소창을 손으로 고쳐 테이블 전체를 요청하지 못하도록 페이지 크기를 제한한다. */
    private int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
