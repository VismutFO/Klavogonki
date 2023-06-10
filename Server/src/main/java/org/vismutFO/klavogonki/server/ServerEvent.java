package org.vismutFO.klavogonki.server;

import org.vismutFO.klavogonki.protocol.PlayerState;

import java.util.ArrayList;

public class ServerEvent {
    int playerId;
    ArrayList<PlayerState> content;
    ServerEvent(int playerId, ArrayList<PlayerState> content) {
        this.playerId = playerId;
        this.content = new ArrayList<>(content.size());
        for (int i = 0; i < content.size(); i++) {
            this.content.add(new PlayerState(content.get(i)));
        }
    }
}
