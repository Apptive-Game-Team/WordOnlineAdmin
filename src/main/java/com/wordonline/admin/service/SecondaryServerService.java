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

    @Qualifier("secondaryJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    private volatile boolean heartbeatColumnsMissing;

    /**
     * The secondary database does not have to be on the same migration version as the primary
     * one, so a database without the heartbeat columns of V040 falls back to the initial schema
     * and simply reports no session counts.
     */
    public List<ServerDto> getServers() {
        if (heartbeatColumnsMissing) {
            return query(BASE_COLUMNS, false);
        }
        try {
            return query(HEARTBEAT_COLUMNS, true);
        } catch (DataAccessException e) {
            log.warn("Secondary database has no server heartbeat columns, reading the initial schema instead: {}",
                    e.getMessage());
            heartbeatColumnsMissing = true;
            return query(BASE_COLUMNS, false);
        }
    }

    private List<ServerDto> query(String columns, boolean withHeartbeat) {
        return jdbcTemplate.query("select " + columns + " from servers order by id", rowMapper(withHeartbeat));
    }

    private RowMapper<ServerDto> rowMapper(boolean withHeartbeat) {
        return (rs, rowNum) -> new ServerDto(
                rs.getLong("id"),
                rs.getString("protocol"),
                rs.getString("domain"),
                rs.getInt("port"),
                ServerType.valueOf(rs.getString("type")),
                ServerState.valueOf(rs.getString("state")),
                withHeartbeat ? (Integer) rs.getObject("session_count") : null,
                withHeartbeat ? toInstant(rs) : null
        );
    }

    private Instant toInstant(ResultSet rs) throws SQLException {
        Timestamp timestamp = rs.getTimestamp("last_heartbeat_at");
        return timestamp == null ? null : timestamp.toInstant();
    }
}
