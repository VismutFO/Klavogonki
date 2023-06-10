package org.vismutFO.klavogonki.protocol;

import java.io.Serializable;

public class PlayerState implements Serializable {

    final public static int SERVER_START_TEAM = 0;
    final public static int SERVER_BEGIN_GAME = 1;
    final public static int SERVER_END_GAME = 2;
    final public static int SERVER_UPDATE = 3;
    final public static int CLIENT_UPDATE = 4;
    final public static int CLIENT_DISCONNECTED = 5;
    final public static int DEFAULT = 6;

    public int type;
    public int playerId;
    public int errors;
    public int symbols;
    public String playerName;
    public int status; // 0 - default, 1 - thisPlayer, 2 - disconnected

    public int secondsUntil;

    public PlayerState(int playerId) {
        type = DEFAULT;
        this.playerId = playerId;
        playerName = "";
        errors = 0;
        symbols = 0;
        status = 0;
        secondsUntil = 0;
    }

    public PlayerState(PlayerState other) {
        type = other.type;
        playerId = other.playerId;
        playerName = other.playerName;
        errors = other.errors;
        symbols = other.symbols;
        status = other.status;
        secondsUntil = other.secondsUntil;
    }
}
