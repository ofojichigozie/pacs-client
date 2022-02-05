package app;

import app.controllers.DetailsController;
import app.controllers.StartController;
import app.services.ApiUtils;
import app.services.WebService;
import app.utils.Store;
import com.google.gson.JsonObject;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception{
        Scene scene = new Scene(new StackPane());
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setTitle("PACS CLIENT");
        stage.show();

        loadStartWindow(scene);
    }

    private void loadStartWindow(Scene scene) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("views/start.fxml"));
            StackPane pane = fxmlLoader.load();
            StartController startController = fxmlLoader.<StartController>getController();
            scene.setRoot(pane);

            // Load screen with fade in effect
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(5), pane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setCycleCount(1);

            fadeIn.play();

            fadeIn.setOnFinished((e) -> {
                // Client access key
                String accessKey = "e644a9334b4ba07aeee0541a44db377dab7be42f";

                // Declare a reference for WebService
                WebService webService = ApiUtils.getWebService();
                Call<JsonObject> call = webService.authenticate(accessKey);
                call.enqueue(new Callback<JsonObject>() {

                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                        if(response.isSuccessful()) {

                            // Get the returned JSON object
                            JsonObject jsonObject = response.body();

                            if(jsonObject.has("token")) {
                                // Create a preference object to store authentication token
                                Preferences prefs = Store.getPreferences();
                                prefs.put("token", jsonObject.get("token").getAsString());

                                Platform.runLater(() -> {
                                    loadMainWindow(scene);
                                });
                            }else {
                                Platform.runLater(() -> {
                                    startController.setInfo("Authentication failed", "Couldn't establish secure connection");
                                });
                            }

                        }else {
                            Platform.runLater(() -> {
                                startController.setInfo("Authentication failed", "Internal server error");
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable throwable) {
                        Platform.runLater(() -> {
                            startController.setInfo("Authentication failed", "Error: " + throwable.getMessage());
                        });
                    }
                });
            });
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadMainWindow(Scene scene) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("views/register.fxml"));
            Parent root = fxmlLoader.load();
            scene.setRoot(root);
        } catch (IOException ex) {
            Logger.getLogger(DetailsController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
