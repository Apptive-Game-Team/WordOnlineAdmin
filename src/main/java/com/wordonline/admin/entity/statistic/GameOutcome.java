package com.wordonline.admin.entity.statistic;

// How a recorded game ended. Only WIN rows carry win/loss user ids; ABANDONED rows are
// written by the game server's loop watchdog with the data accumulated up to the stall.
public enum GameOutcome {
    WIN,
    DRAW,
    ABANDONED
}
