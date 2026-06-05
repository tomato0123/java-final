package ui;

import config.CoinManager;
import config.ConfigManager;
import network.WebHookHandler;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;

public class DashboardFrame extends JFrame {
    private final RootFrame root;

    // ── 專注 tab 動態元件 ──────────────────────────
    private JTabbedPane tabs;
    private JPanel      focusCardParent;
    private CardLayout  focusCards;
    private JLabel      qrLabel;
    private JTextField  urlField;
    private JLabel      leaveStatusLabel;
    private JLabel      countdownLabel;
    private JLabel      activeCoinLabel;
    private JLabel      activeComboLabel;
    private JLabel      activeNextRewardLabel;
    private Timer       countdownTimer;
    private StatsTab    statsTab;
    private JButton     phoneToggleBtn;

    public DashboardFrame(RootFrame root) {
        this.root = root;
        setTitle("系統控制面板");
        setSize(490, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        tabs = new JTabbedPane();
        tabs.addTab("寵物與外觀", wrapInScroll(createPetPanel()));
        tabs.addTab("專注監控",   createFocusPanel());              // 已有內建 scroll
        statsTab = new StatsTab(root);
        tabs.addTab("數據分析",   wrapInScroll(statsTab));
        tabs.addTab("定時提醒",   wrapInScroll(new ReminderTab(root.getReminderManager())));
        tabs.addTab("待辦事項",   wrapInScroll(new TodoTab(root.getTodoManager())));
        tabs.addTab("黑名單設定", wrapInScroll(new BlacklistTab(root.getBlacklistManager())));
        // 用 index 比對（getSelectedComponent() 回傳的是 JScrollPane wrapper，不是 statsTab）
        tabs.addChangeListener(e -> { if (tabs.getSelectedIndex() == 2) statsTab.refresh(); });
        add(tabs);

        // 若開啟時專注已在進行中，直接顯示 QR Code
        if (root.isFocusActive()) {
            String url = root.getCurrentFocusUrl();
            if (url != null) onFocusStarted(url);
        }
    }

    // ════════════════════════════════════════════
    //  專注 tab 公開 API（由 RootFrame 呼叫）
    // ════════════════════════════════════════════
    public void onFocusStarted(String url) {
        SwingUtilities.invokeLater(() -> {
            qrLabel.setIcon(null);
            qrLabel.setText("載入 QR Code 中...");

            new SwingWorker<ImageIcon, Void>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    String encoded = URLEncoder.encode(url, "UTF-8");
                    URI apiUri = URI.create(
                        "https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=" + encoded);
                    return new ImageIcon(ImageIO.read(apiUri.toURL()));
                }
                @Override
                protected void done() {
                    try {
                        qrLabel.setIcon(get());
                        qrLabel.setText(null);
                    } catch (Exception ex) {
                        qrLabel.setText(
                            "<html><center>無法載入 QR Code<br>（需要網路）<br>請手動輸入網址</center></html>");
                    }
                }
            }.execute();

            urlField.setText(url);
            leaveStatusLabel.setText("狀態：專注中");
            focusCards.show(focusCardParent, "active");
            tabs.setSelectedIndex(1);
            setVisible(true);
            toFront();

            if (countdownTimer != null) countdownTimer.stop();
            countdownTimer = new Timer(500, e -> updateCountdown());
            countdownTimer.start();
            if (phoneToggleBtn != null) phoneToggleBtn.setText("📵 停用手機監控");
            statsTab.refresh();
        });
    }

    public void onFocusStopped() {
        SwingUtilities.invokeLater(() -> {
            if (countdownTimer != null) countdownTimer.stop();
            focusCards.show(focusCardParent, "idle");
            statsTab.refresh();
        });
    }

    public void onPhoneMonitorChanged(boolean active) {
        SwingUtilities.invokeLater(() -> {
            if (phoneToggleBtn != null)
                phoneToggleBtn.setText(active ? "📵 停用手機監控" : "📱 重新連線手機");
            if (!active) {
                qrLabel.setIcon(null);
                qrLabel.setText("<html><center>手機監控已停用<br>點「重新連線手機」即可恢復</center></html>");
                urlField.setText("（已停用）");
            }
        });
    }

    // ════════════════════════════════════════════
    //  寵物與外觀
    // ════════════════════════════════════════════
    private JPanel createPetPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.fill   = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0;
        panel.add(new JLabel("寵物種類：", SwingConstants.RIGHT), g);
        JComboBox<String> petBox = new JComboBox<>(new String[]{"blue", "baseball"});
        petBox.setSelectedItem(ConfigManager.getPetType());
        g.gridx = 1;
        panel.add(petBox, g);

        g.gridx = 0; g.gridy = 1;
        panel.add(new JLabel("隨機漫步：", SwingConstants.RIGHT), g);
        JCheckBox wanderBox = new JCheckBox("允許寵物在桌面上走動");
        wanderBox.setSelected(ConfigManager.isWanderAllowed());
        g.gridx = 1;
        panel.add(wanderBox, g);

        JButton saveBtn = new JButton("儲存並套用");
        saveBtn.addActionListener(e -> {
            ConfigManager.setPetType((String) petBox.getSelectedItem());
            ConfigManager.setWanderAllowed(wanderBox.isSelected());
            root.getPetPanel().reloadSettings();
            JOptionPane.showMessageDialog(this, "設定已更新！");
        });
        g.gridx = 1; g.gridy = 2;
        panel.add(saveBtn, g);

        return panel;
    }

    // ════════════════════════════════════════════
    //  專注監控 tab（CardLayout 切換 idle ↔ active）
    // ════════════════════════════════════════════
    private JPanel createFocusPanel() {
        focusCards      = new CardLayout();
        focusCardParent = new JPanel(focusCards);
        focusCardParent.add(wrapInScroll(buildFocusIdleCard()),   "idle");
        focusCardParent.add(buildFocusActiveCard(), "active");
        return focusCardParent;
    }

    /** 尚未開始專注時顯示 */
    private JPanel buildFocusIdleCard() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridwidth = 2;

        JLabel desc = new JLabel(
            "<html><center>啟動後會開啟本地伺服器並顯示 QR Code，<br>用手機掃描即可開始監控。</center></html>",
            SwingConstants.CENTER);
        desc.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
        g.gridx = 0; g.gridy = 0;
        panel.add(desc, g);

        // 番茄鐘目標
        g.gridwidth = 1; g.gridx = 0; g.gridy = 1;
        g.insets = new Insets(4, 10, 4, 4);
        panel.add(new JLabel("番茄鐘目標：", SwingConstants.RIGHT), g);
        JSpinner pomSpinner = new JSpinner(new SpinnerNumberModel(
            ConfigManager.getPomodoroDuration(), 0, 120, 5));
        pomSpinner.setPreferredSize(new Dimension(65, 26));
        JPanel pomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pomRow.add(pomSpinner);
        pomRow.add(new JLabel("分鐘（0 = 不顯示進度條）"));
        g.gridx = 1;
        panel.add(pomRow, g);
        pomSpinner.addChangeListener(e -> {
            ConfigManager.setPomodoroDuration((Integer) pomSpinner.getValue());
            root.getPetPanel().reloadSettings();
        });

        g.gridwidth = 2; g.gridx = 0; g.gridy = 2;
        g.insets = new Insets(10, 10, 10, 10);
        JButton focusBtn = new JButton("▶ 開始專注");
        focusBtn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 15));
        focusBtn.addActionListener(e -> root.startFocusSession());
        panel.add(focusBtn, g);

        return panel;
    }

    /** 專注進行中顯示（QR Code + 專注金幣 + 請假系統），可垂直滾動 */
    private JPanel buildFocusActiveCard() {
        // 所有元件以 BoxLayout Y 垂直堆疊，確保橫向全寬對齊
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // ── WiFi 提示 ──
        JLabel hint = new JLabel(
            "<html><center>⚠ 手機與電腦必須連接<b>同一個 WiFi</b>，掃描後才能連線！</center></html>",
            SwingConstants.CENTER);
        hint.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        hint.setForeground(new Color(140, 80, 0));
        hint.setOpaque(true);
        hint.setBackground(new Color(255, 243, 205));
        hint.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 180, 80)),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(hint);
        content.add(Box.createVerticalStrut(6));

        // ── QR Code + URL ──
        JPanel qrSection = new JPanel(new BorderLayout(0, 6));
        qrSection.setBorder(BorderFactory.createEmptyBorder(6, 0, 4, 0));
        qrSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        qrLabel = new JLabel("", SwingConstants.CENTER);
        qrLabel.setPreferredSize(new Dimension(160, 160));
        qrSection.add(qrLabel, BorderLayout.CENTER);
        urlField = new JTextField();
        urlField.setEditable(false);
        urlField.setHorizontalAlignment(JTextField.CENTER);
        urlField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        qrSection.add(urlField, BorderLayout.SOUTH);
        content.add(qrSection);
        content.add(Box.createVerticalStrut(4));

        // ── 請假系統 + 結束 ──
        JPanel leavePanel = new JPanel(new BorderLayout(0, 4));
        leavePanel.setBorder(BorderFactory.createTitledBorder("手機請假系統"));
        leavePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 說明文字
        JLabel leaveDesc = new JLabel(
            "<html><b>【手機請假】</b>需要短暫拿起手機（上廁所、倒水等）時使用。<br>"
            + "申請後手機畫面關閉或切換 App 不會觸發違規警告。<br>"
            + "<b>手機端計時頁面</b>同樣有「申請請假」按鈕，效果完全相同。</html>");
        leaveDesc.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 11));
        leaveDesc.setForeground(new Color(90, 90, 90));
        leaveDesc.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));

        JPanel statusRow = new JPanel(new GridLayout(2, 1, 0, 2));
        leaveStatusLabel = new JLabel("狀態：專注中", SwingConstants.CENTER);
        leaveStatusLabel.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        countdownLabel = new JLabel(" ", SwingConstants.CENTER);
        countdownLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 13));
        statusRow.add(leaveStatusLabel);
        statusRow.add(countdownLabel);

        JPanel northSection = new JPanel();
        northSection.setLayout(new BoxLayout(northSection, BoxLayout.Y_AXIS));
        northSection.add(leaveDesc);
        northSection.add(statusRow);
        leavePanel.add(northSection, BorderLayout.NORTH);
        JPanel leaveBtns = new JPanel(new GridLayout(1, 4, 4, 0));
        leaveBtns.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        for (int min : new int[]{1, 3, 5, 10}) {
            JButton b = new JButton(min + " 分鐘");
            b.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
            final int m = min;
            b.addActionListener(e -> {
                root.applyForLeave(m);
                leaveStatusLabel.setText("請假中（" + m + " 分鐘）");
            });
            leaveBtns.add(b);
        }
        leavePanel.add(leaveBtns, BorderLayout.CENTER);
        JButton endBtn = new JButton("⏹ 結束專注");
        endBtn.setForeground(new Color(180, 0, 0));
        endBtn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 13));
        endBtn.addActionListener(e -> root.stopFocusSession());
        phoneToggleBtn = new JButton("📵 停用手機監控");
        phoneToggleBtn.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        phoneToggleBtn.addActionListener(e -> root.togglePhoneMonitor());
        JPanel controlBtns = new JPanel(new GridLayout(1, 2, 6, 0));
        controlBtns.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        controlBtns.add(phoneToggleBtn);
        controlBtns.add(endBtn);
        leavePanel.add(controlBtns, BorderLayout.SOUTH);
        content.add(leavePanel);
        content.add(Box.createVerticalStrut(4));

        // ── 專注金幣 ──
        JPanel coinPanel = buildActiveCoinPanel();
        coinPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(coinPanel);

        // ── 整個 active card 包進 JScrollPane ──
        JScrollPane scrollPane = new JScrollPane(content,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    private void updateCountdown() {
        WebHookHandler wh = root.getWebhookHandler();
        if (wh == null) return;
        if (wh.isOnLeave()) {
            long remaining = wh.getLeaveEndTime() - System.currentTimeMillis();
            if (remaining > 0) {
                long secs = remaining / 1000;
                countdownLabel.setText(String.format("倒數：%02d:%02d", secs / 60, secs % 60));
                countdownLabel.setForeground(new Color(0, 140, 0));
            } else {
                countdownLabel.setText("⚠ 請假時間已到！");
                countdownLabel.setForeground(Color.RED);
            }
        } else {
            countdownLabel.setText(" ");
            leaveStatusLabel.setText("狀態：專注中");
        }
        // 同步更新金幣三欄
        if (activeCoinLabel != null) {
            activeCoinLabel.setText(root.getCoinManager().getCoins() + " 枚");
            activeComboLabel.setText(root.getCoinManager().getCombo() + "x");
            activeNextRewardLabel.setText("+" + root.getCoinManager().getNextReward() + " 枚");
        }
    }

    // ── 專注中金幣儀表板（active card 內）──────────
    private JPanel buildActiveCoinPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder("專注金幣"));

        JPanel cards = new JPanel(new GridLayout(1, 3, 6, 0));
        cards.setOpaque(false);

        activeCoinLabel = new JLabel(root.getCoinManager().getCoins() + " 枚", SwingConstants.CENTER);
        activeCoinLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 16));
        activeCoinLabel.setForeground(new Color(180, 120, 0));

        activeComboLabel = new JLabel(root.getCoinManager().getCombo() + "x", SwingConstants.CENTER);
        activeComboLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 16));
        activeComboLabel.setForeground(new Color(210, 70, 0));

        activeNextRewardLabel = new JLabel("+" + root.getCoinManager().getNextReward() + " 枚", SwingConstants.CENTER);
        activeNextRewardLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 16));
        activeNextRewardLabel.setForeground(new Color(0, 140, 80));

        cards.add(makeCoinCard("💰 金幣餘額",    activeCoinLabel));
        cards.add(makeCoinCard("⚡ 專注回饋倍率", activeComboLabel));
        cards.add(makeCoinCard("🎁 下次獎勵",    activeNextRewardLabel));
        panel.add(cards, BorderLayout.CENTER);

        JButton buyBtn = new JButton(
            "購買 10 分鐘放鬆通行證（需要 " + CoinManager.RELAX_PASS_COST + " 枚金幣）");
        buyBtn.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 11));
        buyBtn.addActionListener(e -> {
            if (!root.purchaseRelaxPass()) {
                JOptionPane.showMessageDialog(this,
                    "金幣不足！需要 " + CoinManager.RELAX_PASS_COST
                    + " 枚，目前只有 " + root.getCoinManager().getCoins() + " 枚。");
                return;
            }
            activeCoinLabel.setText(root.getCoinManager().getCoins() + " 枚");
            JOptionPane.showMessageDialog(this, "已購買！享受你的 10 分鐘放鬆時光～");
        });
        panel.add(buyBtn, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel makeCoinCard(String title, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 215, 240)),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        card.setBackground(new Color(245, 248, 255));
        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 10));
        titleLbl.setForeground(Color.GRAY);
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JScrollPane wrapInScroll(JComponent content) {
        JScrollPane sp = new JScrollPane(content,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getHorizontalScrollBar().setUnitIncrement(16);
        return sp;
    }
}
