package org.vismutFO.klavogonki.server;

import org.vismutFO.klavogonki.protocol.PlayerState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static int allPlayers = 0;

    static private final ExecutorService executeIt = Executors.newFixedThreadPool(1);

    static volatile ArrayList<Team> teams;

    static private ConcurrentLinkedQueue<ServerEvent> eventsToServer;

    static private ConcurrentLinkedQueue<Socket> newClients;

    static private Set<Integer> bannedSockets;

    private static  int findSocketIndexByPlayerId(Team team, int id) {
        for (int j = 0; j < team.sockets.size(); j++) {
            if (id == team.sockets.get(j).getPlayerId()) {
                return j;
            }
        }
        return -1;
    }

    private static int findTeamIndexByPlayerId(int id) {
        if (bannedSockets.contains(id)) {
            return -1;
        }
        for (int i = 0; i < teams.size(); i++) {
            int result = findSocketIndexByPlayerId(teams.get(i), id);
            if (result != -1) {
                return i;
            }
        }
        throw new RuntimeException("Unavailable id in findTeamIndexByPlayerId, id: " + id);
    }

    public static void main(String[] args) {
        assert(args.length < 2);
        int port;
        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
            assert(port >= 1024 && port < 49152);
        }
        else {
            port = 5619;
        }

        bannedSockets = new HashSet<>();

        newClients = new ConcurrentLinkedQueue<>();

        SocketFactory gameServer = new SocketFactory(port, newClients);

        teams = new ArrayList<>();
        eventsToServer = new ConcurrentLinkedQueue<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Server socket created, command console reader for listen to server commands");
            executeIt.execute(gameServer);

            while (true) {

                if (br.ready()) {
                    System.out.println("Main Server found some messages in channel");

                    String serverCommand = br.readLine();
                    if (serverCommand.equalsIgnoreCase("exit")) {
                        System.out.println("Main Server initiate exiting...");
                        gameServer.stop();
                        executeIt.shutdown();
                        break;
                    }
                }

                Socket newClient = newClients.poll();

                if (newClient != null) {
                    System.out.println("Have new player!");
                    boolean haveFinished = false;
                    for (Team team : teams) {
                        if (team.isAvailable()) {
                            team.addPlayer(newClient, eventsToServer, allPlayers++);
                            haveFinished = true;
                            break;
                        }
                    }
                    if (!haveFinished) {
                        teams.add(new Team());
                        teams.get(teams.size() - 1).addPlayer(newClient, eventsToServer, allPlayers++);
                    }
                }

                ArrayList<Team> teamsToDrop = new ArrayList<>();
                for (Team team : teams) {
                    if (team.isNotAlive()) {
                        System.out.println("-----------------------");
                        System.out.println("All players leaved team");
                        System.out.println("-----------------------");
                        team.stop();
                        teamsToDrop.add(team);
                        continue;
                    }
                    if (!team.isReady && (team.getSize() == 3 || Instant.now().minusSeconds(30).isAfter(team.beginConnect))) {
                        System.out.println("-----------------------");
                        System.out.println("new team is ready");
                        System.out.println("-----------------------");
                        PlayerState temp = new PlayerState(-1);
                        temp.type = PlayerState.SERVER_START_TEAM;
                        team.send(new ArrayList<>(List.of(temp)));
                        team.isReady = true;
                    }
                    if (team.isReady) {
                        if (!team.isStarted) {
                            if (Instant.now().minusSeconds(5).isAfter(team.beginConnect)) {
                                System.out.println("-----------------------");
                                System.out.println("new team started");
                                System.out.println("-----------------------");
                                PlayerState temp = new PlayerState(-1);
                                temp.type = PlayerState.SERVER_BEGIN_GAME;
                                temp.playerName = "Text for typing.";
                                team.send(new ArrayList<>(List.of(temp)));
                                team.isStarted = true;
                                team.textSize = temp.playerName.length();
                                team.send(team.playerStates);
                                team.lastUpdate = Instant.now();
                            }
                        }
                        if (team.isFinished()) {
                            System.out.println("-----------------------");
                            System.out.println("Game finished!");
                            System.out.println("-----------------------");
                            for (PlayerState state : team.playerStates) {
                                state.type = PlayerState.SERVER_END_GAME;
                            }
                            team.send(team.playerStates);
                            for (PlayerThread s : team.sockets) {
                                bannedSockets.add(s.getPlayerId());
                            }
                            Thread.sleep(500);
                            team.send(null);
                            team.stop();
                            teamsToDrop.add(team);
                        }
                    }
                    if (Instant.now().minusMillis(850).isAfter(team.lastUpdate)) {
                        System.out.println("send update");
                        team.send(team.playerStates);
                        team.lastUpdate = Instant.now();
                        // send always
                    }
                }
                for (Team team : teamsToDrop) {
                    if(!teams.remove(team)) {
                        System.out.println("-----------------------");
                        System.out.println("Couldn't erase team");
                        System.out.println("-----------------------");
                    }
                    else {
                        System.out.println("-----------------------");
                        System.out.println("Erased team");
                        System.out.println("-----------------------");
                    }
                }

                // обработка событий от клиентов
                while (true) {
                    ServerEvent srcForClientEvents = eventsToServer.poll();
                    if (srcForClientEvents == null) {
                        //System.out.println("server events ended for now");
                        break;
                    }
                    System.out.println("new server event");


                    ArrayList<PlayerState> clientEvents = PlayerState.getStates(srcForClientEvents.content);
                    if (clientEvents.size() != 1) {
                        throw new RuntimeException("Incorrect size of clientEvents");
                    }
                    PlayerState clientEvent = clientEvents.get(0);
                    clientEvent.playerId = srcForClientEvents.playerId;

                    int i = findTeamIndexByPlayerId(clientEvent.playerId);
                    if (i == -1) {
                        continue;
                    }
                    int j = findSocketIndexByPlayerId(teams.get(i), clientEvent.playerId);
                    if (clientEvent.type == PlayerState.CLIENT_DISCONNECTED) {
                        teams.get(i).playerStates.get(j).isDisconnected = true;
                        continue;
                    }
                    if (clientEvent.type != PlayerState.CLIENT_UPDATE) {
                        throw new RuntimeException("Incorrect type of clientEvent");
                    }
                    teams.get(i).playerStates.get(j).playerName = clientEvent.playerName;
                    teams.get(i).playerStates.get(j).errors = clientEvent.errors;
                    teams.get(i).playerStates.get(j).symbols = clientEvent.symbols;
                    System.out.println("Server updated PlayerState " + clientEvent.playerName + clientEvent.playerId);

                }
            }
            executeIt.shutdown();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}