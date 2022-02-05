package app.controllers;

import app.models.Patient;
import app.services.ApiUtils;
import app.services.DicomService;
import app.services.WebService;
import app.utils.Token;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegisterController implements Initializable {

    @FXML private TextField searchTextField;
    @FXML private Button searchButton;
    @FXML private TextField firstNameTextField;
    @FXML private TextField lastNameTextField;
    @FXML private ComboBox<String> sexComboBox;
    @FXML private DatePicker dateOfBirthPicker;
    @FXML private CheckBox crCheckBox;
    @FXML private CheckBox ctCheckBox;
    @FXML private CheckBox mrCheckBox;
    @FXML private CheckBox nmCheckBox;
    @FXML private CheckBox ptCheckBox;
    @FXML private Button submitButton;
    @FXML private Button waitButton;

    // Declare a reference for WebService
    WebService webService = ApiUtils.getWebService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Boolean binding for submit button
        BooleanBinding searchButtonBooleanBinding = searchTextField.textProperty().isEmpty();
        searchButton.disableProperty().bind(searchButtonBooleanBinding);

        waitButton.setVisible(false);

        // Boolean binding for submit button
        BooleanBinding submitButtonBooleanBinding =
                firstNameTextField.textProperty().isEmpty()
                .or(lastNameTextField.textProperty().isEmpty())
                .or(sexComboBox.getSelectionModel().selectedIndexProperty().lessThan(0))
                .or(dateOfBirthPicker.valueProperty().isNull());
        submitButton.disableProperty().bind(submitButtonBooleanBinding);

        // Populate sex combo box with items
        final String[] sexArray = {
                "Male",
                "Female",
        };
        ObservableList<String> observableSex = FXCollections.observableArrayList(sexArray);
        sexComboBox.setItems(observableSex);
        sexComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    public void searchPatientHandler(ActionEvent event){
        String accessCode = searchTextField.getText().trim().toLowerCase();

        Call<JsonObject> call = webService.getPatient(Token.getAuthorizationToken(), accessCode);
        call.enqueue(new Callback<JsonObject>() {

            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                if(response.isSuccessful()) {
                    JsonElement patientJson = response.body().get("data");
                    Gson gson = new Gson();
                    Patient patient = gson.fromJson(patientJson, Patient.class);

                    Platform.runLater(() -> {
                        if(patient != null){
                            loadDetailsWindow(event, patient);
                        }else{
                            (new Alert(Alert.AlertType.NONE, "No patient with the access code", ButtonType.CLOSE)).showAndWait();
                        }
                    });
                }else{
                    Platform.runLater(() -> {
                        (new Alert(Alert.AlertType.NONE, "Could not get patient from PACS server", ButtonType.CLOSE)).showAndWait();
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

    @FXML
    public void addPatientHandler(ActionEvent event){
        String firstName = firstNameTextField.getText().trim();
        String lastName = lastNameTextField.getText().trim();
        String sex = sexComboBox.getSelectionModel().getSelectedItem();
        String dateOfBirth = dateOfBirthPicker.getValue().toString();
        String modalities = getModalities().trim();

        if(modalities.isEmpty()){
            return;
        }

        submitButton.setVisible(false);
        waitButton.setVisible(true);

        Call<JsonObject> call = webService.addPatient(Token.getAuthorizationToken(), firstName, lastName, sex, dateOfBirth, modalities);
        call.enqueue(new Callback<JsonObject>() {

            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                if(response.isSuccessful()) {

                    JsonElement patientJson = response.body().get("data");
                    Gson gson = new Gson();
                    Patient patient = gson.fromJson(patientJson, Patient.class);

                    Platform.runLater(() -> {
                        loadDetailsWindow(event, patient);
                    });
                }else{
                    Platform.runLater(() -> {
                        submitButton.setVisible(true);
                        waitButton.setVisible(false);

                        (new Alert(Alert.AlertType.NONE, "Could not add patient to PACS server", ButtonType.CLOSE)).showAndWait();
                    });
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable throwable) {
                Platform.runLater(() -> {
                    submitButton.setVisible(true);
                    waitButton.setVisible(false);

                    (new Alert(Alert.AlertType.NONE, "ERROR: " + throwable.getMessage(), ButtonType.CLOSE)).showAndWait();
                });
            }
        });
    }

    private String getModalities() {
        String modalities =
                (crCheckBox.isSelected() ? crCheckBox.getText() + " " : "") +
                (ctCheckBox.isSelected() ? ctCheckBox.getText() + " " : "") +
                (mrCheckBox.isSelected() ? mrCheckBox.getText() + " " : "") +
                (nmCheckBox.isSelected() ? nmCheckBox.getText() + " " : "") +
                (ptCheckBox.isSelected() ? ptCheckBox.getText() : "");
        return modalities;
    }

    private void loadDetailsWindow(ActionEvent event, Patient patient){
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("../views/details.fxml"));
            Parent root = fxmlLoader.load();
            DetailsController detailsController = (DetailsController) fxmlLoader.getController();
            detailsController.setPatient(patient);
            ((Button)(event.getSource())).getScene().setRoot(root);
        } catch (IOException ex) {
            Logger.getLogger(RegisterController.class.getName()).log(Level.INFO, null, ex);
        }
    }
}
