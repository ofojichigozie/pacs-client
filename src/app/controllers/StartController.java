package app.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import java.net.URL;
import java.util.ResourceBundle;

public class StartController implements Initializable {
    @FXML private Label infoLabel1;
    @FXML private Label infoLabel2;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    public void setInfo(String info1, String info2){
        this.infoLabel1.setText(info1);
        this.infoLabel2.setText(info2);
    }
}
