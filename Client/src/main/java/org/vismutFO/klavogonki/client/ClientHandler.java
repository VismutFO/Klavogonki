package org.vismutFO.klavogonki.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import org.vismutFO.klavogonki.protocol.PlayerState;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientHandler implements Runnable {
    private final String host;
    private final int port;

    private final ConcurrentLinkedQueue<ArrayList<PlayerState>> eventsToClient;
    private final ConcurrentLinkedQueue<PlayerState> eventsFromClient;

    private PlayerState stateForSending;

    private final Main parent;


    public ClientHandler(Main parent, String host, int port, String playerName,
                         ConcurrentLinkedQueue<ArrayList<PlayerState>> eventsToClient,
                         ConcurrentLinkedQueue<PlayerState> eventsFromClient) {
        this.parent = parent;
        this.host = host;
        this.port = port;
        this.eventsToClient = eventsToClient;
        this.eventsFromClient = eventsFromClient;
        stateForSending = new PlayerState(-2);
        stateForSending.type = PlayerState.CLIENT_UPDATE;
        stateForSending.playerName = playerName;
    }

    private void printJSON(String source) {
        System.out.println("-----------------------");
        System.out.println(source);
        System.out.println("-----------------------");
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            socket.setSoTimeout(2000);

            while (!socket.isClosed()) {
                while(!eventsFromClient.isEmpty()) {
                    stateForSending = eventsFromClient.poll();
                }

                String source = PlayerState.getSource(new ArrayList<>(List.of(stateForSending)));
                printJSON(source);
                out.writeUTF(source);
                out.flush();

                System.out.println("Server reading from channel");
                String entry;
                try {
                    entry = in.readUTF();
                }
                catch (SocketTimeoutException e) {
                    System.out.println("Server disconnected");
                    break;
                }
                printJSON(entry);
                ArrayList<PlayerState> list = PlayerState.getStates(entry);
                if (list.isEmpty()) {
                    throw new RuntimeException("Server response is empty!");
                }
                if (!eventsToClient.offer(list)) {
                    throw new RuntimeException("Can't place server response to queue");
                }
                Platform.runLater(parent::callFromHandler);
                System.out.println("Sent update to client");
                if (list.get(0).type == PlayerState.SERVER_END_GAME) {
                    break;
                }
                Thread.sleep(500);
                /*
                ArrayList<PlayerState> result = PlayerState.getStates(entry);
                if (result.size() != 1) {
                    System.out.println("Server response wrong size?");
                }
                else {
                    switch (result.get(0).type) {
                        case PlayerState.SERVER_START_TEAM -> {
                            System.out.println("New Team!");
                            System.out.println("Text: " + result.get(0).playerName);
                        }
                        case PlayerState.SERVER_BEGIN_GAME -> {
                            System.out.println("New Game!");
                            gameStarted = true;
                        }
                        case PlayerState.SERVER_END_GAME -> {
                            System.out.println("End Game.");
                            endGame = true;
                        }
                        case PlayerState.SERVER_UPDATE -> {
                            System.out.println("Game update");
                        }
                        default -> {
                            System.out.println("Something wrong!");
                        }
                    }
                }
                if (endGame) {
                    break;
                }

                if (gameStarted) {
                    Thread.sleep(900);
                    stateForSending.symbols++;
                    stateForSending.errors++;
                }
                */
            }
            in.close();
            out.close();

            socket.close();

            System.out.println("Closing connections & channels - DONE.");
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
