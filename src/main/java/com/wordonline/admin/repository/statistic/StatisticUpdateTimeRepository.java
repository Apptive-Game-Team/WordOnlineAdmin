package com.wordonline.admin.repository.statistic;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.wordonline.admin.dto.statistic.GameFrameSummaryDto;
import com.wordonline.admin.dto.statistic.GameTimingDetailDto;
import com.wordonline.admin.dto.statistic.SessionCountDto;
import com.wordonline.admin.dto.statistic.StatisticDataSource;
import com.wordonline.admin.dto.statistic.SystemTimingDto;
import com.wordonline.admin.dto.statistic.TimeSeriesPointDto;
import com.wordonline.admin.entity.statistic.GameType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * `statistic_update_time`의 조회 전용 리포지토리.
 * <p>
 * 집계와 백분위수는 파생 쿼리로 표현할 수 없어 {@code BotAdminRepository}와 같이 네이티브 SQL을
 * 쓴다. {@code game_type}은 Postgres enum이라 문자열 파라미터와 직접 비교할 수 없으므로 양쪽을
 * text로 맞춘다.
 * <p>
 * {@code EntityManager}가 아니라 {@code JdbcTemplate}으로 도는 이유는 보조 데이터베이스 때문이다.
 * {@code SecondaryDatabaseConfig}는 보조 쪽에 JPA를 붙이지 않고 {@code JdbcTemplate}만 만든다. 두
 * 데이터베이스를 같은 코드로 조회하려면 이쪽에 맞춰야 한다.
 */
@Repository
public class StatisticUpdateTimeRepository {

    /** 게임별 프레임 간격 통계가 기록되는 이름. 게임 서버의 `GameResultBuilder`가 정한 값이다. */
    public static final String FRAME_STATISTIC_NAME = "Frame";

    private final NamedParameterJdbcTemplate primary;
    private final NamedParameterJdbcTemplate secondary;

    public StatisticUpdateTimeRepository(
            JdbcTemplate primaryJdbcTemplate,
            // 보조 데이터베이스는 DEV_DATABASE_URL이 있을 때만 뜬다. 없으면 null로 남고,
            // 이 화면은 주 데이터베이스만 제공한다.
            @Autowired(required = false) @Qualifier("secondaryJdbcTemplate") JdbcTemplate secondaryJdbcTemplate) {
        this.primary = new NamedParameterJdbcTemplate(primaryJdbcTemplate);
        this.secondary = secondaryJdbcTemplate == null
                ? null
                : new NamedParameterJdbcTemplate(secondaryJdbcTemplate);
    }

    public boolean isSecondaryAvailable() {
        return secondary != null;
    }

    /** 보조가 없으면 조용히 주 데이터베이스로 떨어진다. 화면에서도 토글을 감춘다. */
    private NamedParameterJdbcTemplate template(StatisticDataSource dataSource) {
        if (dataSource == StatisticDataSource.SECONDARY && secondary != null) {
            return secondary;
        }
        return primary;
    }

    /** 모든 조회가 공유하는 필터. 게임 테이블은 항상 {@code g}로 조인한다. */
    private static final String FILTER = """
             AND (CAST(:fromDate AS TIMESTAMP) IS NULL OR g.created_at >= CAST(:fromDate AS TIMESTAMP))
             AND (CAST(:gameType AS TEXT) IS NULL OR g.game_type::text = CAST(:gameType AS TEXT))
            """;

    private static final String FIND_SYSTEM_TIMINGS = """
            SELECT ut.name AS name,
                   MIN(ut.min_interval_ns) AS min_ns,
                   MAX(ut.max_interval_ns) AS max_ns,
                   PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY CAST(ut.mean_interval_ns AS DECIMAL(30, 3))) AS median_ns,
                   PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY CAST(ut.mean_interval_ns AS DECIMAL(30, 3))) AS p95_ns,
                   COUNT(*) AS games,
                   PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY CASE
                       WHEN g.created_at < CAST(:midpoint AS TIMESTAMP)
                       THEN CAST(ut.mean_interval_ns AS DECIMAL(30, 3)) END) AS earlier_p95_ns,
                   PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY CASE
                       WHEN g.created_at >= CAST(:midpoint AS TIMESTAMP)
                       THEN CAST(ut.mean_interval_ns AS DECIMAL(30, 3)) END) AS later_p95_ns,
                   COUNT(*) FILTER (WHERE g.created_at <  CAST(:midpoint AS TIMESTAMP)) AS earlier_games,
                   COUNT(*) FILTER (WHERE g.created_at >= CAST(:midpoint AS TIMESTAMP)) AS later_games
            FROM statistic_update_time ut
            JOIN statistic_games g ON g.id = ut.statistic_game_id
            WHERE 1 = 1
            """ + FILTER + """
            GROUP BY ut.name
            ORDER BY p95_ns DESC, ut.name ASC
            """;

    private static final RowMapper<SystemTimingDto> SYSTEM_TIMING_MAPPER = (rs, rowNum) -> new SystemTimingDto(
            rs.getString("name"),
            rs.getLong("min_ns"),
            rs.getLong("max_ns"),
            rs.getDouble("median_ns"),
            rs.getDouble("p95_ns"),
            rs.getLong("games"),
            nullableDouble(rs, "earlier_p95_ns"),
            nullableDouble(rs, "later_p95_ns"),
            rs.getLong("earlier_games"),
            rs.getLong("later_games"));

    private static final RowMapper<TimeSeriesPointDto> TIME_SERIES_MAPPER = (rs, rowNum) -> new TimeSeriesPointDto(
            rs.getTimestamp("bucket_start").toLocalDateTime(),
            rs.getDouble("median_ns"),
            rs.getLong("games"));

    private static final RowMapper<SessionCountDto> SESSION_COUNT_MAPPER = (rs, rowNum) -> new SessionCountDto(
            rs.getTimestamp("bucket_start").toLocalDateTime(),
            rs.getLong("games"));

    private static final RowMapper<GameFrameSummaryDto> GAME_SUMMARY_MAPPER = (rs, rowNum) -> new GameFrameSummaryDto(
            rs.getLong("game_id"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getString("game_type"),
            rs.getLong("duration"),
            nullableDouble(rs, "frame_mean_ns"),
            nullableLong(rs, "frame_max_ns"));

    private static final RowMapper<GameTimingDetailDto> GAME_TIMING_MAPPER = (rs, rowNum) -> new GameTimingDetailDto(
            rs.getString("name"),
            rs.getLong("min_ns"),
            rs.getLong("max_ns"),
            rs.getDouble("mean_ns"));

    private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Map<String, Object> filterParams(GameType gameType, LocalDateTime fromDate) {
        // HashMap이어야 한다. Map.of는 null 값을 거부하는데 필터 없음이 곧 null이다.
        Map<String, Object> params = new HashMap<>();
        params.put("gameType", gameType == null ? null : gameType.name());
        params.put("fromDate", fromDate);
        return params;
    }

    /**
     * 이름별 집계와, 같은 구간을 {@code midpoint}로 갈라 구한 전·후반 p95.
     * <p>
     * 전·후반을 따로 조회하지 않고 한 statement에서 낸다. {@code CASE}가 반대쪽 절반을 NULL로 만들고
     * 순서 집합 집계는 NULL을 무시하므로, 같은 스캔에서 두 값이 함께 나온다.
     */
    public List<SystemTimingDto> findSystemTimings(StatisticDataSource dataSource, GameType gameType,
                                                   LocalDateTime fromDate, LocalDateTime midpoint) {
        Map<String, Object> params = filterParams(gameType, fromDate);
        params.put("midpoint", midpoint);
        return template(dataSource).query(FIND_SYSTEM_TIMINGS, params, SYSTEM_TIMING_MAPPER);
    }

    /**
     * 이름 하나의 추이. 게임마다 점을 찍지 않고 {@code bucket} 단위로 묶는다.
     * <p>
     * 게임당 한 점이면 1년 범위에서 15,000점이 넘어 페이지가 900KB가 되고 차트가 멈춘다.
     */
    public List<TimeSeriesPointDto> findTimeSeries(StatisticDataSource dataSource, String name, GameType gameType,
                                                   LocalDateTime fromDate, String bucket) {
        Map<String, Object> params = filterParams(gameType, fromDate);
        params.put("name", name);
        params.put("bucket", bucket);
        return template(dataSource).query("""
                SELECT date_trunc(CAST(:bucket AS TEXT), g.created_at) AS bucket_start,
                       PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY CAST(ut.mean_interval_ns AS DECIMAL(30, 3))) AS median_ns,
                       COUNT(*) AS games
                FROM statistic_update_time ut
                JOIN statistic_games g ON g.id = ut.statistic_game_id
                WHERE ut.name = CAST(:name AS TEXT)
                """ + FILTER + """
                GROUP BY bucket_start
                ORDER BY bucket_start ASC
                """, params, TIME_SERIES_MAPPER);
    }

    /**
     * 구간별로 끝난 게임 수.
     * <p>
     * {@code statistic_games}만 읽는다. 타이밍 통계가 없는 게임도 세션은 세션이므로 조인하지 않는다.
     */
    public List<SessionCountDto> findSessionCounts(StatisticDataSource dataSource, GameType gameType,
                                                   LocalDateTime fromDate, String bucket) {
        Map<String, Object> params = filterParams(gameType, fromDate);
        params.put("bucket", bucket);
        return template(dataSource).query("""
                SELECT date_trunc(CAST(:bucket AS TEXT), g.created_at) AS bucket_start, COUNT(*) AS games
                FROM statistic_games g
                WHERE 1 = 1
                """ + FILTER + """
                GROUP BY bucket_start
                ORDER BY bucket_start ASC
                """, params, SESSION_COUNT_MAPPER);
    }

    /** 프레임 행이 없는 게임도 목록에서 사라지지 않도록 left join이다. */
    public List<GameFrameSummaryDto> findRecentGames(StatisticDataSource dataSource, GameType gameType,
                                                     LocalDateTime fromDate, int page, int size) {
        Map<String, Object> params = filterParams(gameType, fromDate);
        params.put("frameName", FRAME_STATISTIC_NAME);
        params.put("size", size);
        params.put("offset", (long) page * size);
        return template(dataSource).query("""
                SELECT g.id AS game_id, g.created_at AS created_at, g.game_type::text AS game_type,
                       g.duration AS duration,
                       f.mean_interval_ns AS frame_mean_ns, f.max_interval_ns AS frame_max_ns
                FROM statistic_games g
                LEFT JOIN statistic_update_time f
                       ON f.statistic_game_id = g.id AND f.name = CAST(:frameName AS TEXT)
                WHERE 1 = 1
                """ + FILTER + """
                ORDER BY g.created_at DESC, g.id DESC
                LIMIT CAST(:size AS INTEGER) OFFSET CAST(:offset AS INTEGER)
                """, params, GAME_SUMMARY_MAPPER);
    }

    public long countRecentGames(StatisticDataSource dataSource, GameType gameType, LocalDateTime fromDate) {
        Long total = template(dataSource).queryForObject("""
                SELECT COUNT(*)
                FROM statistic_games g
                WHERE 1 = 1
                """ + FILTER, filterParams(gameType, fromDate), Long.class);
        return total == null ? 0L : total;
    }

    public Optional<GameFrameSummaryDto> findGame(StatisticDataSource dataSource, long gameId) {
        Map<String, Object> params = new HashMap<>();
        params.put("gameId", gameId);
        params.put("frameName", FRAME_STATISTIC_NAME);
        return template(dataSource).query("""
                SELECT g.id AS game_id, g.created_at AS created_at, g.game_type::text AS game_type,
                       g.duration AS duration,
                       f.mean_interval_ns AS frame_mean_ns, f.max_interval_ns AS frame_max_ns
                FROM statistic_games g
                LEFT JOIN statistic_update_time f
                       ON f.statistic_game_id = g.id AND f.name = CAST(:frameName AS TEXT)
                WHERE g.id = :gameId
                """, params, GAME_SUMMARY_MAPPER).stream().findFirst();
    }

    public List<GameTimingDetailDto> findGameTimings(StatisticDataSource dataSource, long gameId) {
        return template(dataSource).query("""
                SELECT ut.name AS name, ut.min_interval_ns AS min_ns,
                       ut.max_interval_ns AS max_ns, ut.mean_interval_ns AS mean_ns
                FROM statistic_update_time ut
                WHERE ut.statistic_game_id = :gameId
                ORDER BY ut.mean_interval_ns DESC, ut.name ASC
                """, Map.of("gameId", gameId), GAME_TIMING_MAPPER);
    }
}
