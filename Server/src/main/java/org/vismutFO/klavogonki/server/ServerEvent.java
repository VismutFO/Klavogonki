package org.vismutFO.klavogonki.server;

public class ServerEvent {
    int playerId;
    String content;
    ServerEvent(int playerId, String content) {
        this.playerId = playerId;
        this.content = content;
    }
}
