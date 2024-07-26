package tck.jakarta.rewrite.fx;

import io.quarkiverse.fx.FxStartupEvent;
import io.quarkiverse.fx.RunOnFxThread;
import io.quarkiverse.fx.views.FxView;
import io.quarkus.logging.Log;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import tck.jakarta.platform.ant.api.TestClientFile;
import tck.jakarta.platform.ant.api.TestMethodInfo;
import tck.jakarta.platform.ant.api.TestPackageInfo;
import tck.jakarta.platform.ant.api.TestPackageInfoBuilder;
import tck.jakarta.platform.vehicles.VehicleType;
import tck.jakarta.rewrite.fx.codeview.JavaCodeView;
import tck.jakarta.rewrite.fx.codeview.JavaTestNameVisitor;
import tck.jakarta.rewrite.fx.codeview.MethodUtils;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

@FxView("app")
@Singleton
public class AppController {
    @FXML
    BorderPane root;
    @FXML
    TreeView<FileItem> fileTreeView;

    @FXML
    SplitPane mainSplitPane;
    @FXML
    TextField searchField;
    @FXML
    Label statusLabel;

    @Inject
    SourceViewController sourceViewController;

    // The EE10 TCK dist root
    Path tsHome;
    // tsHome.resolve(src/com/sun/ts/tests)
    Path testsRoot;
    // The root of the Arquillian/Junit5 tests repo
    Path testsRepoHome;
    FileTreeItem rootItem;
    ClassLoader tckClassLoader;
    @Inject
    Event<Path> testClassSelected;
    TestPackageInfo lastTestPackageInfo;

    public BorderPane getRoot() {
        return root;
    }

    @FXML
    public void initialize() throws FileNotFoundException {
        // Test JUL logging levels
        Logger tmp = Logger.getLogger("tck.jakarta.rewrite.fx.AppController");
        tmp.severe("JUL logging test at SEVERE level");
        tmp.config("JUL logging test at CONFIG level");
        tmp.warning("JUL logging test at WARN level");
        tmp.info("JUL logging test at INFO level");
        tmp.fine("JUL logging test at FINE level");
        tmp.finer("JUL logging test at FINER level");
        tmp.finest("JUL logging test at FINEST level");

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
            Log.infof("TS_HOME: %s", tsHome);
        } else {
            String pwd = System.getenv("PWD");
            rootFile = new File(pwd);
            FileItem item = new FileItem(rootFile);
            rootItem = new FileTreeItem(item, rootFile.toPath().getNameCount()-1);
        }
        String testsRepo = System.getenv("TESTS_REPO");
        if (testsRepo != null) {
            this.testsRepoHome = Paths.get(testsRepo);
            Log.infof("TESTS_REPO: %s", testsRepoHome);
        }

        fileTreeView.setRoot(rootItem);
        Log.infof("rootFile: %s, fileName=%s", rootFile, rootItem.getFileName());
        // Get the tree view selection model.
        MultipleSelectionModel<TreeItem<FileItem>> tvSelModel = fileTreeView.getSelectionModel();

        // Use a change listener to respond to a selection
        tvSelModel.selectedItemProperty().addListener(this::fileSelected);

        Log.infof("SourceViewController.rootPane: %s", sourceViewController.getRootPane());
    }

    public void onFxStartup(@Observes final FxStartupEvent event) {
        Log.infof("onFxStartup, SourceViewController.rootPane: %s", sourceViewController.getRootPane());
        mainSplitPane.getItems().set(1, sourceViewController.getRootPane());
    }

    @FXML
    public void onFileSetTsHome() {
        statusLabel.setText("Selecting TS_HOME...");
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("Select TCK Dist Root");
        File newRoot = fileChooser.showDialog(null);
        if (newRoot != null) {
            try {
                tsHome = newRoot.toPath();
                Utils.validateTSHome(tsHome);
                statusLabel.setText("TS_HOME set to: " + tsHome);
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
    public void onFileSetTestsRoot() {
        statusLabel.setText("Selecting Arquillian/Junit5 tests repo root...");
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("Select Tests Root");
        File newRoot = fileChooser.showDialog(null);
        if (newRoot != null) {
            this.testsRepoHome = newRoot.toPath();
            statusLabel.setText("Tests repo root: "+testsRepoHome);
        }
    }
    @FXML
    public void onFileSave() {
        Log.infof("onFileSave, testsRepoHome: %s, have testPkgInfo: %s", testsRepoHome, lastTestPackageInfo != null);
        if(lastTestPackageInfo != null) {
            setStatus("Saving Arquillian/Junit5 tests...");
            try {
                List<TestClientFile> clientFiles = lastTestPackageInfo.getTestClientFiles();
                for (TestClientFile clientFile : clientFiles) {
                    Path testDir = testsRepoHome.resolve(clientFile.getPackagePathFromRoot());
                    String javaFileName = clientFile.getName() + ".java";
                    Path clientPath = testDir.resolve(javaFileName);
                    Files.writeString(clientPath, clientFile.getContent());
                    Log.infof("Wrote: %s", clientPath);
                }
            } catch (IOException e) {
                showAlert(e, "Error saving Arquillian/Junit5 tests");
            }
            setStatus("Done");
        } else {
            statusLabel.setText("No TestPackageInfo result seen");
        }
    }

    @FXML
    public void onFileQuit() {
        Platform.exit();
        System.exit(0);
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

    @FXML
    private void onEditCopy() {
        sourceViewController.copySelectedTab();
    }

    private void fileSelected(ObservableValue<? extends TreeItem<FileItem>> changed, TreeItem<FileItem> old,
                              TreeItem<FileItem> newVal) {
        if (newVal != null) {
            File path = newVal.getValue().getFile();
            if(path.isFile() && path.getName().endsWith(".java")) {
                Path testClassPath = path.toPath();
                testClassSelected.fireAsync(testClassPath);
                fileTreeView.getScene().setCursor(Cursor.WAIT);
            }
        }
    }

    void parseTestClass(@ObservesAsync Path testClassPath) {
        Path srcDir = tsHome.resolve("src");
        Path pkgPath = srcDir.relativize(testClassPath.getParent());
        String pkgName = pkgPath.toString().replace(File.separator, ".");
        String simpleClassName = testClassPath.getFileName().toString().replace(".java", "");
        String className = pkgName + "." + simpleClassName;
        try {
            Class<?> clazz = Class.forName(className, true, tckClassLoader);
            @Language("java")
            String source = Files.readString(testClassPath, StandardCharsets.UTF_8);
            List<TestMethodInfo> methodNames = getMethodNames(clazz, source);

            TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
            setStatus("Parsing build.xml for: "+className);
            TestPackageInfo pkgInfo = builder.buildTestPackgeInfoEx(clazz, methodNames);
            List<TestClientFile> testFiles = pkgInfo.getTestClientFiles();
            updateTestClassSelectionView(testClassPath, source, testFiles);
            lastTestPackageInfo = pkgInfo;
            setStatus("Done");
        } catch (Throwable e) {
            clearCursor();
            Log.errorf(e, "Error parsing test class: %s", testClassPath);
            showAlert(e, "Error parsing test class");
        }
    }
    private List<TestMethodInfo> getMethodNames(Class<?> testClass, String source) throws IOException {
        J.CompilationUnit clientCu = JavaParser.fromJavaVersion()
                .build()
                .parse(source)
                .findFirst()
                .map(J.CompilationUnit.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("Could not parse as Java"));

        JavaTestNameVisitor<ExecutionContext> visitor = new JavaTestNameVisitor<>();
        clientCu.acceptJava(visitor, new InMemoryExecutionContext());
        ArrayList<TestMethodInfo> allMethodNames = new ArrayList<>(visitor.getMethodNames());
        allMethodNames.addAll(visitor.getExtMethodNames());
        HashMap<String, TestMethodInfo> allMethods = new HashMap<>();
        for(TestMethodInfo methodInfo : allMethodNames) {
            allMethods.put(methodInfo.getMethodName(), methodInfo);
        }
        // Now resolve the throws exceptions by looking at superclass method info
        Class<?> testBaseClass = testClass.getSuperclass();
        Log.infof("Resolving throws for %s, baseClass: %s", testClass, testBaseClass);
        MethodUtils.resolveMethodThrows(testBaseClass, allMethods);
        Log.infof("--- Resolved methods for %s:", allMethods);

        return allMethodNames;
    }
    @RunOnFxThread
    void setStatus(String msg) {
        statusLabel.setText(msg);
    }
    @RunOnFxThread
    void updateTestClassSelectionView(Path testClassPath, String originalCode, List<TestClientFile> testFiles) {
        sourceViewController.updateTestClassSelectionView(testClassPath, originalCode, testFiles);
        fileTreeView.getScene().setCursor(Cursor.DEFAULT);
    }

    @RunOnFxThread
    void clearCursor() {
        fileTreeView.getScene().setCursor(Cursor.DEFAULT);
    }

    private void showAlert(Throwable e, String title) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(sw.toString());
        alert.showAndWait();
    }
}
