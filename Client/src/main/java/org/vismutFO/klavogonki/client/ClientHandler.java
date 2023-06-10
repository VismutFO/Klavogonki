package org.vismutFO.klavogonki.client;

import javafx.application.Platform;
import org.vismutFO.klavogonki.protocol.PlayerState;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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

                ArrayList<PlayerState> event = new ArrayList<>(List.of(stateForSending));
                try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                     ObjectOutputStream objectOut = new ObjectOutputStream(byteOut)) {
                    objectOut.writeObject(event);
                    objectOut.flush();
                    objectOut.close();

                    out.writeInt(byteOut.toByteArray().length);
                    out.write(byteOut.toByteArray());
                    out.flush();
                }
                catch (SocketException e) {
                    System.out.println("SocketException from server1");
                    throw new RuntimeException(e);
                }

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
                    catch (SocketException e) {
                        System.out.println("SocketException from server2");
                        break;
                    }
                }
                catch (SocketTimeoutException e) {
                    System.out.println("Server disconnected");
                    break;
                }
                if (entry.isEmpty()) {
                    throw new RuntimeException("Server response is empty!");
                }
                ArrayList<PlayerState> entryCopy = new ArrayList<>(entry.size());
                for (PlayerState temp : entry) {
                    entryCopy.add(new PlayerState(temp));
                }
                if (!eventsToClient.offer(entryCopy)) {
                    throw new RuntimeException("Can't place server response to queue");
                }
                Platform.runLater(parent::callFromHandler);
                System.out.println("Sent update to client");
                if (entry.get(0).type == PlayerState.SERVER_END_GAME) {
                    break;
                }
                Thread.sleep(500);
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
