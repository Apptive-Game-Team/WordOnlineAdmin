package com.wordonline.admin.entity.statistic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;

@Entity
@Table(name = "statistic_game_cards")
@Getter
public class StatisticGameCard {

    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "statistic_game_id")
    private StatisticGame statisticGame;

    private Long userId;

    @ManyToOne
    @JoinColumn(name = "card_id")
    private Card card;

    private Integer count;

}
