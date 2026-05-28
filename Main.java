import monitor.JnaMonitor;
import network.LocalServer;
import network.WebHookHandler;
import ui.RootFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("專案啟動中...");

        SwingUtilities.invokeLater(() -> {
            RootFrame root = new RootFrame();
            root.setVisible(true);

            // 系統監控執行緒
            JnaMonitor monitor = new JnaMonitor(root);
            monitor.start();

            // 網路元件（不在這裡啟動伺服器，按「開始專注」時才啟動）
            WebHookHandler webhookHandler = new WebHookHandler(root.getPetPanel());
            LocalServer localServer = new LocalServer(webhookHandler);
            root.setNetworkComponents(webhookHandler, localServer);
        });
    }
}
