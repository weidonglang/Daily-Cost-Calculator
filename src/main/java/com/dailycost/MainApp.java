package com.dailycost;

import com.dailycost.model.AppData;
import com.dailycost.storage.JsonDataStore;
import com.dailycost.storage.StorageException;
import com.dailycost.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class MainApp extends Application {
    public static final String APP_TITLE = "设备日均资费计算器";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        JsonDataStore dataStore = new JsonDataStore();
        AppData appData = new AppData();
        String startupError = null;
        try {
            appData = dataStore.load();
        } catch (StorageException e) {
            startupError = e.getMessage();
        }

        MainView mainView = new MainView(appData, dataStore);

        Scene scene = new Scene(mainView.getRoot(), 1400, 900);
        scene.getStylesheets().add(resource("/app.css"));
        mainView.attachScene(scene);

        stage.setTitle(APP_TITLE);
        stage.setMinWidth(1080);
        stage.setMinHeight(720);
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();

        if (startupError != null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("数据读取失败");
            alert.setHeaderText("无法读取本地数据");
            alert.setContentText(startupError);
            alert.showAndWait();
        }
    }

    private String resource(String path) {
        return getClass().getResource(path).toExternalForm();
    }
}
