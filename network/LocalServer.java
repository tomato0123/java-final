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
        server.createContext("/leave",  this::handleLeave);
        server.createContext("/status", this::handleStatus);
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

    private void handleLeave(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        String query = exchange.getRequestURI().getQuery(); // e.g. "minutes=5"
        int minutes = 5;
        if (query != null) {
            for (String part : query.split("&")) {
                if (part.startsWith("minutes=")) {
                    try { minutes = Integer.parseInt(part.substring(8)); } catch (NumberFormatException ignored) {}
                } else if (part.equals("cancel")) {
                    webhookHandler.cancelLeave();
                    sendJson(exchange, "{\"ok\":true,\"cancelled\":true}");
                    return;
                }
            }
        }
        webhookHandler.applyForLeave(minutes);
        sendJson(exchange, "{\"ok\":true,\"minutes\":" + minutes + "}");
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        sendJson(exchange, webhookHandler.getStatusJson());
    }

    private void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
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
            "justify-content:center;min-height:100vh;margin:0;text-align:center;padding:16px;box-sizing:border-box}" +
            "h1{font-size:1.4em;margin-bottom:16px}" +
            "#timer{font-size:5em;font-weight:bold;color:#00d4ff;letter-spacing:4px}" +
            "#status{margin-top:18px;font-size:1.1em;padding:8px 20px;" +
            "border-radius:20px;background:#16213e}" +
            "#warn{margin-top:14px;font-size:1em;color:#ff6b6b;" +
            "background:#2a0a0a;padding:8px 18px;border-radius:12px;display:none}" +
            ".leave-section{margin-top:24px;background:#0f3460;border-radius:16px;" +
            "padding:16px 20px;width:100%;max-width:320px;box-sizing:border-box}" +
            ".leave-section h2{font-size:1em;margin:0 0 12px;color:#aad4ff}" +
            ".leave-btns{display:grid;grid-template-columns:1fr 1fr;gap:8px}" +
            ".leave-btn{padding:12px 0;border:none;border-radius:10px;font-size:1em;" +
            "background:#1a6eb5;color:#fff;cursor:pointer;font-family:sans-serif}" +
            ".leave-btn:active{background:#0d4d8a}" +
            "#cancel-btn{margin-top:8px;width:100%;padding:10px 0;border:none;" +
            "border-radius:10px;font-size:0.95em;background:#7a1a1a;color:#ffaaaa;" +
            "cursor:pointer;display:none;font-family:sans-serif}" +
            "#leave-status{margin-top:10px;font-size:0.95em;color:#7affb0;min-height:1.2em}" +
            "#leave-countdown{font-size:1.4em;font-weight:bold;color:#00e676;min-height:1.6em}" +
            "</style></head><body>" +
            "<h1>🎯 專注計時器</h1>" +
            "<div id='timer'>00:00</div>" +
            "<div id='status'>✅ 保持此頁面在前台</div>" +
            "<div id='warn'>⚠️ 離開偵測！訊號已送出！</div>" +
            "<div class='leave-section'>" +
            "  <h2>📋 申請請假</h2>" +
            "  <div class='leave-btns'>" +
            "    <button class='leave-btn' onclick='applyLeave(1)'>1 分鐘</button>" +
            "    <button class='leave-btn' onclick='applyLeave(3)'>3 分鐘</button>" +
            "    <button class='leave-btn' onclick='applyLeave(5)'>5 分鐘</button>" +
            "    <button class='leave-btn' onclick='applyLeave(10)'>10 分鐘</button>" +
            "  </div>" +
            "  <button id='cancel-btn' onclick='cancelLeave()'>✕ 取消請假</button>" +
            "  <div id='leave-status'></div>" +
            "  <div id='leave-countdown'></div>" +
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
            "function applyLeave(m){" +
            "  fetch('/leave?minutes='+m).then(function(){updateLeaveStatus();}).catch(function(){});" +
            "}" +
            "function cancelLeave(){" +
            "  fetch('/leave?cancel').then(function(){updateLeaveStatus();}).catch(function(){});" +
            "}" +
            "function updateLeaveStatus(){" +
            "  fetch('/status').then(function(r){return r.json();}).then(function(d){" +
            "    var sl=document.getElementById('leave-status');" +
            "    var cl=document.getElementById('leave-countdown');" +
            "    var cb=document.getElementById('cancel-btn');" +
            "    if(d.onLeave){" +
            "      var secs=Math.ceil(d.remainingMs/1000);" +
            "      sl.textContent='假期進行中...';" +
            "      cl.textContent=String(Math.floor(secs/60)).padStart(2,'0')+':'+String(secs%60).padStart(2,'0');" +
            "      cb.style.display='block';" +
            "    } else {" +
            "      sl.textContent='';" +
            "      cl.textContent='';" +
            "      cb.style.display='none';" +
            "    }" +
            "  }).catch(function(){});" +
            "}" +
            "setInterval(updateLeaveStatus,1000);" +
            "updateLeaveStatus();" +
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
