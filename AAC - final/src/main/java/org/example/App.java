package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage palco) throws Exception {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("prancha.fxml"));
        Scene cena = new Scene(loader.load());
        cena.getStylesheets().add(folhaDeEstilo());
        palco.setScene(cena);
        palco.setTitle("Prancha de Comunicação");
        palco.setMinWidth(700);
        palco.setMinHeight(500);
        palco.show();
    }


    public static String folhaDeEstilo() {
        return App.class.getResource("estilo.css").toExternalForm();
    }
}
