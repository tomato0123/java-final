package network;

import ui.RootFrame;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class LocalServer {
    private RootFrame root;
    private int port = 8080;

    public LocalServer(RootFrame root) {
        this.root = root;
    }

    public void startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // 建立一條路徑給手機網頁發送「切換分心」的請求
            server.createContext("/phone-signal", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    System.out.println("[網路通知] 收到手機端傳來的網頁狀態改變訊號！");
                    
                    // 呼叫主視窗改變狀態
                    root.updatePetState("distracted", "手機切換分心 (LINE/IG)");

                    String response = "Signal Received";
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            });

            server.setExecutor(null); // 使用預設的 executor
            server.start();
            System.out.println("本地伺服器已在 Port " + port + " 啟動，等待手機連線...");
            
        } catch (IOException e) {
            System.out.println("伺服器啟動失敗: " + e.getMessage());
        }
    }
}