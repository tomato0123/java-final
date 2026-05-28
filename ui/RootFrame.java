package ui;

import network.LocalServer;
import network.WebHookHandler;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class RootFrame extends JFrame {
    private PetPanel petPanel;
    private WebHookHandler webhookHandler;
    private LocalServer localServer;
    private FocusDialog focusDialog;
    private boolean isFocusActive = false;

    public RootFrame() {
        setAlwaysOnTop(true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(300, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        petPanel = new PetPanel(this);
        add(petPanel);
    }

    public void setNetworkComponents(WebHookHandler webhookHandler, LocalServer localServer) {
        this.webhookHandler = webhookHandler;
        this.localServer = localServer;
    }

    public PetPanel getPetPanel() {
        return petPanel;
    }

    public WebHookHandler getWebhookHandler() {
        return webhookHandler;
    }

    public void startFocusSession() {
        if (localServer == null) {
            JOptionPane.showMessageDialog(this, "網路元件尚未初始化，請重新啟動程式。");
            return;
        }
        if (!isFocusActive) {
            try {
                isFocusActive = true;
                String url = localServer.start();
                petPanel.setState("normal", "專注模式啟動！加油！");
                focusDialog = new FocusDialog(this, url);
                focusDialog.setVisible(true);
            } catch (IOException e) {
                isFocusActive = false;
                JOptionPane.showMessageDialog(this, "無法啟動伺服器：" + e.getMessage());
            }
        } else {
            // 已在專注中，把對話框帶到前台
            if (focusDialog != null && focusDialog.isDisplayable()) {
                focusDialog.setVisible(true);
                focusDialog.toFront();
            }
        }
    }

    public void stopFocusSession() {
        isFocusActive = false;
        if (webhookHandler != null) webhookHandler.cancelLeave();
        if (localServer != null) localServer.stop();
        petPanel.setState("normal", "專注結束！辛苦了！");
    }

    public void applyForLeave(int minutes) {
        if (webhookHandler != null) webhookHandler.applyForLeave(minutes);
    }

    public boolean isFocusActive() {
        return isFocusActive;
    }

    public void updatePetState(String state, String message) {
        petPanel.setState(state, message);
    }
}
