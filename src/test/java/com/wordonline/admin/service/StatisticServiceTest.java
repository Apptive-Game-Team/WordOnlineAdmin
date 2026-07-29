package com.wordonline.admin.service;

import com.wordonline.admin.entity.magic.Card;
import com.wordonline.admin.entity.statistic.StatisticGame;
import com.wordonline.admin.entity.statistic.StatisticGameCard;
import com.wordonline.admin.repository.statistic.StatisticGameRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticServiceTest {

    @Mock
    private StatisticGameRepository statisticGameRepository;

    @Test
    void calculateCardGameCounts_countsLosingPlayersCardsToo() {
        Card card = mock(Card.class);

        Set<StatisticGameCard> cards = Set.of(
                statisticGameCard(1L, card),
                statisticGameCard(2L, card)
        );

        StatisticGame game = mock(StatisticGame.class);
        when(game.getWinUserId()).thenReturn(1L);
        when(game.getStatisticGameCards()).thenReturn(cards);
        when(statisticGameRepository.findAll()).thenReturn(List.of(game));

        StatisticService statisticService = new StatisticService(statisticGameRepository);

        assertEquals(1, statisticService.calculateCardWinCounts().get(card));
        assertEquals(2, statisticService.calculateCardGameCounts().get(card));
    }

    private StatisticGameCard statisticGameCard(Long userId, Card card) {
        StatisticGameCard statisticGameCard = mock(StatisticGameCard.class);
        when(statisticGameCard.getUserId()).thenReturn(userId);
        when(statisticGameCard.getCard()).thenReturn(card);
        return statisticGameCard;
    }
}
