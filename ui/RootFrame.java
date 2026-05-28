package ui;

import config.BlacklistManager;
import config.ReminderManager;
import network.LocalServer;
import network.WebHookHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class RootFrame extends JFrame {

    // ── 核心元件 ──────────────────────────────────
    private PetPanel         petPanel;
    private ReminderManager  reminderManager;
    private BlacklistManager blacklistManager;
    private WebHookHandler   webhookHandler;
    private LocalServer      localServer;
    private FocusDialog      focusDialog;
    private boolean          isFocusActive = false;

    // ── 透明淡出 ──────────────────────────────────
    private volatile float petOpacity      = 1.0f;
    private static final float MIN_OPACITY = 0.15f;
    private static final int   IDLE_MS     = 30_000;  // 30 秒無互動後開始淡出
    private Timer inactivityTimer;
    private Timer fadeTimer;

    // ── 系統工作列 ────────────────────────────────
    private TrayIcon trayIcon;

    public RootFrame() {
        setAlwaysOnTop(true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(300, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // 關閉改為縮到工作列

        petPanel = new PetPanel(this);
        add(petPanel);

        reminderManager  = new ReminderManager(petPanel);
        blacklistManager = new BlacklistManager();

        setupTrayIcon();

        // 預設隱藏到工作列
        setVisible(false);
    }

    // ════════════════════════════════════════════
    //  系統工作列
    // ════════════════════════════════════════════
    private void setupTrayIcon() {
        if (!SystemTray.isSupported()) {
            setVisible(true); // 不支援時直接顯示
            return;
        }

        Image icon = createTrayImage();
        trayIcon = new TrayIcon(icon, "桌面學習寵物");
        trayIcon.setImageAutoSize(true);

        // 雙擊顯示主視窗
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showFromTray();
            }
        });

        // 右鍵選單
        PopupMenu popup = new PopupMenu();
        MenuItem showItem = new MenuItem("顯示寵物");
        showItem.addActionListener(e -> showFromTray());
        MenuItem focusItem = new MenuItem("開始專注");
        focusItem.addActionListener(e -> startFocusSession());
        MenuItem exitItem = new MenuItem("關閉程式");
        exitItem.addActionListener(e -> System.exit(0));
        popup.add(showItem);
        popup.add(focusItem);
        popup.addSeparator();
        popup.add(exitItem);
        trayIcon.setPopupMenu(popup);

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            setVisible(true); // 加入失敗時直接顯示
        }
    }

    /** 建立 32×32 工作列小圖示（程式生成，不需外部圖檔） */
    private Image createTrayImage() {
        int s = 32;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(60, 130, 240));
        g.fillOval(1, 1, s - 2, s - 2);
        g.setColor(Color.WHITE);
        g.fillOval(9,  9, 5, 5);   // 左眼
        g.fillOval(18, 9, 5, 5);   // 右眼
        g.setStroke(new BasicStroke(2f));
        g.drawArc(8, 14, 16, 10, 0, -180); // 笑臉
        g.dispose();
        return img;
    }

    public void hideToTray() {
        setVisible(false);
        if (trayIcon != null) trayIcon.displayMessage("桌面學習寵物", "已縮小到工作列", TrayIcon.MessageType.NONE);
    }

    public void showFromTray() {
        SwingUtilities.invokeLater(() -> {
            petOpacity = 1.0f;
            petPanel.repaint();
            setVisible(true);
            toFront();
        });
    }

    // ════════════════════════════════════════════
    //  淡出機制（只在專注模式中啟動）
    // ════════════════════════════════════════════
    public float getPetOpacity() { return petOpacity; }

    /** 滑鼠觸碰寵物時呼叫 → 立即恢復不透明並重置閒置計時器 */
    public void onPetActivity() {
        petOpacity = 1.0f;
        if (fadeTimer != null) fadeTimer.stop();
        petPanel.repaint();
        if (isFocusActive) resetInactivityTimer();
    }

    /** 違規或警告時呼叫 → 恢復不透明 + 顯示紅色疊層 */
    public void triggerAlert() {
        petOpacity = 1.0f;
        if (fadeTimer != null) fadeTimer.stop();
        petPanel.showAlert();
        petPanel.repaint();
        if (isFocusActive) resetInactivityTimer();
    }

    private void resetInactivityTimer() {
        if (inactivityTimer != null) inactivityTimer.stop();
        inactivityTimer = new Timer(IDLE_MS, e -> startFading());
        inactivityTimer.setRepeats(false);
        inactivityTimer.start();
    }

    private void startFading() {
        if (fadeTimer != null && fadeTimer.isRunning()) return;
        fadeTimer = new Timer(60, e -> {
            petOpacity = Math.max(MIN_OPACITY, petOpacity - 0.025f);
            petPanel.repaint();
            if (petOpacity <= MIN_OPACITY) ((Timer) e.getSource()).stop();
        });
        fadeTimer.start();
    }

    // ════════════════════════════════════════════
    //  專注模式
    // ════════════════════════════════════════════
    public void startFocusSession() {
        if (localServer == null) {
            JOptionPane.showMessageDialog(this, "網路元件尚未初始化，請重新啟動程式。");
            return;
        }
        showFromTray(); // 確保視窗可見
        if (!isFocusActive) {
            try {
                isFocusActive = true;
                petOpacity    = 1.0f;
                String url    = localServer.start();
                petPanel.setState("normal", "專注模式啟動！加油！");
                resetInactivityTimer();
                focusDialog = new FocusDialog(this, url);
                focusDialog.setVisible(true);
            } catch (IOException e) {
                isFocusActive = false;
                JOptionPane.showMessageDialog(this, "無法啟動伺服器：" + e.getMessage());
            }
        } else {
            if (focusDialog != null && focusDialog.isDisplayable()) {
                focusDialog.setVisible(true);
                focusDialog.toFront();
            }
        }
    }

    public void stopFocusSession() {
        isFocusActive = false;
        if (inactivityTimer != null) inactivityTimer.stop();
        if (fadeTimer       != null) fadeTimer.stop();
        petOpacity = 1.0f;
        petPanel.repaint();
        if (webhookHandler != null) webhookHandler.cancelLeave();
        if (localServer    != null) localServer.stop();
        petPanel.setState("normal", "專注結束！辛苦了！");

        // 3 秒後縮回工作列
        new Timer(3000, e -> { hideToTray(); ((Timer) e.getSource()).stop(); }).start();
    }

    public void applyForLeave(int minutes) {
        if (webhookHandler != null) webhookHandler.applyForLeave(minutes);
    }

    // ════════════════════════════════════════════
    //  Getters / setters
    // ════════════════════════════════════════════
    public void setNetworkComponents(WebHookHandler wh, LocalServer ls) {
        this.webhookHandler = wh;
        this.localServer    = ls;
    }

    public PetPanel          getPetPanel()          { return petPanel; }
    public WebHookHandler    getWebhookHandler()    { return webhookHandler; }
    public ReminderManager   getReminderManager()   { return reminderManager; }
    public BlacklistManager  getBlacklistManager()  { return blacklistManager; }
    public boolean           isFocusActive()        { return isFocusActive; }

    public void updatePetState(String state, String message) {
        petPanel.setState(state, message);
    }

    /** Stage 2 警告：嗶聲後將寵物移到螢幕中央 */
    public void moveToCenter() {
        setLocationRelativeTo(null);
        setVisible(true);
        toFront();
        petPanel.setState("angry", "回來讀書！！");
    }

    /** Stage 3 懲罰：彈出警告並結束本次專注 */
    public void triggerFocusFailed(String keyword) {
        petOpacity = 1.0f;
        petPanel.showAlert();
        petPanel.setState("angry", "違規！專注失敗！");
        JOptionPane.showMessageDialog(this,
            "偵測到「" + keyword + "」超過 10 秒！\n本次專注已強制結束。",
            "⚠ 專注失敗", JOptionPane.WARNING_MESSAGE);
        if (isFocusActive) stopFocusSession();
    }

    /** 申請查資料豁免（學習模式） */
    public void applyResearchMode(int minutes) {
        blacklistManager.startResearchMode(minutes);
        petPanel.setState("happy", "好，去查資料吧！" + minutes + " 分鐘後記得回來！");
    }
}
