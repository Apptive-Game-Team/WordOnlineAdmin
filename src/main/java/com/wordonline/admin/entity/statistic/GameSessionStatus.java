package com.wordonline.admin.entity.statistic;

// Lifecycle state of a statistic_game_sessions row. A row that stays IN_PROGRESS long
// after its started_at means the hosting game server process died without cleaning up.
public enum GameSessionStatus {
    IN_PROGRESS,
    COMPLETED,
    DRAW,
    ABANDONED
}
