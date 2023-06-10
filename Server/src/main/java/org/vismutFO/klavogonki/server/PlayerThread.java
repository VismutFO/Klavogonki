package org.vismutFO.klavogonki.server;

import org.vismutFO.klavogonki.protocol.PlayerState;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PlayerThread implements Runnable {
    private final int playerId;
    private final Socket clientDialog;

    private final ConcurrentLinkedQueue<String> eventsToClients;
    private final ConcurrentLinkedQueue<ServerEvent> eventsToServer;
    volatile boolean gameStarted;
    volatile boolean clientAvailable;

    public PlayerThread(Socket client, ConcurrentLinkedQueue<String> eventsToClients,
                        ConcurrentLinkedQueue<ServerEvent> eventsToServer, int id) {
        playerId = id;
        clientDialog = client;
        this.eventsToClients = eventsToClients;
        this.eventsToServer = eventsToServer;
        gameStarted = false;
        clientAvailable = true;
    }

    public int getPlayerId() {
        return playerId;
    }

    @Override
    public void run() {

        try(clientDialog;
            DataOutputStream out = new DataOutputStream(clientDialog.getOutputStream());
            DataInputStream in = new DataInputStream(clientDialog.getInputStream())) {


            System.out.println("DataInputStream created");
            System.out.println("DataOutputStream  created");

            while (!clientDialog.isClosed()) {
                System.out.println("Server reading from channel");
                String entry;
                try {
                    entry = in.readUTF();
                }
                catch (SocketException | SocketTimeoutException e) {
                    //in.close();
                    //out.close();
                    //clientDialog.close();
                    System.out.println("SocketException or SocketTimeoutException from client");
                    PlayerState temp = new PlayerState(playerId);
                    temp.type = PlayerState.CLIENT_DISCONNECTED;
                    if(!eventsToServer.offer(new ServerEvent(playerId, PlayerState.getSource(new ArrayList<>(List.of(temp)))))) {
                        throw new RuntimeException("Can't place client response to queue");
                    }
                    break;
                    //return;
                }
                if(!eventsToServer.offer(new ServerEvent(playerId, entry))) {
                    throw new RuntimeException("Can't place client response to queue");
                }
                try {
                    Thread.sleep(880);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                boolean needToExit = false;
                while (true) {
                    System.out.println("PlayerThread trying to get string");
                    String event = eventsToClients.poll();

                    if (event == null) {
                        break;
                    }
                    System.out.println("PlayerThread got string");
                    if (event.equals("")) {
                        System.out.println("PlayerThread got empty message");
                        Thread.sleep(200);
                        needToExit = true;
                        break;
                    }
                    System.out.println("PlayerThread send message");
                    try {
                        out.writeUTF(event);
                    } catch (SocketException e) {
                        System.out.println("SocketException from client");
                        PlayerState temp = new PlayerState(playerId);
                        temp.type = PlayerState.CLIENT_DISCONNECTED;
                        if (!eventsToServer.offer(new ServerEvent(playerId, PlayerState.getSource(new ArrayList<>(List.of(temp)))))) {
                            throw new RuntimeException("Can't place client response to queue");
                        }
                        needToExit = true;
                        break;
                    }
                }
                if (needToExit) {
                    break;
                }
            }
            in.close();
            out.close();

            clientDialog.close();

            System.out.println("Closing connections & channels - DONE.");
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
