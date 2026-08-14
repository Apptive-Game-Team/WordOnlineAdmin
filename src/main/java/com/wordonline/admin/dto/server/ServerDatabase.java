package com.wordonline.admin.dto.server;

/**
 * Which database a server row came from. Server ids collide between the two databases,
 * so the dashboard needs this to match a session count to the card that shows it.
 */
public enum ServerDatabase {
    PRIMARY,
    SECONDARY
}
