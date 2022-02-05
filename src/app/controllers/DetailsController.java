package app.controllers;

import app.models.Image;
import app.models.Patient;
import app.services.ApiUtils;
import app.services.DicomService;
import app.services.WebService;
import app.utils.Token;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DetailsController implements Initializable {

    @FXML private Label accessCodeLabel;
    @FXML private Label firstNameLabel;
    @FXML private Label lastNameLabel;
    @FXML private Label sexLabel;
    @FXML private Label dateOfBirthLabel;
    @FXML private Label modalitiesLabel;
    @FXML private Button uploadImageButton;
    @FXML private VBox imageListVBox;

    private static FileChooser fileChooser = new FileChooser();
    private static File file = null;
    private DicomService dicomService = null;
    private Patient patient;

    // Declare a reference for WebService
    WebService webService = ApiUtils.getWebService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Medical images", "*.dcm"));
    }

    @FXML
    public void goBackToRegisterWindow(ActionEvent event){
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("../views/register.fxml"));
            Parent root = fxmlLoader.load();
            ((Hyperlink)(event.getSource())).getScene().setRoot(root);
        } catch (IOException ex) {
            Logger.getLogger(RegisterController.class.getName()).log(Level.INFO, null, ex);
        }
    }

    @FXML
    public void uploadImageHandler(ActionEvent event){
        try{
            String initDirectory = System.getProperty("user.home") + "\\Pictures";
            if(Files.exists(Paths.get(initDirectory))){
                fileChooser.setInitialDirectory(new File(initDirectory));
            }

            Stage stage = (Stage)uploadImageButton.getParent().getScene().getWindow();
            file = fileChooser.showOpenDialog(stage);

            if (file == null) {
                return;
            }

            Stage waitStage = getWaitStage("Please, wait.\nPerforming LFSR encryption and uploading to PACS server");
            waitStage.show();

            Task<Boolean> loadImageTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {

                    dicomService = new DicomService("encDicoms", "decDicoms");
                    File encFile = dicomService.encrypt(file);

                    if(encFile == null){
                        Platform.runLater(() -> {
                            (new Alert(Alert.AlertType.ERROR, "Unsupported medical image format", ButtonType.CLOSE)).showAndWait();
                        });
                        return false;
                    }

                    RequestBody patientId =
                            RequestBody.create(MediaType.parse("multipart/form-data"), patient.get_id());
                    RequestBody requestFile =
                            RequestBody.create(MediaType.parse("multipart/form-data"), encFile);
                    MultipartBody.Part image =
                            MultipartBody.Part.createFormData("image", encFile.getName(), requestFile);

                    Call<JsonObject> call = webService.savePatientImage(Token.getAuthorizationToken(), patientId, image);
                    call.enqueue(new Callback<JsonObject>() {

                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                            if(response.isSuccessful()) {

                                JsonElement imageJson = response.body().get("data");
                                Gson gson = new Gson();
                                Image image = gson.fromJson(imageJson, Image.class);

                                Platform.runLater(() -> {
                                    imageListVBox.getChildren().add(getImageListViewItem(image));
                                    (new Alert(Alert.AlertType.NONE, "Successfully uploaded dicom image to PACS server", ButtonType.CLOSE)).showAndWait();
                                });
                            }else{
                                Platform.runLater(() -> {
                                    (new Alert(Alert.AlertType.NONE, "Could not upload dicom image to PACS server", ButtonType.CLOSE)).showAndWait();
                                });
                            }
                        }

                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable throwable) {
                            Platform.runLater(() -> {
                                (new Alert(Alert.AlertType.NONE, "ERROR: " + throwable.getMessage(), ButtonType.CLOSE)).showAndWait();
                            });
                        }
                    });


                    return true;
                }
            };

            loadImageTask.setOnSucceeded(e -> {
                Boolean result = loadImageTask.getValue();

                Platform.runLater(() -> {
                    waitStage.close();
                });
            });

            loadImageTask.setOnFailed(e -> {
                Platform.runLater(() -> {
                    waitStage.close();
                    (new Alert(Alert.AlertType.NONE, "Error: " + e.getSource().getMessage(), ButtonType.CLOSE)).showAndWait();
                });
            });

            Thread thread = new Thread(loadImageTask);
            thread.start();


        }catch(Exception error){

        }
    }

    public void setPatient(Patient patient){
        this.patient = patient;

        accessCodeLabel.setText(patient.getAccessCode());
        firstNameLabel.setText(patient.getFirstName());
        lastNameLabel.setText(patient.getLastName());
        sexLabel.setText(patient.getSex());
        dateOfBirthLabel.setText(patient.getDateOfBirth());
        modalitiesLabel.setText(patient.getModalities());

        // Retrieve medical images for this client
        retrieveImages(patient.get_id());
    }

    private void retrieveImages(String patientId){
        Call<JsonObject> call = webService.retrievePatientImages(Token.getAuthorizationToken(), patientId);
        call.enqueue(new Callback<JsonObject>() {

            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                if(response.isSuccessful()) {

                    // Get the returned JSON object
                    JsonObject jsonObject = response.body();

                    if(jsonObject.has("data")) {
                        JsonElement jsonElement = jsonObject.get("data");

                        Gson gson = new Gson();
                        Type userListType = new TypeToken<ArrayList<Image>>(){}.getType();
                        ArrayList<Image> images = gson.fromJson(jsonElement, userListType);

                        Platform.runLater(() -> {
                            for(Image image : images){
                                imageListVBox.getChildren().add(getImageListViewItem(image));
                            }
                        });
                    }
                }else{
                    Platform.runLater(() -> {
                        (new Alert(Alert.AlertType.NONE, "Could not retrieve dicom image from PACS server", ButtonType.CLOSE)).showAndWait();
                    });
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable throwable) {
                Platform.runLater(() -> {
                    (new Alert(Alert.AlertType.NONE, "ERROR: " + throwable.getMessage(), ButtonType.CLOSE)).showAndWait();
                });
            }
        });
    }

    private HBox getImageListViewItem(Image image){
        Label imageFileNameLabel = new Label(image.getFilename());
        Label timeStampLabel = new Label("[" + image.getTimeStamp() + "]");

        HBox hBox = new HBox();
        hBox.setSpacing(10);
        hBox.setPadding(new Insets(5));
        hBox.setCursor(Cursor.HAND);
        hBox.setStyle("-fx-background-color: #ffffff;");

        hBox.getChildren().addAll(imageFileNameLabel, timeStampLabel);

        hBox.setOnMouseClicked(e -> {
            loadImageViewModal(image);
        });

        return hBox;
    }

    private void loadImageViewModal(Image image){

        Stage waitStage = getWaitStage("Please, wait.\nRetrieving and decrypting DICOM file");
        Stage imageStage = getImageStage();

        waitStage.show();

        Task<Scene> loadImageTask = new Task<Scene>() {
            @Override
            protected Scene call() throws Exception {

                // Decrypt and create a file object from DICOM image URL
                URL url = new URL(image.getUrl());
                dicomService = new DicomService("encDicoms", "decDicoms");
                File inputFile = dicomService.getFileFromURL(url);
                File decryptedFile = dicomService.decrypt(inputFile);

                File[] files = dicomService.convertDcmToImageFile(decryptedFile);
                javafx.scene.image.Image image = new javafx.scene.image.Image(files[files.length - 1].toURI().toString());
                ImageView imageView = new ImageView(image);

                /*BufferedImage bufferedImage = dicomService.convertDcmToImage(decryptedFile);
                javafx.scene.image.Image pngImage = SwingFXUtils.toFXImage(bufferedImage, null);
                ImageView imageView = new ImageView(pngImage);*/

                StackPane stackPane = new StackPane(imageView);

                Scene scene = new Scene(stackPane);
                scene.setFill(Color.BLACK);

                imageView.fitWidthProperty().bind(scene.widthProperty());
                imageView.setPreserveRatio(true);

                return scene;
            }
        };

        loadImageTask.setOnSucceeded(e -> {
            Scene scene = loadImageTask.getValue();

            Platform.runLater(() -> {
                waitStage.close();
                imageStage.setScene(scene);
                imageStage.showAndWait();
            });
        });

        loadImageTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                waitStage.close();
                (new Alert(Alert.AlertType.NONE, "Error: " + e.getSource().getMessage(), ButtonType.CLOSE)).showAndWait();
            });
        });

        Thread thread = new Thread(loadImageTask);
        thread.start();
    }

    private Stage getImageStage(){
        Stage stage = new Stage();
        stage.setWidth(450);
        stage.setHeight(550);
        stage.centerOnScreen();
        stage.setResizable(false);
        stage.setTitle("Dicom Viewer");
        stage.initModality(Modality.APPLICATION_MODAL);

        return stage;
    }

    private Stage getWaitStage(String text){
        Label label = new Label(text);
        label.setTextAlignment(TextAlignment.CENTER);
        StackPane stackPane = new StackPane(label);

        Scene scene = new Scene(stackPane);
        scene.setFill(Color.BLACK);

        Stage stage = new Stage();
        stage.setWidth(400);
        stage.setHeight(200);
        stage.centerOnScreen();
        stage.setResizable(false);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);

        return stage;
    }
}
