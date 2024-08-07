package tck.jakarta.rewrite.fx;

import io.quarkiverse.fx.views.FxView;
import io.quarkus.logging.Log;
import jakarta.inject.Singleton;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import tck.jakarta.platform.ant.Utils;
import tck.jakarta.platform.ant.api.DeploymentInfo;
import tck.jakarta.platform.ant.api.TestClientFile;
import tck.jakarta.platform.ant.api.TestClientInfo;
import tck.jakarta.platform.vehicles.VehicleType;
import tck.jakarta.rewrite.fx.codeview.JavaCodeView;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The controller for the tab pane that shows the EE10 test class and the transformed arquillian/Junit5 test classes.
 */
@FxView("orig-view")
@Singleton
public class SourceViewController {
    @FXML
    AnchorPane rootPane;
    @FXML
    TabPane codeTabPane;
    @FXML
    Tab originalTab;
    @FXML
    Tab transformedTab;
    @FXML
    CheckBox appclient;
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
    @FXML
    ListView<String> descriptorsListView;

    JavaCodeView originalCodeView;
    JavaCodeView transformedCodeView;
    List<TestClientInfo> testClientInfos;

    @FXML
    public void initialize() throws FileNotFoundException {
        originalCodeView = new JavaCodeView();
        originalTab.setContent(originalCodeView);
        transformedCodeView = new JavaCodeView();
        transformedTab.setContent(transformedCodeView);
        Log.infof("SourceViewController initialized");
    }

    /**
     * Called to update the view with the original test class and the transformed test classes when the treeview
     * selection changes.
     *
     * @param testClassPath EE10 test class path
     * @param originalCode - the content of the original test java file
     * @param testFiles - the content(s) of the transformed test java files
     */
    public void updateTestClassSelectionView(Path testClassPath, String originalCode, List<TestClientFile> testFiles, List<TestClientInfo> testClientInfos) {

        this.testClientInfos = testClientInfos;
        originalCodeView.setCode(originalCode);
        List<VehicleType> vehicles = Utils.getVehicleTypes(testClassPath);
        updateVehicles(vehicles);
        if(codeTabPane.getTabs().size() > 1) {
            codeTabPane.getTabs().remove(1, codeTabPane.getTabs().size());
        }
        codeTabPane.getSelectionModel().selectedIndexProperty().addListener(this::onTabSelected);
        for (TestClientFile testFile : testFiles) {
            JavaCodeView codeView = new JavaCodeView();
            codeView.setCode(testFile.getContent());
            Tab tab = new Tab(testFile.getName());
            tab.setText(testFile.getName());
            tab.setContent(codeView);
            codeTabPane.getTabs().add(tab);
        }

    }

    @FXML
    void onTabSelected(ObservableValue<? extends Number> ov, Number prevIndex, Number newIndex) {
        int selectedIndex = newIndex.intValue() - 1;
        if(selectedIndex < 0) {
            return;
        }

        // Update descriptors list view
        TestClientInfo testClientInfo = testClientInfos.get(selectedIndex);
        testClientInfo.getTestDeployment().getDebugInfo().getDeploymentDescriptors();
        ArrayList<String> descriptors = new ArrayList<>();
        DeploymentInfo deployInfo = testClientInfo.getTestDeployment().getDebugInfo();
        if(deployInfo.getHasClientJar()) {
            List<String> tmp = deployInfo.getClientJar().getFoundDescriptors();
            if(!tmp.isEmpty()) {
                descriptors.add("Client Descriptors:");
                descriptors.addAll(tmp);
            }
        }
        if(deployInfo.getHasEjbJar()) {
            List<String> tmp = deployInfo.getEjbJar().getFoundDescriptors();
            if(!tmp.isEmpty()) {
                descriptors.add("Ejb Descriptors:");
                descriptors.addAll(tmp);
            }
        }
        if(deployInfo.getHasWar()) {
            List<String> tmp = deployInfo.getWar().getFoundDescriptors();
            if(!tmp.isEmpty()) {
                descriptors.add("War Descriptors:");
                descriptors.addAll(tmp);
            }
        }
        if(deployInfo.getHasPar()) {
            List<String> tmp = deployInfo.getPar().getFoundDescriptors();
            if(!tmp.isEmpty()) {
                descriptors.add("Par Descriptors:");
                descriptors.addAll(tmp);
            }
        }
        if(deployInfo.getHasRar()) {
            List<String> tmp = deployInfo.getRar().getFoundDescriptors();
            if(!tmp.isEmpty()) {
                descriptors.add("Rar Descriptors:");
                descriptors.addAll(tmp);
            }
        }
        if(deployInfo.getHasEar()) {
            List<String> tmp = deployInfo.getEar().getFoundDescriptors();
            if(!tmp.isEmpty()) {
                descriptors.add("Ear Descriptors:");
                descriptors.addAll(tmp);
            }
        }
        descriptorsListView.getItems().clear();
        descriptorsListView.getItems().addAll(descriptors);
        Log.infof("Descriptors: %s", descriptors);
    }

    private void updateVehicles(List<VehicleType> vehicles) {
        Log.infof("Vehicles: %s", vehicles);
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

    public void copySelectedTab() {
        Tab selectedTab = codeTabPane.getSelectionModel().getSelectedItem();
        if(selectedTab != null) {
            JavaCodeView codeView = (JavaCodeView) selectedTab.getContent();
            ClipboardContent content = new ClipboardContent();
            content.putString(codeView.getText());
            Clipboard.getSystemClipboard().setContent(content);

            Log.infof("Copy from %s", selectedTab.getText());
        }
    }

    public Node getRootPane() {
        return rootPane;
    }
}
