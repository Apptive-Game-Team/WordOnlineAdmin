package com.wordonline.admin.entity.statistic;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Table(name = "statistic_games")
@Entity
public class StatisticGame {

    @Id
    private Long id;
    private Long winUserId;
    private Long lossUserId;
    private Long duration;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "statisticGame")
    private List<StatisticGameMagic> statisticGameMagics;

    @OneToMany(mappedBy = "statisticGame")
    private List<StatisticGameCard> statisticGameCards;
}
