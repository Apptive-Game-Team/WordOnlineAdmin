package com.wordonline.admin.repository.statistic;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.statistic.StatisticGame;

public interface StatisticGameRepository extends JpaRepository<StatisticGame, Long> {

    @EntityGraph(attributePaths = {
            "statisticGameMagics",
            "statisticGameMagics.magic",
            "statisticGameCards",
            "statisticGameCards.card",
    })
    List<StatisticGame> findAll();
}
