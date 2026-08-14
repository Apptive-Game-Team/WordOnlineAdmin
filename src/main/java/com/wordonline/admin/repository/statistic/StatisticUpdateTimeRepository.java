package com.wordonline.admin.repository.statistic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.wordonline.admin.dto.statistic.GameFrameSummaryDto;
import com.wordonline.admin.dto.statistic.GameTimingDetailDto;
import com.wordonline.admin.dto.statistic.SystemTimingDto;
import com.wordonline.admin.dto.statistic.TimeSeriesPointDto;
import com.wordonline.admin.entity.statistic.GameType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.stereotype.Repository;

/**
 * `statistic_update_time`의 조회 전용 리포지토리.
 * <p>
 * 집계와 백분위수는 파생 쿼리로 표현할 수 없어 {@code BotAdminRepository}와 같이 네이티브 쿼리를
 * 쓴다. {@code game_type}은 Postgres enum이라 문자열 파라미터와 직접 비교할 수 없으므로 양쪽을
 * text로 맞춘다.
 */
@Repository
public class StatisticUpdateTimeRepository {

    /** 게임별 프레임 간격 통계가 기록되는 이름. 게임 서버의 `GameResultBuilder`가 정한 값이다. */
    public static final String FRAME_STATISTIC_NAME = "Frame";

    @PersistenceContext
    private EntityManager entityManager;

    /** 모든 조회가 공유하는 필터. 게임 테이블은 항상 {@code g}로 조인한다. */
    private static final String FILTER = """
             AND (CAST(:fromDate AS TIMESTAMP) IS NULL OR g.created_at >= CAST(:fromDate AS TIMESTAMP))
             AND (CAST(:gameType AS TEXT) IS NULL OR g.game_type::text = CAST(:gameType AS TEXT))
            """;

    /**
     * 이름별 집계.
     * <p>
     * `AVG(mean_interval_ns)`를 쓰지 않는 이유는 {@link SystemTimingDto}에 적어 두었다.
     * {@code PERCENTILE_CONT} 안의 {@code DECIMAL} 캐스팅은 장식이 아니다. 보간이 일어나는
     * 백분위수를 큰 부동소수 값에 적용하면 결과 스케일이 음수로 계산되어 statement 자체가 거부되는
     * 엔진이 있다. 스케일이 선언된 타입으로 캐스팅하면 사라지고, Postgres는 어느 쪽이든 같은 값을
     * 낸다.
     */
    public List<SystemTimingDto> findSystemTimings(GameType gameType, LocalDateTime fromDate) {
        Query query = entityManager.createNativeQuery("""
                SELECT ut.name,
                       MIN(ut.min_interval_ns),
                       MAX(ut.max_interval_ns),
                       PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY CAST(ut.mean_interval_ns AS DECIMAL(30, 3))),
                       PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY CAST(ut.mean_interval_ns AS DECIMAL(30, 3))),
                       COUNT(*)
                FROM statistic_update_time ut
                JOIN statistic_games g ON g.id = ut.statistic_game_id
                WHERE 1 = 1
                """ + FILTER + """
                GROUP BY ut.name
                ORDER BY 5 DESC, ut.name ASC
                """);
        bindFilter(query, gameType, fromDate);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(row -> new SystemTimingDto(
                (String) row[0],
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue(),
                ((Number) row[3]).doubleValue(),
                ((Number) row[4]).doubleValue(),
                ((Number) row[5]).longValue()
        )).toList();
    }

    /**
     * 이름 하나의 추이. 게임마다 점을 찍지 않고 {@code bucket} 단위로 묶는다.
     * <p>
     * 게임당 한 점이면 1년 범위에서 15,000점이 넘어 페이지가 900KB가 되고 차트가 멈춘다. 구간별
     * 중앙값을 쓰면 점 개수가 범위와 무관하게 수십 개로 유지되고, 이름별 집계와 같은 방식(평균이
     * 아니라 분포)으로 계산된다.
     *
     * @param bucket {@code date_trunc}에 넘길 단위. 호출자가 조회 범위에 맞춰 고른다.
     */
    public List<TimeSeriesPointDto> findTimeSeries(String name, GameType gameType, LocalDateTime fromDate,
                                                   String bucket) {
        Query query = entityManager.createNativeQuery("""
                SELECT date_trunc(CAST(:bucket AS TEXT), g.created_at) AS bucket_start,
                       PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY CAST(ut.mean_interval_ns AS DECIMAL(30, 3))),
                       COUNT(*)
                FROM statistic_update_time ut
                JOIN statistic_games g ON g.id = ut.statistic_game_id
                WHERE ut.name = CAST(:name AS TEXT)
                """ + FILTER + """
                GROUP BY bucket_start
                ORDER BY bucket_start ASC
                """);
        query.setParameter("name", name);
        query.setParameter("bucket", bucket);
        bindFilter(query, gameType, fromDate);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(row -> new TimeSeriesPointDto(
                toLocalDateTime(row[0]),
                ((Number) row[1]).doubleValue(),
                ((Number) row[2]).longValue()
        )).toList();
    }

    /** 프레임 행이 없는 게임도 목록에서 사라지지 않도록 left join이다. */
    public List<GameFrameSummaryDto> findRecentGames(GameType gameType, LocalDateTime fromDate, int page, int size) {
        Query query = entityManager.createNativeQuery("""
                SELECT g.id, g.created_at, g.game_type::text, g.duration,
                       f.mean_interval_ns, f.max_interval_ns
                FROM statistic_games g
                LEFT JOIN statistic_update_time f
                       ON f.statistic_game_id = g.id AND f.name = CAST(:frameName AS TEXT)
                WHERE 1 = 1
                """ + FILTER + """
                ORDER BY g.created_at DESC, g.id DESC
                LIMIT CAST(:size AS INTEGER) OFFSET CAST(:offset AS INTEGER)
                """);
        query.setParameter("frameName", FRAME_STATISTIC_NAME);
        query.setParameter("size", size);
        query.setParameter("offset", (long) page * size);
        bindFilter(query, gameType, fromDate);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(StatisticUpdateTimeRepository::toGameSummary).toList();
    }

    public long countRecentGames(GameType gameType, LocalDateTime fromDate) {
        Query query = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM statistic_games g
                WHERE 1 = 1
                """ + FILTER);
        bindFilter(query, gameType, fromDate);
        return ((Number) query.getSingleResult()).longValue();
    }

    public Optional<GameFrameSummaryDto> findGame(long gameId) {
        Query query = entityManager.createNativeQuery("""
                SELECT g.id, g.created_at, g.game_type::text, g.duration,
                       f.mean_interval_ns, f.max_interval_ns
                FROM statistic_games g
                LEFT JOIN statistic_update_time f
                       ON f.statistic_game_id = g.id AND f.name = CAST(:frameName AS TEXT)
                WHERE g.id = :gameId
                """);
        query.setParameter("frameName", FRAME_STATISTIC_NAME);
        query.setParameter("gameId", gameId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().findFirst().map(StatisticUpdateTimeRepository::toGameSummary);
    }

    public List<GameTimingDetailDto> findGameTimings(long gameId) {
        Query query = entityManager.createNativeQuery("""
                SELECT ut.name, ut.min_interval_ns, ut.max_interval_ns, ut.mean_interval_ns
                FROM statistic_update_time ut
                WHERE ut.statistic_game_id = :gameId
                ORDER BY ut.mean_interval_ns DESC, ut.name ASC
                """);
        query.setParameter("gameId", gameId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(row -> new GameTimingDetailDto(
                (String) row[0],
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue(),
                ((Number) row[3]).doubleValue()
        )).toList();
    }

    private void bindFilter(Query query, GameType gameType, LocalDateTime fromDate) {
        query.setParameter("gameType", gameType == null ? null : gameType.name());
        query.setParameter("fromDate", fromDate);
    }

    private static GameFrameSummaryDto toGameSummary(Object[] row) {
        return new GameFrameSummaryDto(
                ((Number) row[0]).longValue(),
                toLocalDateTime(row[1]),
                (String) row[2],
                ((Number) row[3]).longValue(),
                row[4] == null ? null : ((Number) row[4]).doubleValue(),
                row[5] == null ? null : ((Number) row[5]).longValue()
        );
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }
}
