package network;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import ui.PetPanel;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;

public class WebHookHandler implements HttpHandler {
    private PetPanel pet;

    private volatile boolean isOnLeave = false;
    private volatile long leaveEndTime = 0;
    private Timer leaveExpiryTimer;

    public WebHookHandler(PetPanel pet) {
        this.pet = pet;
    }

    public void applyForLeave(int minutes) {
        SwingUtilities.invokeLater(() -> {
            if (leaveExpiryTimer != null) leaveExpiryTimer.stop();

            isOnLeave = true;
            leaveEndTime = System.currentTimeMillis() + (minutes * 60 * 1000L);
            pet.setState("sleep", "准假 " + minutes + " 分鐘，快去快回！");

            // 倒數結束後偵測是否逾假
            leaveExpiryTimer = new Timer(minutes * 60 * 1000 + 5000, e -> {
                if (isOnLeave && System.currentTimeMillis() > leaveEndTime) {
                    isOnLeave = false;
                    pet.triggerViolation("逾假未歸！嚴重扣血！！！");
                }
                ((Timer) e.getSource()).stop();
            });
            leaveExpiryTimer.setRepeats(false);
            leaveExpiryTimer.start();

            System.out.println("進入等待模式，倒數 " + minutes + " 分鐘");
        });
    }

    public void cancelLeave() {
        isOnLeave = false;
        SwingUtilities.invokeLater(() -> {
            if (leaveExpiryTimer != null) leaveExpiryTimer.stop();
        });
    }

    public boolean isOnLeave() {
        return isOnLeave;
    }

    public long getLeaveEndTime() {
        return leaveEndTime;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();

        if (query != null && query.contains("status=hidden")) {
            SwingUtilities.invokeLater(this::checkViolation);
        } else if (query != null && query.contains("status=visible")) {
            SwingUtilities.invokeLater(() -> {
                pet.setState("happy", "乖寶寶，繼續保持！");
                if (isOnLeave) {
                    isOnLeave = false;
                    if (leaveExpiryTimer != null) leaveExpiryTimer.stop();
                }
            });
        }

        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        byte[] resp = "OK".getBytes();
        exchange.sendResponseHeaders(200, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }

    private void checkViolation() {
        long now = System.currentTimeMillis();

        if (isOnLeave && now <= leaveEndTime) {
            System.out.println("合法離開中...");
        } else if (isOnLeave && now > leaveEndTime) {
            isOnLeave = false;
            if (leaveExpiryTimer != null) leaveExpiryTimer.stop();
            pet.triggerViolation("逾假未歸！嚴重扣血！！！");
        } else {
            pet.triggerViolation("抓到了！偷滑手機！扣血！");
        }
    }
}
