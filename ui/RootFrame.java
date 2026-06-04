package ui;

import config.BlacklistManager;
import config.CoinManager;
import config.ConfigManager;
import config.FocusStatsManager;
import config.ReminderManager;
import config.TodoManager;
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
    private TodoManager      todoManager;
    private CoinManager      coinManager;
    private WebHookHandler   webhookHandler;
    private LocalServer      localServer;
    private DashboardFrame   dashboardFrame;
    private boolean           isFocusActive     = false;
    private long              focusStartTime    = 0;
    private FocusStatsManager statsManager      = new FocusStatsManager();
    private boolean           phoneMonitorActive = false;
    private String            currentFocusTask  = null; // null = no task selected

    // ── 透明淡出 ──────────────────────────────────
    private volatile float petOpacity      = 1.0f;
    private static final float MIN_OPACITY = 0.15f;
    private static final int   IDLE_MS     = 30_000;  // 30 秒無互動後開始淡出
    private Timer inactivityTimer;
    private Timer fadeTimer;

    // ── 定向走路 ──────────────────────────────────
    private Point preMovePos = null;   // 走到中間之前的原始位置
    private Timer moveTimer  = null;

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
        todoManager      = new TodoManager();
        coinManager      = new CoinManager();

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
            // 顯示任務選擇對話框
            TaskSelectDialog taskDialog = new TaskSelectDialog(this, todoManager);
            taskDialog.setVisible(true);
            if (taskDialog.isCancelled()) return;
            currentFocusTask = taskDialog.getSelectedTitle();

            try {
                isFocusActive      = true;
                focusStartTime     = System.currentTimeMillis();
                statsManager.onFocusStart();
                petOpacity         = 1.0f;
                String url         = localServer.start();
                phoneMonitorActive = true;
                String startMsg = currentFocusTask != null
                    ? "收到！我們現在全力解決『" + currentFocusTask + "』！"
                    : "專注模式啟動！加油！";
                petPanel.setState("normal", startMsg);
                resetInactivityTimer();
                getOrCreateDashboard().onFocusStarted(url);
            } catch (IOException e) {
                isFocusActive    = false;
                focusStartTime   = 0;
                currentFocusTask = null;
                JOptionPane.showMessageDialog(this, "無法啟動伺服器：" + e.getMessage());
            }
        } else {
            DashboardFrame df = getOrCreateDashboard();
            df.setVisible(true);
            df.toFront();
        }
    }

    public void stopFocusSession() {
        long sessionStart  = focusStartTime; // capture before reset
        statsManager.onFocusEnd();
        focusStartTime     = 0;
        phoneMonitorActive = false;
        isFocusActive      = false;
        currentFocusTask   = null;

        // Award coins if at least one pomodoro duration was completed
        int coinsEarned = 0;
        if (sessionStart > 0) {
            int pomMin = ConfigManager.getPomodoroDuration();
            if (pomMin == 0) pomMin = 25;
            if (System.currentTimeMillis() - sessionStart >= (long) pomMin * 60_000) {
                coinsEarned = CoinManager.BASE_REWARD * coinManager.getCombo();
                coinManager.onPomodoroCompleted();
                int earned = coinsEarned;
                ToastNotification.show(
                    "獲得專注金幣！",
                    "本次獲得 " + earned + " 枚！目前共 " + coinManager.getCoins() + " 枚",
                    () -> {}, () -> {}
                );
            }
        }
        if (inactivityTimer != null) inactivityTimer.stop();
        if (fadeTimer       != null) fadeTimer.stop();
        if (moveTimer       != null) { moveTimer.stop(); moveTimer = null; }
        petPanel.setDirectedWalk(false);
        if (preMovePos != null) { setLocation(preMovePos); preMovePos = null; }
        petOpacity = 1.0f;
        petPanel.repaint();
        if (webhookHandler  != null) webhookHandler.cancelLeave();
        if (localServer     != null) localServer.stop();
        if (dashboardFrame  != null && dashboardFrame.isDisplayable())
            dashboardFrame.onFocusStopped();
        if (coinsEarned > 0) {
            petPanel.setState("happy", "太棒了！獲得 " + coinsEarned + " 枚金幣！辛苦了！");
        } else {
            petPanel.setState("normal", "專注結束！辛苦了！");
        }

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
        wh.setOnComboBreak(() -> coinManager.breakCombo());
    }

    /** Spends coins and grants a 10-minute relax leave. Returns false if coins are insufficient. */
    public boolean purchaseRelaxPass() {
        if (!coinManager.spendCoins(CoinManager.RELAX_PASS_COST)) return false;
        applyForLeave(10);
        return true;
    }

    public PetPanel          getPetPanel()          { return petPanel; }
    public WebHookHandler    getWebhookHandler()    { return webhookHandler; }
    public ReminderManager   getReminderManager()   { return reminderManager; }
    public BlacklistManager  getBlacklistManager()  { return blacklistManager; }
    public TodoManager       getTodoManager()       { return todoManager; }
    public CoinManager       getCoinManager()       { return coinManager; }
    public boolean           isFocusActive()        { return isFocusActive; }
    public String            getCurrentFocusUrl()   { return localServer != null ? localServer.getLocalUrl() : null; }
    public String            getCurrentFocusTask()  { return currentFocusTask; }

    public long getFocusElapsedMs() {
        if (!isFocusActive || focusStartTime == 0) return 0;
        return System.currentTimeMillis() - focusStartTime;
    }

    public FocusStatsManager getStatsManager()      { return statsManager; }
    public boolean           isPhoneMonitorActive() { return phoneMonitorActive; }

    public void togglePhoneMonitor() {
        if (!isFocusActive) return;
        if (phoneMonitorActive) {
            if (localServer != null) localServer.stop();
            phoneMonitorActive = false;
            if (dashboardFrame != null && dashboardFrame.isDisplayable())
                dashboardFrame.onPhoneMonitorChanged(false);
        } else {
            if (localServer != null) {
                try {
                    String url = localServer.start();
                    phoneMonitorActive = true;
                    if (dashboardFrame != null && dashboardFrame.isDisplayable())
                        dashboardFrame.onFocusStarted(url);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "無法重新連線：" + e.getMessage());
                }
            }
        }
    }

    public void recordDistraction() { statsManager.onDistraction(); }

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

    /** Stage 2 警告：寵物衝刺到螢幕中央，停留 3 秒後衝回原位 */
    public void moveToCenter() {
        setVisible(true);
        toFront();
        if (preMovePos == null) preMovePos = getLocation();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Point center = new Point(screen.width  / 2 - getWidth()  / 2,
                                 screen.height / 2 - getHeight() / 2);

        walkToPosition(center, "找你來了！！", () -> {
            String arriveMsg = currentFocusTask != null
                ? "你的『" + currentFocusTask + "』呢？！快回來！！"
                : "回來讀書！！快！！";
            petPanel.setState("angry", arriveMsg);
            Toolkit.getDefaultToolkit().beep();
            Timer stay = new Timer(3000, e -> {
                returnToPreMovePos();
                ((Timer) e.getSource()).stop();
            });
            stay.setRepeats(false);
            stay.start();
        });
    }

    private void returnToPreMovePos() {
        if (preMovePos == null) return;
        Point target = preMovePos;
        walkToPosition(target, "我回去了！", () -> {
            preMovePos = null;
            String msg = currentFocusTask != null
                ? "繼續完成『" + currentFocusTask + "』！加油！"
                : "繼續讀書！加油！";
            petPanel.setState("normal", msg);
        });
    }

    /**
     * 讓寵物以走路動畫斜線衝刺到目標位置。
     * 每 40 ms 移動一次，方向向量正規化後乘以速度；抵達後執行 onArrived。
     */
    private void walkToPosition(Point target, String msgWhileWalking, Runnable onArrived) {
        if (moveTimer != null) { moveTimer.stop(); moveTimer = null; }
        petPanel.setDirectedWalk(true);

        final int      SPEED   = 9;
        final String[] lastDir = {""};

        moveTimer = new Timer(40, null);
        moveTimer.addActionListener(e -> {
            Point  cur  = getLocation();
            double dx   = target.x - cur.x;
            double dy   = target.y - cur.y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist <= SPEED) {
                setLocation(target.x, target.y);
                ((Timer) e.getSource()).stop();
                moveTimer = null;
                petPanel.setDirectedWalk(false);
                SwingUtilities.invokeLater(() -> { if (onArrived != null) onArrived.run(); });
            } else {
                int    moveX = (int)(SPEED * dx / dist);
                int    moveY = (int)(SPEED * dy / dist);
                String dir   = (dx < 0) ? "walk_left" : "walk_right";
                if (!dir.equals(lastDir[0])) {
                    petPanel.setState(dir, msgWhileWalking);
                    lastDir[0] = dir;
                }
                setLocation(cur.x + moveX, cur.y + moveY);
            }
        });
        moveTimer.start();
    }

    /** Stage 3 懲罰：彈出警告並結束本次專注 */
    public void triggerFocusFailed(String keyword) {
        if (moveTimer != null) { moveTimer.stop(); moveTimer = null; }
        petPanel.setDirectedWalk(false);
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
