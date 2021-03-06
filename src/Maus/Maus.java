package Maus;

import GUI.Styler;
import GUI.Views.MainView;
import Logger.Level;
import Logger.Logger;
import Server.Data.PseudoBase;
import Server.Server;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class Maus extends Application {
    private static Stage primaryStage;
    private static Server server = new Server();
    private static final int PORT = 9999;
    private static ServerSocket socket;
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException, ClassNotFoundException {
        Maus.primaryStage = primaryStage;
        /* Prevents more than one instance of Maus at a time. */
        try {
            socket = new ServerSocket(PORT,0, InetAddress.getByAddress(new byte[] {127,0,0,1}));
        }
        catch (BindException e) {
            System.exit(1);
        }

        /* Ensure that the necessary files exist */
        new PseudoBase().createMausData();
        /* Load data from files - including client data, server settings, etc. */
        new PseudoBase().loadData(System.getProperty("user.home") + "/Maus/clients/");
        /* Set up primary view */
        getPrimaryStage().setTitle("Maus 0.5a");
        getPrimaryStage().setMinWidth(600);
        getPrimaryStage().setMinHeight(500);
        getPrimaryStage().setMaxWidth(900);
        getPrimaryStage().setMaxHeight(800);
        Scene mainScene = new Scene(new MainView().getMainView(), 900, 500);
        mainScene.getStylesheets().add(Styler.globalCSS);
        getPrimaryStage().setScene(mainScene);
        getPrimaryStage().getIcons().add(new Image("Resources/Images/Icons/icon.png"));
        getPrimaryStage().setOnCloseRequest(event -> System.exit(0));
        /* Maus is running! */
        Logger.log(Level.INFO, "Maus is running.");
        getPrimaryStage().initStyle(StageStyle.UNDECORATED);
        getPrimaryStage().show();

        /* Start the server to listen for client connections. */
        Runnable startServer = server;
        new Thread(startServer).start();

        /*Before Maus is closed - write Maus data to file (server settings, clients, etc.) */
        Runtime.getRuntime().addShutdownHook(
                new Thread("mausData") {
                    @Override
                    public void run() {
                        try {
                            PseudoBase.writeMausData();
                            Logger.log(Level.INFO, "MausData saved to file. ");
                        } catch (IOException e) {
                            Logger.log(Level.ERROR, e.toString());
                        }
                    }
                });
    }
}
