package com.wordonline.admin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.wordonline.admin.entity.magic.Card;
import com.wordonline.admin.entity.magic.Magic;
import com.wordonline.admin.entity.statistic.GameType;
import com.wordonline.admin.entity.statistic.StatisticGame;
import com.wordonline.admin.entity.statistic.StatisticGameCard;
import com.wordonline.admin.entity.statistic.StatisticGameMagic;
import com.wordonline.admin.repository.statistic.StatisticGameRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticService {

    private final StatisticGameRepository statisticGameRepository;

    public Map<Card, Integer> calculateCardWinCounts() {
        return calculateCardWinCounts(null);
    }

    public Map<Card, Integer> calculateCardWinCounts(GameType gameType) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType);

        Map<Card, Integer> cardCounts = new HashMap<>();

        statisticGames
                .forEach(statisticGame -> {
                    statisticGame.getStatisticGameCards()
                            .stream()
                            .filter(statisticGameCard ->
                                    Objects.equals(
                                            statisticGameCard.getUserId(),
                                            statisticGame.getWinUserId()
                                    ))
                            .map(StatisticGameCard::getCard)
                            .forEach(card -> {
                                int count = cardCounts.getOrDefault(card, 0);
                                cardCounts.put(card, count + 1);
                            });
                });
        return cardCounts;
    }

    public Map<Magic, Integer> calculateMagicWinCounts() {
        return calculateMagicWinCounts(null);
    }

    public Map<Magic, Integer> calculateMagicWinCounts(GameType gameType) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType);

        Map<Magic, Integer> magicCounts = new HashMap<>();

        statisticGames
                .forEach(statisticGame -> {
                    statisticGame.getStatisticGameMagics()
                            .stream()
                            .filter(statisticGameMagic ->
                                    Objects.equals(
                                            statisticGameMagic.getUserId(),
                                            statisticGame.getWinUserId()
                                    ))
                            .map(StatisticGameMagic::getMagic)
                            .forEach(magic -> {
                                int count = magicCounts.getOrDefault(magic, 0);
                                magicCounts.put(magic, count + 1);
                            });
                });
        return magicCounts;
    }

    public Map<Card, Integer> calculateCardGameCounts() {
        return calculateCardGameCounts(null);
    }

    public Map<Card, Integer> calculateCardGameCounts(GameType gameType) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType);

        Map<Card, Integer> cardCounts = new HashMap<>();

        statisticGames
                .forEach(statisticGame -> {
                    statisticGame.getStatisticGameCards()
                            .stream()
                            .filter(statisticGameCard ->
                                    Objects.equals(
                                            statisticGameCard.getUserId(),
                                            statisticGame.getWinUserId()
                                    ))
                            .map(StatisticGameCard::getCard)
                            .forEach(card -> {
                                int count = cardCounts.getOrDefault(card, 0);
                                cardCounts.put(card, count + 1);
                            });
                });
        return cardCounts;
    }

    public Map<Magic, Integer> calculateMagicGameCounts() {
        return calculateMagicGameCounts(null);
    }

    public Map<Magic, Integer> calculateMagicGameCounts(GameType gameType) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType);

        Map<Magic, Integer> magicCounts = new HashMap<>();

        statisticGames
                .forEach(statisticGame -> {
                    statisticGame.getStatisticGameMagics()
                            .stream()
                            .map(StatisticGameMagic::getMagic)
                            .forEach(magic -> {
                                int count = magicCounts.getOrDefault(magic, 0);
                                magicCounts.put(magic, count + 1);
                            });
                });
        return magicCounts;
    }

    public Map<Card, Integer> calculateCardUseCounts() {
        return calculateCardUseCounts(null);
    }

    public Map<Card, Integer> calculateCardUseCounts(GameType gameType) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType);

        Map<Card, Integer> cardCounts = new HashMap<>();

        statisticGames
                .forEach(statisticGame -> {
                    statisticGame.getStatisticGameCards()
                            .forEach(statisticGameCard -> {
                                Card card = statisticGameCard.getCard();
                                int count = cardCounts.getOrDefault(card, 0);
                                cardCounts.put(card, count + statisticGameCard.getCount());
                            });
                });
        return cardCounts;
    }

    public Map<Magic, Integer> calculateMagicUseCounts() {
        return calculateMagicUseCounts(null);
    }

    public Map<Magic, Integer> calculateMagicUseCounts(GameType gameType) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType);

        Map<Magic, Integer> magicCounts = new HashMap<>();

        statisticGames
                .forEach(statisticGame -> {
                    statisticGame.getStatisticGameMagics()
                            .forEach(statisticGameMagic -> {
                                Magic magic = statisticGameMagic.getMagic();
                                int count = magicCounts.getOrDefault(magic, 0);
                                magicCounts.put(magic, count + statisticGameMagic.getCount());
                            });
                });
        return magicCounts;
    }

    // Per-player statistics
    public Map<Long, Integer> calculatePlayerWinCounts() {
        return calculatePlayerWinCounts(null);
    }

    public Map<Long, Integer> calculatePlayerWinCounts(GameType gameType) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType);
        
        Map<Long, Integer> winCounts = new HashMap<>();
        
        statisticGames.forEach(statisticGame -> {
            Long winUserId = statisticGame.getWinUserId();
            if (winUserId != null) {
                winCounts.put(winUserId, winCounts.getOrDefault(winUserId, 0) + 1);
            }
        });
        
        return winCounts;
    }

    public Map<Long, Map<Card, Integer>> calculatePlayerCardUsage() {
        return calculatePlayerCardUsage(null);
    }

    public Map<Long, Map<Card, Integer>> calculatePlayerCardUsage(GameType gameType) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType);
        
        Map<Long, Map<Card, Integer>> playerCardUsage = new HashMap<>();
        
        statisticGames.forEach(statisticGame -> {
            statisticGame.getStatisticGameCards().forEach(statisticGameCard -> {
                Long userId = statisticGameCard.getUserId();
                Card card = statisticGameCard.getCard();
                
                playerCardUsage.computeIfAbsent(userId, k -> new HashMap<>());
                Map<Card, Integer> cardUsage = playerCardUsage.get(userId);
                cardUsage.put(card, cardUsage.getOrDefault(card, 0) + statisticGameCard.getCount());
            });
        });
        
        return playerCardUsage;
    }

    public Map<Long, Map<Magic, Integer>> calculatePlayerMagicUsage() {
        return calculatePlayerMagicUsage(null);
    }

    public Map<Long, Map<Magic, Integer>> calculatePlayerMagicUsage(GameType gameType) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType);
        
        Map<Long, Map<Magic, Integer>> playerMagicUsage = new HashMap<>();
        
        statisticGames.forEach(statisticGame -> {
            statisticGame.getStatisticGameMagics().forEach(statisticGameMagic -> {
                Long userId = statisticGameMagic.getUserId();
                Magic magic = statisticGameMagic.getMagic();
                
                playerMagicUsage.computeIfAbsent(userId, k -> new HashMap<>());
                Map<Magic, Integer> magicUsage = playerMagicUsage.get(userId);
                magicUsage.put(magic, magicUsage.getOrDefault(magic, 0) + statisticGameMagic.getCount());
            });
        });
        
        return playerMagicUsage;
    }

    private List<StatisticGame> getStatisticGames(GameType gameType) {
        if (gameType == null) {
            return statisticGameRepository.findAll();
        }
        return statisticGameRepository.findByGameType(gameType);
    }
}
