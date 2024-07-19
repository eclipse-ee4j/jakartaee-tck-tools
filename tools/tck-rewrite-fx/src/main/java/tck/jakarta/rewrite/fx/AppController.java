package tck.jakarta.rewrite.fx;

import io.quarkiverse.fx.RunOnFxThread;
import io.quarkiverse.fx.views.FxView;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import tck.jakarta.platform.ant.api.TestClientFile;
import tck.jakarta.platform.ant.api.TestPackageInfo;
import tck.jakarta.platform.ant.api.TestPackageInfoBuilder;
import tck.jakarta.platform.vehicles.VehicleType;
import tck.jakarta.rewrite.fx.codeview.JavaCodeView;
import tck.jakarta.rewrite.fx.codeview.JavaTestNameVisitor;
import tck.jakarta.rewrite.fx.dirview.FileItem;
import tck.jakarta.rewrite.fx.dirview.FileTreeItem;
import tck.jakarta.platform.ant.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@FxView("app")
@Singleton
public class AppController {
    @FXML
    BorderPane root;
    @FXML
    TreeView<FileItem> fileTreeView;
    @FXML
    TabPane codeTabPane;
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
    @FXML
    TextField searchField;

    JavaCodeView originalCodeView;
    JavaCodeView transformedCodeView;
    Path tsHome;
    Path testsRoot;
    FileTreeItem rootItem;
    ClassLoader tckClassLoader;
    @Inject
    Event<Path> testClassSelected;

    public BorderPane getRoot() {
        return root;
    }

    @FXML
    public void initialize() throws FileNotFoundException {
        // Look for TS_HOME env
        String tsHome = System.getenv("TS_HOME");
        File rootFile;
        if (tsHome != null) {
            this.tsHome = Paths.get(tsHome);
            this.testsRoot = this.tsHome.resolve("src/com/sun/ts/tests");
            System.setProperty("TS_HOME", tsHome.toString());
            rootFile = this.testsRoot.toFile();
            FileItem item = new FileItem(rootFile);
            rootItem = new FileTreeItem(item, testsRoot.getNameCount()-1);
            tckClassLoader = Utils.getTSClassLoader(this.tsHome);
        } else {
            String pwd = System.getenv("PWD");
            rootFile = new File(pwd);
            FileItem item = new FileItem(rootFile);
            rootItem = new FileTreeItem(item, rootFile.toPath().getNameCount()-1);
        }

        fileTreeView.setRoot(rootItem);
        Log.infof("rootFile: %s, fileName=%s", rootFile, rootItem.getFileName());
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
                this.testsRoot = tsHome.resolve("src/com/sun/ts/tests");
                System.setProperty("ts.home", tsHome.toString());
                System.setProperty("TS_HOME", tsHome.toString());
                rootItem = new FileTreeItem(this.testsRoot.toFile());
                fileTreeView.setRoot(rootItem);
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

    @FXML
    private void onSearchFieldKey(KeyEvent event) {
        boolean ok = event.getCode().isLetterKey() || event.getCode().isDigitKey() || event.getCode().getChar().equals(".");
        if (!ok) {
            return;
        }
        String searchText = searchField.getText().replace(".", "/");
        Path searchPath = Paths.get(searchText);
        FileTreeItem searchItem = rootItem.resolve(searchPath);
        if (searchItem != null) {
            //Log.info(searchItem);
            fileTreeView.getSelectionModel().select(searchItem);
            int row = fileTreeView.getRow(searchItem);
            fileTreeView.scrollTo(row);
        }
    }

    private void fileSelected(ObservableValue<? extends TreeItem<FileItem>> changed, TreeItem<FileItem> old,
                              TreeItem<FileItem> newVal) {
        if (newVal != null) {
            File path = newVal.getValue().getFile();
            if(path.isFile() && path.getName().endsWith(".java")) {
                Path testClassPath = path.toPath();
                testClassSelected.fireAsync(testClassPath);
            }
        }
    }

    private void parseTestClass(@ObservesAsync Path testClassPath) {
        Path srcDir = tsHome.resolve("src");
        Path pkgPath = srcDir.relativize(testClassPath.getParent());
        String pkgName = pkgPath.toString().replace(File.separator, ".");
        String simpleClassName = testClassPath.getFileName().toString().replace(".java", "");
        String className = pkgName + "." + simpleClassName;
        try {
            @Language("java")
            String source = Files.readString(testClassPath, StandardCharsets.UTF_8);
            List<String> methodNames = getMethodNames(source);

            Class<?> clazz = tckClassLoader.loadClass(className);
            TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
            TestPackageInfo pkgInfo = builder.buildTestPackgeInfo(clazz, methodNames);
            List<TestClientFile> testFiles = pkgInfo.getTestClientFiles();
            updateTestClassSelectionView(testClassPath, source, testFiles);
        } catch (Exception e) {
            Log.errorf(e, "Error parsing test class: %s", testClassPath);
            showAlert(e, "Error parsing test class");
        }
    }
    private List<String> getMethodNames(String source) throws IOException {
        J.CompilationUnit clientCu = JavaParser.fromJavaVersion()
                .build()
                .parse(source)
                .findFirst()
                .map(J.CompilationUnit.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));

        JavaTestNameVisitor<ExecutionContext> visitor = new JavaTestNameVisitor<>();
        clientCu.acceptJava(visitor, new InMemoryExecutionContext());
        return visitor.getMethodNames();
    }
    @RunOnFxThread
    void updateTestClassSelectionView(Path testClassPath, String originalCode, List<TestClientFile> testFiles) {
        originalCodeView.setCode(originalCode);
        List<VehicleType> vehicles = Utils.getVehicleTypes(testClassPath);
        updateVehicles(vehicles);
        if(codeTabPane.getTabs().size() > 1) {
            codeTabPane.getTabs().remove(1, codeTabPane.getTabs().size());
        }
        for (TestClientFile testFile : testFiles) {
            JavaCodeView codeView = new JavaCodeView();
            codeView.setCode(testFile.getContent());
            Tab tab = new Tab(testFile.getName());
            tab.setText(testFile.getName());
            tab.setContent(codeView);
            codeTabPane.getTabs().add(tab);
        }
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
