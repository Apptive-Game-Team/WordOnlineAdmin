package com.wordonline.admin.dto.bot;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BotDeckForm {
    private String deckName;
    private List<Long> cardIds = new ArrayList<>();
    private List<Integer> counts = new ArrayList<>();
}
