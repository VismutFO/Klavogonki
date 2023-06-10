#!/bin/bash
java --module-path $(PWD)"/client/target/lib" --add-modules javafx.controls,javafx.fxml -jar $(PWD)"/client/target/client-1.0-SNAPSHOT.jar" $1 $2