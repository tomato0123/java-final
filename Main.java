import ui.RootFrame;
import monitor.JnaMonitor;
import network.LocalServer;

public class Main {
    public static void main(String[] args) {
        System.out.println("專案啟動中...");

        // 1. 啟動主 UI 視窗 (Root)
        RootFrame root = new RootFrame();
        root.setVisible(true);

        // 2. 啟動系統監控執行緒（傳入 root 方便在偵測到分心時控制 UI）
        JnaMonitor monitor = new JnaMonitor(root);
        Thread monitorThread = new Thread(monitor);
        monitorThread.start();

        // 3. 啟動本地伺服器（傳入 root 方便在接收到手機訊號時控制 UI）
        LocalServer server = new LocalServer(root);
        server.startServer();
    }
}