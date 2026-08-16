package com.wordonline.admin.entity.statistic;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

/**
 * Lifecycle record of a game session, written by the game server at session start and
 * resolved at session end. Unlike {@link StatisticGame} this row exists for every session
 * from the moment it starts, so games that never finished (deadlocked loops, crashed
 * servers) are visible here.
 */
@Table(name = "statistic_game_sessions")
@Entity
@Getter
public class StatisticGameSession {

    @Id
    private Long id;
    private String sessionId;
    private Long leftUserId;
    private Long rightUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private GameType gameType;

    private String serverDomain;
    private Integer serverPort;
    private String serverInstanceId;
    private String serverVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private GameSessionStatus status;

    private String endReason;
    private String endDetail;
    private Long statisticGameId;
    private Instant startedAt;
    private Instant endedAt;
}
