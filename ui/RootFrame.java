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
    private DashboardFrame   dashboardFrame;
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

        // 雙擊顯示，右鍵改用 Swing JPopupMenu（解決中文顯示亂碼問題）
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showFromTray();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    SwingUtilities.invokeLater(() -> showSwingTrayMenu());
            }
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    SwingUtilities.invokeLater(() -> showSwingTrayMenu());
            }
        });

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

    /**
     * 用全螢幕透明疊層 + Swing JPopupMenu 取代原生 AWT PopupMenu。
     * 疊層 opacity=0.01f：使用者看不見，但 Windows 仍將任何點擊路由給它，
     * 所以點擊選單外任何地方（含其他應用程式視窗）都會讓選單關閉。
     */
    private void showSwingTrayMenu() {
        Point     p      = MouseInfo.getPointerInfo().getLocation();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        // 全螢幕疊層：幾乎不可見，但攔截所有外部點擊
        JWindow overlay = new JWindow();
        overlay.setAlwaysOnTop(true);
        overlay.setBounds(0, 0, screen.width, screen.height);
        try { overlay.setOpacity(0.01f); } catch (Exception ignored) {}

        Font mf = new Font("Microsoft JhengHei", Font.PLAIN, 12);
        JPopupMenu menu = new JPopupMenu();
        menu.setLightWeightPopupEnabled(false); // 強制獨立視窗，確保浮在疊層上方

        JMenuItem showItem = new JMenuItem("顯示寵物");
        showItem.setFont(mf);
        showItem.addActionListener(e -> { showFromTray();      overlay.dispose(); });

        JMenuItem focusItem = new JMenuItem("開始專注");
        focusItem.setFont(mf);
        focusItem.addActionListener(e -> { startFocusSession(); overlay.dispose(); });

        JMenuItem exitItem = new JMenuItem("關閉程式");
        exitItem.setFont(mf);
        exitItem.addActionListener(e -> System.exit(0));

        menu.add(showItem);
        menu.add(focusItem);
        menu.addSeparator();
        menu.add(exitItem);

        // 點擊疊層（選單以外的任何地方）→ 關閉
        overlay.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                menu.setVisible(false);
                overlay.dispose();
            }
        });

        // 選單因其他原因關閉（點選項目 / Escape）→ 也關閉疊層
        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                overlay.dispose();
            }
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                overlay.dispose();
            }
        });

        overlay.setVisible(true);
        // p.x/p.y 是螢幕座標；疊層從 (0,0) 開始，所以直接用原值
        menu.show(overlay, p.x, p.y);
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
                getOrCreateDashboard().onFocusStarted(url);
            } catch (IOException e) {
                isFocusActive = false;
                JOptionPane.showMessageDialog(this, "無法啟動伺服器：" + e.getMessage());
            }
        } else {
            DashboardFrame df = getOrCreateDashboard();
            df.setVisible(true);
            df.toFront();
        }
    }

    public void stopFocusSession() {
        isFocusActive = false;
        if (inactivityTimer != null) inactivityTimer.stop();
        if (fadeTimer       != null) fadeTimer.stop();
        petOpacity = 1.0f;
        petPanel.repaint();
        if (webhookHandler  != null) webhookHandler.cancelLeave();
        if (localServer     != null) localServer.stop();
        if (dashboardFrame  != null && dashboardFrame.isDisplayable())
            dashboardFrame.onFocusStopped();
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
    public String            getCurrentFocusUrl()   { return localServer != null ? localServer.getLocalUrl() : null; }

    /** 取得或建立控制面板（singleton，關閉後重建） */
    public DashboardFrame getOrCreateDashboard() {
        if (dashboardFrame == null || !dashboardFrame.isDisplayable()) {
            dashboardFrame = new DashboardFrame(this);
        }
        return dashboardFrame;
    }

    /** 顯示控制面板並置前 */
    public void openDashboard() {
        DashboardFrame df = getOrCreateDashboard();
        df.setVisible(true);
        df.toFront();
    }

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
