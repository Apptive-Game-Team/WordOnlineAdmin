package com.wordonline.admin.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.wordonline.admin.entity.magic.Card;
import com.wordonline.admin.entity.magic.Magic;
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
        List<StatisticGame> statisticGames = statisticGameRepository.findAll();

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
        List<StatisticGame> statisticGames = statisticGameRepository.findAll();

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
        List<StatisticGame> statisticGames = statisticGameRepository.findAll();

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
        List<StatisticGame> statisticGames = statisticGameRepository.findAll();

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
        List<StatisticGame> statisticGames = statisticGameRepository.findAll();

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
        List<StatisticGame> statisticGames = statisticGameRepository.findAll();

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
}
