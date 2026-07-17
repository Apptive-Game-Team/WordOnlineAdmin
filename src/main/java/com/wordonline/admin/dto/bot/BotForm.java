package com.wordonline.admin.dto.bot;

import lombok.Data;

@Data
public class BotForm {
    private String name;
    private String tier = "BEGINNER";
    private int thinkingTimeMs = 250;
    private int reactionIntervalFrames = 8;
    private double counterAggression = 0.25;
    private boolean enabled = true;
    private short mmr = 1000;
    private String status = "Online";
    private String deckName = "Bot Deck";
}
