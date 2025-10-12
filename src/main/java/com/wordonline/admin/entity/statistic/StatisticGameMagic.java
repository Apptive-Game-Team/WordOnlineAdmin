package com.wordonline.admin.entity.statistic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "statistic_game_magics")
public class StatisticGameMagic {

    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "statistic_game_id")
    private StatisticGame statisticGame;

    private Long userId;

    @ManyToOne
    @JoinColumn(name = "magic_id")
    private Magic magic;

    private Integer count;
}
