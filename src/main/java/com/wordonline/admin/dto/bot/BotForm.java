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
    // 접대 봇 표시. enabled와 별개다 -- 접대 봇도 실제 세션을 뛰므로 enabled는 true로 남는다.
    private boolean hospitality = false;
    private short mmr = 1000;
    private String status = "Online";
    private String deckName = "Bot Deck";
}
