package org.vismutFO.klavogonki.server;

import org.vismutFO.klavogonki.protocol.PlayerState;

import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Team {
    final ArrayList<PlayerThread> sockets;
    private final ArrayList<ConcurrentLinkedQueue<String>> eventsToClients;
    private final ExecutorService executeIt;

    final ArrayList<PlayerState> playerStates;
    boolean isReady;
    boolean isStarted;

    Instant beginConnect, lastUpdate;

    int textSize;

    Team() {
        sockets = new ArrayList<>();
        eventsToClients = new ArrayList<>();
        playerStates = new ArrayList<>();
        isReady = false;
        isStarted = false;
        textSize = -1;
        executeIt = Executors.newFixedThreadPool(3);
    }

    boolean isAvailable() {
        return !isReady && (sockets.size() < 3);
    }

    boolean isFinished() {
        if (Instant.now().minusSeconds(185).isAfter(beginConnect)) {
            return true;
        }
        for (PlayerState state : playerStates) {
            if (state.symbols != textSize) {
                return false;
            }
        }
        return true;
    }

    boolean isNotAlive() {
        for (PlayerState player : playerStates) {
            if (!player.isDisconnected) {
                return false;
            }
        }
        return true;
    }

    void addPlayer(Socket playerSocket, ConcurrentLinkedQueue<ServerEvent> eventsToServer, int id) {
        ConcurrentLinkedQueue<String> eventsToClients = new ConcurrentLinkedQueue<>();
        this.eventsToClients.add(eventsToClients);
        PlayerThread playerThread = new PlayerThread(playerSocket, eventsToClients, eventsToServer, id);
        sockets.add(playerThread);
        executeIt.execute(playerThread);
        PlayerState state = new PlayerState(id);
        state.type = PlayerState.SERVER_UPDATE;
        playerStates.add(state);
        if (sockets.size() == 1) {
            beginConnect = Instant.now();
            lastUpdate = Instant.now().minusSeconds(1);
        }
    }

    int getSize() {
        return sockets.size();
    }

    void dropDisconnectedPlayers() {
        for (int i = 0; i < sockets.size(); i++) {
            while (i < sockets.size() && playerStates.get(i).isDisconnected) {
                if (!eventsToClients.get(i).offer("")) {
                    throw new RuntimeException("Can't put end message to client " + sockets.get(i).getPlayerId());
                }
                sockets.remove(i);
                playerStates.remove(i);
                eventsToClients.remove(i);
            }
        }
    }

    void send(ArrayList<PlayerState> message) {
        assert(sockets.size() == eventsToClients.size());
        for (int i = 0; i < sockets.size(); i++) {
            if (playerStates.get(i).isDisconnected) {
                continue;
            }
            if (message == null) {
                if (!eventsToClients.get(i).offer("")) {
                    throw new RuntimeException("Can't put end message to client " + sockets.get(i).getPlayerId());
                }
                continue;
            }
            for (PlayerState temp : message) {
                if (temp.playerId == sockets.get(i).getPlayerId()) {
                    temp.isThisPlayer = true;
                }
            }
            if (!eventsToClients.get(i).offer(PlayerState.getSource(message))) {
                throw new RuntimeException("Can't put message to client " + sockets.get(i).getPlayerId());
            }
            System.out.println("team putted message");
            for (PlayerState temp : message) {
                temp.isThisPlayer = false;
            }
        }
    }
    void stop() {
        executeIt.shutdown();
        playerStates.clear();
        sockets.clear();
        eventsToClients.clear();
    }
}
