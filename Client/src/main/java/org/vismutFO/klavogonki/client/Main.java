package org.vismutFO.klavogonki.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.vismutFO.klavogonki.protocol.PlayerState;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {

    private ExecutorService executeIt = Executors.newFixedThreadPool(1);
    private boolean inGame;

    private ConcurrentLinkedQueue<ArrayList<PlayerState>> eventsToClient;
    private ConcurrentLinkedQueue<PlayerState> eventsFromClient;

    private Scene startScene, gameScene;

    private ArrayList<PlayerState> playerStates; // for table in gameScene

    private Text textForTyping, timerToEnd, playersTable, errorMessage;

    private TextArea panel;

    private Instant timeBeginGame;
    private PlayerState stateForSending;

    private Button restart;


    @Override
    public void start(Stage stage) {

        timeBeginGame = null;

        stateForSending = new PlayerState(-2);
        stateForSending.type = PlayerState.CLIENT_UPDATE;
        inGame = false;
        eventsToClient = new ConcurrentLinkedQueue<>();
        eventsFromClient = new ConcurrentLinkedQueue<>();
        playerStates = new ArrayList<>();

        stage.setTitle("Klavogonki");

        initStartScene(stage);
        initGameScene(stage);
        stage.setScene(startScene);
        stage.show();
    }

    void callFromHandler() {
        System.out.println("Listener!");

        while (true) {
            ArrayList<PlayerState> clientEvents = eventsToClient.poll();
            if (clientEvents == null) {
                break;
            }

            if (clientEvents.size() == 0) {
                throw new RuntimeException("haven't states in message from server");
            }
            else {
                PlayerState state = clientEvents.get(0);
                switch (state.type) {
                    case PlayerState.SERVER_UPDATE -> {
                        playerStates = clientEvents;
                        updateUI();
                    }
                    case PlayerState.SERVER_BEGIN_GAME -> {
                        textForTyping.setText(state.playerName);
                        panel.setDisable(false);
                    }
                    case PlayerState.SERVER_START_TEAM -> {
                        timeBeginGame = Instant.now();
                    }
                    case PlayerState.SERVER_END_GAME -> {
                        System.out.println("Client End Game!");

                        if (state.status == 1) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setContentText("Congratulations! You won!");
                            alert.show();
                        }

                        panel.setDisable(true);
                        restart.setDisable(false);
                        playerStates = clientEvents;
                        updateUI();
                        inGame = false;
                        executeIt.shutdown();
                        textForTyping.setText("END.");
                    }
                    default -> {
                        throw new RuntimeException("PlayerState wrong type " + state.type);
                    }
                }
            }
        }
    }

    private void updateUI() {
        playerStates.sort((o1, o2) -> {
            if (o1.symbols != o2.symbols) {
                return (o1.symbols - o2.symbols) * -1;
            }
            return o1.errors - o2.errors;
        });
        int secondsUntil = playerStates.get(0).secondsUntil;
        timerToEnd.setText(secondsUntil + "s");

        String newTable = "";
        for (PlayerState state : playerStates) {
            newTable += state.playerName + " ";
            if (state.status == 1) {
                newTable += "(It is You) ";
            }
            if (state.status == 2) {
                newTable += "(Disconnected) ";
            }
            newTable += (state.symbols * 100 / textForTyping.getText().length()) + "%, ";
            newTable += state.errors + " error(s), ";
            if (timeBeginGame == null || Duration.between(timeBeginGame, Instant.now()).toSeconds() == 0) {
                newTable += "0";
            }
            else {
                newTable += (int) (state.symbols * 60 / Duration.between(timeBeginGame, Instant.now()).toSeconds());
            }
            newTable += "\n";
        }
        playersTable.setText(newTable);
    }

    private void initStartScene(Stage stage) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(5);
        grid.setHgap(5);

        TextField host = new TextField();
        host.setPromptText("localhost");
        host.setPrefColumnCount(15);
        GridPane.setConstraints(host, 0, 1);
        grid.getChildren().add(host);

        TextField port = new TextField();
        port.setPromptText("5619");
        port.setPrefColumnCount(4);
        GridPane.setConstraints(port, 0, 2);
        grid.getChildren().add(port);

        TextField name = new TextField();
        name.setPromptText("Billy");
        name.setPrefColumnCount(20);
        GridPane.setConstraints(name, 0, 3);
        grid.getChildren().add(name);


        Button submit = new Button("Start Game");
        GridPane.setConstraints(submit, 0, 4);
        submit.setOnAction(e -> {
            if (inGame) {
                return;
            }
            inGame = true;
            ClientHandler fileClient;
            try {
                String hostString = host.getText();
                if (hostString == null) {
                    throw  new RuntimeException("Wrong host");
                }
                if (hostString.isEmpty()) {
                    hostString = "localhost";
                }
                String portString = port.getText();
                if (portString == null) {
                    throw  new RuntimeException("Wrong port");
                }
                if (portString.isEmpty()) {
                    portString = "5619";
                }
                String nameString = name.getText();
                if (nameString == null) {
                    throw  new RuntimeException("Wrong name");
                }
                if (nameString.isEmpty()) {
                    nameString = "Billy";
                }
                stateForSending.playerName = nameString;
                fileClient = new ClientHandler(this, hostString, Integer.parseInt(portString),
                        nameString, eventsToClient, eventsFromClient);
            }
            catch (NumberFormatException exception) {
                System.out.println("Incorrect port");
                return;
            }
            executeIt.execute(fileClient);
            stage.setScene(gameScene);
        });
        grid.getChildren().add(submit);

        Button aboutGame = new Button("About Game");
        GridPane.setConstraints(aboutGame, 0, 5);
        aboutGame.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Author: Stepanov Artemiy, BPI211");
            alert.show();
        });
        grid.getChildren().add(aboutGame);

        startScene = new Scene(grid, 1080, 720);
    }

    private void initGameScene(Stage stage) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(5);
        grid.setHgap(5);

        playersTable = new Text("");
        GridPane.setConstraints(playersTable, 0, 0);
        grid.getChildren().add(playersTable);

        timerToEnd = new Text("Timer");
        GridPane.setConstraints(timerToEnd, 1, 0);
        grid.getChildren().add(timerToEnd);

        textForTyping = new Text("Здесь будет текст");
        GridPane.setConstraints(textForTyping, 0, 1);
        grid.getChildren().add(textForTyping);



        panel = new TextArea();
        panel.setDisable(true);
        panel.setPromptText("type here");
        panel.setPrefColumnCount(500);
        panel.textProperty().addListener((observable, oldValue, newValue) -> {
            stateForSending.symbols = 0;
            stateForSending.errors = 0;
            boolean haveError = false;
            for (int i = 0; i < newValue.length() && i < textForTyping.getText().length(); i++) {
                if (newValue.charAt(i) != textForTyping.getText().charAt(i)) {
                    stateForSending.symbols = i;
                    stateForSending.errors = newValue.length() - i - 1;
                    haveError = true;
                    break;
                }
            }
            if (!haveError) {
                stateForSending.symbols = Integer.min(newValue.length(), textForTyping.getText().length());
            }
            if (stateForSending.errors > 0) {
                errorMessage.setText("You have error(s), fix it");
            }
            else {
                errorMessage.setText("");
            }
            PlayerState stateForSendingCopy = new PlayerState(stateForSending);

            if (!eventsFromClient.offer(stateForSendingCopy)) {
                throw new RuntimeException("Can't place client state to queue");
            }
            System.out.println("textfield changed from " + oldValue + " to " + newValue);
        });
        GridPane.setConstraints(panel, 0, 2);
        grid.getChildren().add(panel);

        restart = new Button("New Game");
        GridPane.setConstraints(restart, 0, 3);
        restart.setOnAction(e -> {
            eventsToClient.clear();
            eventsFromClient.clear();
            playerStates.clear();
            timeBeginGame = null;
            timerToEnd.setText("Timer");
            playersTable.setText("");
            textForTyping.setText("Здесь будет текст");
            panel.setDisable(true);
            panel.setText("");
            executeIt = Executors.newFixedThreadPool(1);

            stage.setScene(startScene);
        });
        restart.setDisable(true);
        grid.getChildren().add(restart);

        errorMessage = new Text();
        GridPane.setConstraints(errorMessage, 1, 3);
        grid.getChildren().add(errorMessage);

        gameScene = new Scene(grid, 1080, 720);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        executeIt.shutdown();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch();
    }

}