package com.wordonline.admin.dto;

import com.wordonline.admin.entity.magic.Card;
import com.wordonline.admin.entity.magic.CardType;
import com.wordonline.admin.entity.magic.MagicCard;

public record CardDto(
        long id,
        String name,
        CardType cardType
) {

    public CardDto(MagicCard magicCard){
        this(magicCard.getCard());
    }

    public CardDto(Card card){
        this(card.getId(), card.getName(), card.getCardType());
    }
}
