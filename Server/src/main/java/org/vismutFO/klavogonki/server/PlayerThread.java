package org.vismutFO.klavogonki.server;

import org.vismutFO.klavogonki.protocol.PlayerState;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PlayerThread implements Runnable {
    private final int playerId;
    private final Socket clientDialog;

    private final ConcurrentLinkedQueue<ArrayList<PlayerState>> eventsToClients;
    private final ConcurrentLinkedQueue<ServerEvent> eventsToServer;
    volatile boolean gameStarted;
    volatile boolean clientAvailable;

    public PlayerThread(Socket client, ConcurrentLinkedQueue<ArrayList<PlayerState>> eventsToClients,
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
                ArrayList<PlayerState> entry;
                try {
                    int length = in.readInt();
                    try (ByteArrayInputStream byteIn = new ByteArrayInputStream(in.readNBytes(length));
                    ObjectInputStream objectIn = new ObjectInputStream(byteIn)) {
                        entry = (ArrayList<PlayerState>) objectIn.readObject();
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                catch (SocketException | SocketTimeoutException e) {
                    System.out.println("SocketException or SocketTimeoutException from client");
                    PlayerState temp = new PlayerState(playerId);
                    temp.type = PlayerState.CLIENT_DISCONNECTED;
                    if(!eventsToServer.offer(new ServerEvent(playerId, new ArrayList<>(List.of(temp))))) {
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
                    ArrayList<PlayerState> event = eventsToClients.poll();

                    if (event == null) {
                        break;
                    }
                    System.out.println("PlayerThread got string");
                    if (event.isEmpty()) {
                        System.out.println("PlayerThread got empty message");
                        Thread.sleep(200);
                        needToExit = true;
                        break;
                    }
                    System.out.println("PlayerThread send message");
                    if (event.get(0).type == PlayerState.SERVER_UPDATE && event.get(0).status != 1) {
                        System.out.println("AAAAAAAAAAAAAAAAA" + event.get(0).playerName + event.get(0).playerId);
                    }
                    try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                         ObjectOutputStream objectOut = new ObjectOutputStream(byteOut)) {
                        objectOut.writeObject(event);
                        objectOut.flush();
                        objectOut.close();

                        out.writeInt(byteOut.toByteArray().length);
                        out.write(byteOut.toByteArray());
                    } catch (SocketException e) {
                        System.out.println("SocketException from client");
                        PlayerState temp = new PlayerState(playerId);
                        temp.type = PlayerState.CLIENT_DISCONNECTED;
                        if (!eventsToServer.offer(new ServerEvent(playerId, new ArrayList<>(List.of(temp))))) {
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
