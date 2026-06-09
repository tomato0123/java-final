package network;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class LocalServer {
    private static final int PORT = 8080;
    private HttpServer server;
    private WebHookHandler webhookHandler;
    private String localUrl;

    public LocalServer(WebHookHandler webhookHandler) {
        this.webhookHandler = webhookHandler;
    }

    public String start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", this::serveMainPage);
        server.createContext("/webhook", webhookHandler);
        server.setExecutor(null);
        server.start();

        localUrl = "http://" + getLocalIP() + ":" + PORT;
        System.out.println("Local Server started: " + localUrl);
        return localUrl;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public String getLocalUrl() {
        return localUrl;
    }

    private void serveMainPage(HttpExchange exchange) throws IOException {
        byte[] bytes = buildTimerPage().getBytes("UTF-8");
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String buildTimerPage() {
        return "<!DOCTYPE html><html><head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<title>專注計時器</title>" +
            "<style>" +
            "body{font-family:sans-serif;background:#1a1a2e;color:#eee;" +
            "display:flex;flex-direction:column;align-items:center;" +
            "justify-content:center;min-height:100vh;margin:0;text-align:center;" +
            "padding:20px;box-sizing:border-box}" +
            "h1{font-size:1.4em;margin-bottom:16px}" +
            "#timer{font-size:5em;font-weight:bold;color:#00d4ff;letter-spacing:4px}" +
            "#status{margin-top:18px;font-size:1.1em;padding:8px 20px;" +
            "border-radius:20px;background:#16213e}" +
            "#warn{margin-top:14px;font-size:1em;color:#ff6b6b;" +
            "background:#2a0a0a;padding:8px 18px;border-radius:12px;display:none}" +
            ".leave-section{margin-top:24px;width:100%;max-width:320px}" +
            ".leave-title{font-size:0.95em;color:#aaa;margin-bottom:10px}" +
            ".leave-grid{display:grid;grid-template-columns:1fr 1fr;gap:10px}" +
            ".leave-btn{background:#0f3460;color:#eee;border:none;border-radius:14px;" +
            "padding:16px 0;font-size:1.05em;cursor:pointer;touch-action:manipulation}" +
            ".leave-btn:active{background:#1a5276}" +
            "#leave-active{margin-top:14px;padding:12px 20px;" +
            "background:#0a2e0a;border-radius:14px;display:none}" +
            "#leave-countdown{font-size:2.6em;font-weight:bold;color:#4caf50;margin:6px 0}" +
            "#leave-msg{font-size:0.9em;color:#81c784}" +
            "#leave-over{margin-top:14px;padding:10px 18px;background:#2a0a0a;" +
            "border-radius:12px;color:#ff6b6b;font-size:1em;display:none}" +
            "</style></head><body>" +
            "<h1>專注計時器</h1>" +
            "<div id='timer'>00:00</div>" +
            "<div id='status'>保持此頁面在前台</div>" +
            "<div id='warn'>離開偵測！訊號已送出！</div>" +
            "<div class='leave-section'>" +
            "  <div class='leave-title'>手機請假（短暫離開用）</div>" +
            "  <div id='leave-btns' class='leave-grid'>" +
            "    <button class='leave-btn' onclick='applyLeave(1)'>1 分鐘</button>" +
            "    <button class='leave-btn' onclick='applyLeave(3)'>3 分鐘</button>" +
            "    <button class='leave-btn' onclick='applyLeave(5)'>5 分鐘</button>" +
            "    <button class='leave-btn' onclick='applyLeave(10)'>10 分鐘</button>" +
            "  </div>" +
            "  <div id='leave-active'>" +
            "    <div id='leave-msg'>請假中，放鬆一下！</div>" +
            "    <div id='leave-countdown'>00:00</div>" +
            "  </div>" +
            "  <div id='leave-over'>請假時間到！快回來！</div>" +
            "</div>" +
            "<script>" +
            "var start=Date.now();" +
            "setInterval(function(){" +
            "  var e=Math.floor((Date.now()-start)/1000);" +
            "  document.getElementById('timer').textContent=" +
            "    String(Math.floor(e/60)).padStart(2,'0')+':'+String(e%60).padStart(2,'0');" +
            "},500);" +
            "document.addEventListener('visibilitychange',function(){" +
            "  var s=document.hidden?'hidden':'visible';" +
            "  fetch('/webhook?status='+s).catch(function(){});" +
            "  var w=document.getElementById('warn');" +
            "  if(document.hidden){w.style.display='block';" +
            "    setTimeout(function(){w.style.display='none';},3000);}" +
            "});" +
            "var leaveTimer=null;" +
            "function applyLeave(min){" +
            "  fetch('/webhook?leave='+min).catch(function(){});" +
            "  var end=Date.now()+min*60000;" +
            "  document.getElementById('leave-btns').style.display='none';" +
            "  document.getElementById('leave-active').style.display='block';" +
            "  document.getElementById('leave-over').style.display='none';" +
            "  if(leaveTimer)clearInterval(leaveTimer);" +
            "  leaveTimer=setInterval(function(){" +
            "    var rem=Math.max(0,Math.floor((end-Date.now())/1000));" +
            "    document.getElementById('leave-countdown').textContent=" +
            "      String(Math.floor(rem/60)).padStart(2,'0')+':'+String(rem%60).padStart(2,'0');" +
            "    if(rem<=0){" +
            "      clearInterval(leaveTimer);" +
            "      document.getElementById('leave-active').style.display='none';" +
            "      document.getElementById('leave-btns').style.display='grid';" +
            "      document.getElementById('leave-over').style.display='block';" +
            "    }" +
            "  },500);" +
            "}" +
            "</script></body></html>";
    }

    public static String getLocalIP() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    String ip = addr.getHostAddress();
                    if (ip.contains(".") && !addr.isLoopbackAddress()) return ip;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }
}
