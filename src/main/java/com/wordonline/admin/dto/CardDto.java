package com.wordonline.admin.dto;

import com.wordonline.admin.entity.magic.Card;
import com.wordonline.admin.entity.magic.CardType;

public record CardDto(
        long id,
        String name,
        CardType cardType
) {

    public CardDto(Card card){
        this(card.getId(), card.getName(), card.getCardType());
    }
}
