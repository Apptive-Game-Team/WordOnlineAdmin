package com.wordonline.admin.service;

import java.time.LocalDateTime;
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
        return calculateCardWinCounts(null, null);
    }

    public Map<Card, Integer> calculateCardWinCounts(GameType gameType) {
        return calculateCardWinCounts(gameType, null);
    }

    public Map<Card, Integer> calculateCardWinCounts(GameType gameType, LocalDateTime fromDate) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType, fromDate);

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
        return calculateMagicWinCounts(null, null);
    }

    public Map<Magic, Integer> calculateMagicWinCounts(GameType gameType) {
        return calculateMagicWinCounts(gameType, null);
    }

    public Map<Magic, Integer> calculateMagicWinCounts(GameType gameType, LocalDateTime fromDate) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType, fromDate);

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
        return calculateCardGameCounts(null, null);
    }

    public Map<Card, Integer> calculateCardGameCounts(GameType gameType) {
        return calculateCardGameCounts(gameType, null);
    }

    public Map<Card, Integer> calculateCardGameCounts(GameType gameType, LocalDateTime fromDate) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType, fromDate);

        Map<Card, Integer> cardCounts = new HashMap<>();

        statisticGames
                .forEach(statisticGame -> {
                    statisticGame.getStatisticGameCards()
                            .stream()
                            .map(StatisticGameCard::getCard)
                            .forEach(card -> {
                                int count = cardCounts.getOrDefault(card, 0);
                                cardCounts.put(card, count + 1);
                            });
                });
        return cardCounts;
    }

    public Map<Magic, Integer> calculateMagicGameCounts() {
        return calculateMagicGameCounts(null, null);
    }

    public Map<Magic, Integer> calculateMagicGameCounts(GameType gameType) {
        return calculateMagicGameCounts(gameType, null);
    }

    public Map<Magic, Integer> calculateMagicGameCounts(GameType gameType, LocalDateTime fromDate) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType, fromDate);

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
        return calculateCardUseCounts(null, null);
    }

    public Map<Card, Integer> calculateCardUseCounts(GameType gameType) {
        return calculateCardUseCounts(gameType, null);
    }

    public Map<Card, Integer> calculateCardUseCounts(GameType gameType, LocalDateTime fromDate) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType, fromDate);

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
        return calculateMagicUseCounts(null, null);
    }

    public Map<Magic, Integer> calculateMagicUseCounts(GameType gameType) {
        return calculateMagicUseCounts(gameType, null);
    }

    public Map<Magic, Integer> calculateMagicUseCounts(GameType gameType, LocalDateTime fromDate) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType, fromDate);

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
    
    /**
     * Calculates the number of wins per player.
     * 
     * @return Map where keys are player user IDs (Long) and values are win counts (Integer)
     */
    public Map<Long, Integer> calculatePlayerWinCounts() {
        return calculatePlayerWinCounts(null, null);
    }

    /**
     * Calculates the number of wins per player, filtered by game type.
     * 
     * @param gameType The type of game to filter by (PVP, Practice), or null for all games
     * @return Map where keys are player user IDs (Long) and values are win counts (Integer)
     */
    public Map<Long, Integer> calculatePlayerWinCounts(GameType gameType) {
        return calculatePlayerWinCounts(gameType, null);
    }

    /**
     * Calculates the number of wins per player, filtered by game type and date.
     * 
     * @param gameType The type of game to filter by (PVP, Practice), or null for all games
     * @param fromDate The date to filter from (inclusive), or null for all dates
     * @return Map where keys are player user IDs (Long) and values are win counts (Integer)
     */
    public Map<Long, Integer> calculatePlayerWinCounts(GameType gameType, LocalDateTime fromDate) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType, fromDate);
        
        Map<Long, Integer> winCounts = new HashMap<>();
        
        statisticGames.forEach(statisticGame -> {
            Long winUserId = statisticGame.getWinUserId();
            if (winUserId != null) {
                winCounts.put(winUserId, winCounts.getOrDefault(winUserId, 0) + 1);
            }
        });
        
        return winCounts;
    }

    /**
     * Calculates card usage statistics per player.
     * 
     * @return Map where keys are player user IDs (Long) and values are Maps of Card to usage count (Integer)
     */
    public Map<Long, Map<Card, Integer>> calculatePlayerCardUsage() {
        return calculatePlayerCardUsage(null, null);
    }

    /**
     * Calculates card usage statistics per player, filtered by game type.
     * 
     * @param gameType The type of game to filter by (PVP, Practice), or null for all games
     * @return Map where keys are player user IDs (Long) and values are Maps of Card to usage count (Integer)
     */
    public Map<Long, Map<Card, Integer>> calculatePlayerCardUsage(GameType gameType) {
        return calculatePlayerCardUsage(gameType, null);
    }

    /**
     * Calculates card usage statistics per player, filtered by game type and date.
     * 
     * @param gameType The type of game to filter by (PVP, Practice), or null for all games
     * @param fromDate The date to filter from (inclusive), or null for all dates
     * @return Map where keys are player user IDs (Long) and values are Maps of Card to usage count (Integer)
     */
    public Map<Long, Map<Card, Integer>> calculatePlayerCardUsage(GameType gameType, LocalDateTime fromDate) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType, fromDate);
        
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

    /**
     * Calculates magic usage statistics per player.
     * 
     * @return Map where keys are player user IDs (Long) and values are Maps of Magic to usage count (Integer)
     */
    public Map<Long, Map<Magic, Integer>> calculatePlayerMagicUsage() {
        return calculatePlayerMagicUsage(null, null);
    }

    /**
     * Calculates magic usage statistics per player, filtered by game type.
     * 
     * @param gameType The type of game to filter by (PVP, Practice), or null for all games
     * @return Map where keys are player user IDs (Long) and values are Maps of Magic to usage count (Integer)
     */
    public Map<Long, Map<Magic, Integer>> calculatePlayerMagicUsage(GameType gameType) {
        return calculatePlayerMagicUsage(gameType, null);
    }

    /**
     * Calculates magic usage statistics per player, filtered by game type and date.
     * 
     * @param gameType The type of game to filter by (PVP, Practice), or null for all games
     * @param fromDate The date to filter from (inclusive), or null for all dates
     * @return Map where keys are player user IDs (Long) and values are Maps of Magic to usage count (Integer)
     */
    public Map<Long, Map<Magic, Integer>> calculatePlayerMagicUsage(GameType gameType, LocalDateTime fromDate) {
        List<StatisticGame> statisticGames = getStatisticGames(gameType, fromDate);
        
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

    private List<StatisticGame> getStatisticGames(GameType gameType, LocalDateTime fromDate) {
        if (gameType == null && fromDate == null) {
            return statisticGameRepository.findAll();
        } else if (gameType == null) {
            return statisticGameRepository.findByCreatedAtAfter(fromDate);
        } else if (fromDate == null) {
            return statisticGameRepository.findByGameType(gameType);
        } else {
            return statisticGameRepository.findByGameTypeAndCreatedAtAfter(gameType, fromDate);
        }
    }
}
