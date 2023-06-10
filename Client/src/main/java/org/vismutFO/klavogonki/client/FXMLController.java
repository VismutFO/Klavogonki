package org.vismutFO.klavogonki.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;

public class FXMLController {

    @FXML
    private Label label;

    private static Scene startScene, gameScene;
    private static boolean inGame;

    public static void initialize(Scene _startScene, Scene _gameScene) {
        startScene = _startScene;
        gameScene = _gameScene;
        inGame = false;
    }

    @FXML
    private void handleStartButtonAction(ActionEvent event) {

    }

}
