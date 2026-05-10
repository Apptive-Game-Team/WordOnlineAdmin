package com.wordonline.admin.entity.statistic;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.wordonline.admin.entity.parameter.ParameterProfile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.Getter;

@Table(name = "statistic_games")
@Entity
@Getter
public class StatisticGame {

    @Id
    private Long id;
    private Long winUserId;
    private Long lossUserId;
    private Long duration;
    private LocalDateTime createdAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "game_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    @Column(name = "run_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private StatisticRunType runType;

    @ManyToOne
    @JoinColumn(name = "parameter_profile_id")
    private ParameterProfile parameterProfile;

    private UUID simulationBatchId;

    @OneToMany(mappedBy = "statisticGame")
    private Set<StatisticGameMagic> statisticGameMagics;

    @OneToMany(mappedBy = "statisticGame")
    private Set<StatisticGameCard> statisticGameCards;
}
