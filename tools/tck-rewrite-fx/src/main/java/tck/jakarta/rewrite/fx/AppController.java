package tck.jakarta.rewrite.fx;

import io.quarkiverse.fx.views.FxView;
import io.quarkus.logging.Log;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import tck.jakarta.platform.vehicles.VehicleType;
import tck.jakarta.rewrite.fx.codeview.JavaCodeView;
import tck.jakarta.rewrite.fx.dirview.FileItem;
import tck.jakarta.rewrite.fx.dirview.FileTreeItem;
import tck.jakarta.platform.ant.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@FxView("app")
@Singleton
public class AppController {
    @FXML
    BorderPane root;
    @FXML
    TreeView<FileItem> fileTreeView;
    @FXML
    Tab originalTab;
    @FXML
    Tab transformedTab;
    @FXML
    CheckBox  appclient;
    @FXML
    CheckBox appmanaged;
    @FXML
    CheckBox appmanagedNoTx;
    @FXML
    CheckBox ejb;
    @FXML
    CheckBox ejblitejsf;
    @FXML
    CheckBox ejblitejsp;
    @FXML
    CheckBox ejblitesecuredjsp;
    @FXML
    CheckBox ejbliteservlet;
    @FXML
    CheckBox ejbliteservlet2;
    @FXML
    CheckBox jsp;
    @FXML
    CheckBox pmservlet;
    @FXML
    CheckBox puservlet;
    @FXML
    CheckBox servlet;
    @FXML
    CheckBox standalone;
    @FXML
    CheckBox stateful3;
    @FXML
    CheckBox stateless3;
    @FXML
    CheckBox web;
    @FXML
    CheckBox none;

    JavaCodeView originalCodeView;
    JavaCodeView transformedCodeView;
    Path tsHome;
    ClassLoader tckClassLoader;

    public BorderPane getRoot() {
        return root;
    }

    @FXML
    public void initialize() {
        // Look for TS_HOME env
        String tsHome = System.getenv("TS_HOME");
        File rootFile;
        if (tsHome != null) {
            System.setProperty("TS_HOME", tsHome.toString());
            rootFile = new File(tsHome);
        } else {
            String pwd = System.getenv("PWD");
            rootFile = new File(pwd);
        }

        fileTreeView.setRoot(new FileTreeItem(rootFile));
        // Get the tree view selection model.
        MultipleSelectionModel<TreeItem<FileItem>> tvSelModel = fileTreeView.getSelectionModel();

        // Use a change listener to respond to a selection
        tvSelModel.selectedItemProperty().addListener(this::fileSelected);

        originalCodeView = new JavaCodeView();
        originalTab.setContent(originalCodeView);
        transformedCodeView = new JavaCodeView();
        transformedTab.setContent(transformedCodeView);
    }

    @FXML
    public void onFileSetTsHome() {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("Select TCK Dist Root");
        File newRoot = fileChooser.showDialog(null);
        if (newRoot != null) {
            try {
                tsHome = newRoot.toPath();
                Utils.validateTSHome(tsHome);
                System.setProperty("ts.home", tsHome.toString());
                System.setProperty("TS_HOME", tsHome.toString());
                fileTreeView.setRoot(new FileTreeItem(newRoot));
                tckClassLoader = Utils.getTSClassLoader(tsHome);
            } catch (FileNotFoundException e) {
                showAlert(e, "Invalid TS_HOME");
            }
        }
    }
    @FXML
    public void onFileQuit() {
        Platform.exit();
    }

    private void fileSelected(ObservableValue<? extends TreeItem<FileItem>> changed, TreeItem<FileItem> old,
                              TreeItem<FileItem> newVal) {
        if (newVal != null) {
            File path = newVal.getValue().getFile();
            if(path.isFile() && path.getName().endsWith(".java")) {
                try {
                    Path testClassPath = path.toPath();
                    String code = Files.readString(testClassPath);
                    originalCodeView.setCode(code);
                    updateVehicles(testClassPath);
                } catch (IOException e) {
                    showAlert(e, "Error reading source");
                }
            }
        }
    }
    private void updateVehicles(Path testFile) {
        List<VehicleType> vehicles = Utils.getVehicleTypes(testFile);
        Log.infof("Vehicles(%s): %s", testFile, vehicles);
        clearVehicles();
        for (VehicleType type : vehicles) {
            switch (type) {
                case appclient: this.appclient.setSelected(true); break;
                case appmanaged: this.appmanaged.setSelected(true); break;
                case appmanagedNoTx: this.appmanagedNoTx.setSelected(true); break;
                case ejb: this.ejb.setSelected(true); break;
                case ejblitejsf: this.ejblitejsf.setSelected(true); break;
                case ejblitejsp: this.ejblitejsp.setSelected(true); break;
                case ejblitesecuredjsp: this.ejblitesecuredjsp.setSelected(true); break;
                case ejbliteservlet: this.ejbliteservlet.setSelected(true); break;
                case ejbliteservlet2: this.ejbliteservlet2.setSelected(true); break;
                case jsp: this.jsp.setSelected(true); break;
                case pmservlet: this.pmservlet.setSelected(true); break;
                case puservlet: this.puservlet.setSelected(true); break;
                case servlet: this.servlet.setSelected(true); break;
                case standalone: this.standalone.setSelected(true); break;
                case stateful3: this.stateful3.setSelected(true); break;
                case stateless3: this.stateless3.setSelected(true); break;
                case web: this.web.setSelected(true); break;
                case none: this.none.setSelected(true); break;
            }
        }
    }
    private void clearVehicles() {
        appclient.setSelected(false);
        appmanaged.setSelected(false);
        appmanagedNoTx.setSelected(false);
        ejb.setSelected(false);
        ejblitejsf.setSelected(false);
        ejblitejsp.setSelected(false);
        ejblitesecuredjsp.setSelected(false);
        ejbliteservlet.setSelected(false);
        ejbliteservlet2.setSelected(false);
        jsp.setSelected(false);
        pmservlet.setSelected(false);
        puservlet.setSelected(false);
        servlet.setSelected(false);
        standalone.setSelected(false);
        stateful3.setSelected(false);
        stateless3.setSelected(false);
        web.setSelected(false);
        none.setSelected(false);
    }

    private void showAlert(Exception e, String title) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(sw.toString());
        alert.showAndWait();
    }
}
