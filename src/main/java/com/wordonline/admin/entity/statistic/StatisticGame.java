package com.wordonline.admin.entity.statistic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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

    @OneToMany(mappedBy = "statisticGame")
    private Set<StatisticGameMagic> statisticGameMagics;

    @OneToMany(mappedBy = "statisticGame")
    private Set<StatisticGameCard> statisticGameCards;
}
