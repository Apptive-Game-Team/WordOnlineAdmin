package com.wordonline.admin.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.wordonline.admin.dto.statistic.GameFrameSummaryDto;
import com.wordonline.admin.dto.statistic.GameTimingDetailDto;
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

    public List<SystemTimingDto> findSystemTimings(GameType gameType, LocalDateTime fromDate) {
        return repository.findSystemTimings(gameType, fromDate);
    }

    public List<TimeSeriesPointDto> findTimeSeries(String name, GameType gameType, LocalDateTime fromDate) {
        return repository.findTimeSeries(name, gameType, fromDate);
    }

    public List<GameFrameSummaryDto> findRecentGames(GameType gameType, LocalDateTime fromDate, int page, int size) {
        return repository.findRecentGames(gameType, fromDate, Math.max(page, 0), clampSize(size));
    }

    public long countRecentGames(GameType gameType, LocalDateTime fromDate) {
        return repository.countRecentGames(gameType, fromDate);
    }

    public Optional<GameFrameSummaryDto> findGame(long gameId) {
        return repository.findGame(gameId);
    }

    public List<GameTimingDetailDto> findGameTimings(long gameId) {
        return repository.findGameTimings(gameId);
    }

    public int totalPages(long totalCount, int size) {
        int pageSize = clampSize(size);
        return (int) ((totalCount + pageSize - 1) / pageSize);
    }

    /**
     * 시계열로 그릴 이름을 고른다. 이 페이지가 존재하는 이유가 프레임 간격이므로 {@code Frame}을
     * 기본값으로 쓰고, 그 이름이 없으면 가장 느린 항목으로 넘어간다.
     */
    public String selectName(String requested, List<SystemTimingDto> timings) {
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        boolean hasFrame = timings.stream()
                .anyMatch(timing -> StatisticUpdateTimeRepository.FRAME_STATISTIC_NAME.equals(timing.name()));
        if (hasFrame) {
            return StatisticUpdateTimeRepository.FRAME_STATISTIC_NAME;
        }
        return timings.isEmpty() ? null : timings.getFirst().name();
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
