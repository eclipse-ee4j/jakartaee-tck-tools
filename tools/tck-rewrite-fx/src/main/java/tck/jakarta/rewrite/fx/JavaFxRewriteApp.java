package tck.jakarta.rewrite.fx;

import io.quarkiverse.fx.FxPostStartupEvent;
import io.quarkus.logging.Log;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.awt.*;

/**
 * A Quarkus FX application for testing the Jakarta TCK Rewrite libraries in an interactive manner.
 *
 * <a href="https://docs.quarkiverse.io/quarkus-fx/dev/index.html">Quarkus FX</a>
 */
@ApplicationScoped
public class JavaFxRewriteApp {
    @Inject
    AppController appController;

    void observePrimaryStage(@Observes final FxPostStartupEvent event) {
        Stage stage = event.getPrimaryStage();
        stage.setOnCloseRequest(ce -> {
            Platform.exit();
            System.exit(0);
        });
        Image appIcon = new Image("/images/app.png");
        stage.getIcons().add(appIcon);
        //Set icon on the taskbar/dock
        if (Taskbar.isTaskbarSupported()) {
            var taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
                var dockIcon = defaultToolkit.getImage(getClass().getResource("/images/app.png"));
                taskbar.setIconImage(dockIcon);
            } else {
                Log.info("Taskbar does not support ICON_IMAGE");
            }
        }

        Scene scene = new Scene(appController.getRoot());
        stage.setScene(scene);
        stage.setTitle("Jakarta TCK Rewrite Application");
        scene.getStylesheets().add(JavaFxRewriteApp.class.getResource("/java-keywords.css").toExternalForm());
        stage.show();
    }
}
