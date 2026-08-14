package com.wordonline.admin.service;

import com.wordonline.admin.dto.server.ServerDto;
import com.wordonline.admin.entity.server.ServerState;
import com.wordonline.admin.entity.server.ServerType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@ConditionalOnBean(name = "secondaryJdbcTemplate")
@RequiredArgsConstructor
    @Transactional(readOnly = true, transactionManager = "secondaryTransactionManager")
public class SecondaryServerService {

    @Qualifier("secondaryJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    /**
     * Reads only the columns that exist since the initial schema, so the secondary database
     * does not have to be on the same migration version as the primary one.
     */
    public List<ServerDto> getServers() {
        return jdbcTemplate.query(
                "select id, protocol, domain, port, type, state from servers order by id",
                (rs, rowNum) -> new ServerDto(
                        rs.getLong("id"),
                        rs.getString("protocol"),
                        rs.getString("domain"),
                        rs.getInt("port"),
                        ServerType.valueOf(rs.getString("type")),
                        ServerState.valueOf(rs.getString("state"))
                )
        );
    }
}
