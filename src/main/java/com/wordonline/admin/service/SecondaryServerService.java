package com.wordonline.admin.service;

import com.wordonline.admin.dto.server.ServerDto;
import com.wordonline.admin.entity.server.ServerState;
import com.wordonline.admin.entity.server.ServerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@ConditionalOnBean(name = "secondaryJdbcTemplate")
@RequiredArgsConstructor
    @Transactional(readOnly = true, transactionManager = "secondaryTransactionManager")
public class SecondaryServerService {

    private static final String BASE_COLUMNS = "id, protocol, domain, port, type, state";
    private static final String HEARTBEAT_COLUMNS = BASE_COLUMNS + ", session_count, last_heartbeat_at";
    private static final String TARGET_BOT_COLUMNS = HEARTBEAT_COLUMNS + ", target_bot_sessions";

    @Qualifier("secondaryJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    private volatile boolean heartbeatColumnsMissing;
    private volatile boolean targetBotColumnMissing;

    /**
     * The secondary database does not have to be on the same migration version as the primary
     * one, so the read degrades column set by column set: without V051 it reports no bot target,
     * and without the heartbeat columns of V040 it falls back to the initial schema and simply
     * reports no session counts either.
     */
    public List<ServerDto> getServers() {
        if (!heartbeatColumnsMissing && !targetBotColumnMissing) {
            try {
                return query(TARGET_BOT_COLUMNS, true, true);
            } catch (DataAccessException e) {
                log.warn("Secondary database has no target_bot_sessions column, reading without it: {}",
                        e.getMessage());
                targetBotColumnMissing = true;
            }
        }
        if (!heartbeatColumnsMissing) {
            try {
                return query(HEARTBEAT_COLUMNS, true, false);
            } catch (DataAccessException e) {
                log.warn("Secondary database has no server heartbeat columns, reading the initial schema instead: {}",
                        e.getMessage());
                heartbeatColumnsMissing = true;
            }
        }
        return query(BASE_COLUMNS, false, false);
    }

    /**
     * @return the number of rows changed; 0 when the id does not name a game server
     * @throws DataAccessException when the secondary database predates the
     *         {@code target_bot_sessions} column (migration V051 not applied yet)
     */
    @Transactional(transactionManager = "secondaryTransactionManager")
    public int updateTargetBotSessions(long serverId, Integer targetBotSessions) {
        return jdbcTemplate.update(
                "update servers set target_bot_sessions = ? where id = ? and type = 'GAME'",
                targetBotSessions, serverId);
    }

    private List<ServerDto> query(String columns, boolean withHeartbeat, boolean withTargetBot) {
        return jdbcTemplate.query("select " + columns + " from servers order by id",
                rowMapper(withHeartbeat, withTargetBot));
    }

    private RowMapper<ServerDto> rowMapper(boolean withHeartbeat, boolean withTargetBot) {
        return (rs, rowNum) -> new ServerDto(
                rs.getLong("id"),
                rs.getString("protocol"),
                rs.getString("domain"),
                rs.getInt("port"),
                ServerType.valueOf(rs.getString("type")),
                ServerState.valueOf(rs.getString("state")),
                withHeartbeat ? (Integer) rs.getObject("session_count") : null,
                withHeartbeat ? toInstant(rs) : null,
                withTargetBot ? (Integer) rs.getObject("target_bot_sessions") : null
        );
    }

    private Instant toInstant(ResultSet rs) throws SQLException {
        Timestamp timestamp = rs.getTimestamp("last_heartbeat_at");
        return timestamp == null ? null : timestamp.toInstant();
    }
}
