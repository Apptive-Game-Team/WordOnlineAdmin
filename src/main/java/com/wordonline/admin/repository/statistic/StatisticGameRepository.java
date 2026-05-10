package com.wordonline.admin.repository.statistic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.statistic.GameType;
import com.wordonline.admin.entity.statistic.StatisticGame;
import com.wordonline.admin.entity.statistic.StatisticRunType;

public interface StatisticGameRepository extends JpaRepository<StatisticGame, Long> {

    @EntityGraph(attributePaths = {
            "statisticGameMagics",
            "statisticGameMagics.magic",
            "statisticGameCards",
            "statisticGameCards.card",
            "statisticGameCards.card.gameObject",
            "statisticGameCards.card.gameObject.parameterValues",
    })
    List<StatisticGame> findAll();
    
    @EntityGraph(attributePaths = {
            "statisticGameMagics",
            "statisticGameMagics.magic",
            "statisticGameCards",
            "statisticGameCards.card",
            "statisticGameCards.card.gameObject",
            "statisticGameCards.card.gameObject.parameterValues",
    })
    List<StatisticGame> findByGameType(GameType gameType);
    
    @EntityGraph(attributePaths = {
            "statisticGameMagics",
            "statisticGameMagics.magic",
            "statisticGameCards",
            "statisticGameCards.card",
            "statisticGameCards.card.gameObject",
            "statisticGameCards.card.gameObject.parameterValues",
    })
    List<StatisticGame> findByCreatedAtAfter(LocalDateTime date);
    
    @EntityGraph(attributePaths = {
            "statisticGameMagics",
            "statisticGameMagics.magic",
            "statisticGameCards",
            "statisticGameCards.card",
            "statisticGameCards.card.gameObject",
            "statisticGameCards.card.gameObject.parameterValues",
    })
    List<StatisticGame> findByGameTypeAndCreatedAtAfter(GameType gameType, LocalDateTime date);

    @EntityGraph(attributePaths = {
            "statisticGameMagics",
            "statisticGameMagics.magic",
            "statisticGameCards",
            "statisticGameCards.card",
            "statisticGameCards.card.gameObject",
            "statisticGameCards.card.gameObject.parameterValues",
    })
    List<StatisticGame> findByRunType(StatisticRunType runType);

    @EntityGraph(attributePaths = {
            "statisticGameMagics",
            "statisticGameMagics.magic",
            "statisticGameCards",
            "statisticGameCards.card",
            "statisticGameCards.card.gameObject",
            "statisticGameCards.card.gameObject.parameterValues",
    })
    List<StatisticGame> findBySimulationBatchId(UUID simulationBatchId);
}
