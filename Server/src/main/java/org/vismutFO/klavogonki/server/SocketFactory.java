package org.vismutFO.klavogonki.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SocketFactory implements Runnable {

    final int port;
    private final ServerSocket server;

    private final ConcurrentLinkedQueue<Socket> eventsToServer;

    SocketFactory(int portMain, ConcurrentLinkedQueue<Socket> events) {
        port = portMain;
        eventsToServer = events;
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (!server.isClosed()) {
            try {
                System.out.println("GameServer trying to get socket");
                Socket client = server.accept();
                client.setSoTimeout(2000);
                if(!eventsToServer.offer(client)) {
                    throw new RuntimeException("Can't put new client to queue");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    synchronized void stop() {
        try {
            server.close();
        } catch (IOException e) {
            throw new RuntimeException("Error while closing server", e);
        }
    }
}
