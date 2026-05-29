package ui;

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
    private Timer       countdownTimer;

    public DashboardFrame(RootFrame root) {
        this.root = root;
        setTitle("系統控制面板");
        setSize(480, 530);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        tabs = new JTabbedPane();
        tabs.addTab("寵物與外觀", createPetPanel());
        tabs.addTab("專注監控",   createFocusPanel());
        tabs.addTab("定時提醒",   new ReminderTab(root.getReminderManager()));
        tabs.addTab("倒數計時",   new CountdownTab(root.getReminderManager()));
        tabs.addTab("黑名單設定", new BlacklistTab(root.getBlacklistManager()));
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
        });
    }

    public void onFocusStopped() {
        SwingUtilities.invokeLater(() -> {
            if (countdownTimer != null) countdownTimer.stop();
            focusCards.show(focusCardParent, "idle");
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
        focusCardParent.add(buildFocusIdleCard(),   "idle");
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

        JButton focusBtn = new JButton("▶ 開始專注");
        focusBtn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 15));
        focusBtn.addActionListener(e -> root.startFocusSession());
        g.gridy = 1;
        panel.add(focusBtn, g);

        return panel;
    }

    /** 專注進行中顯示（QR Code + 請假系統） */
    private JPanel buildFocusActiveCard() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

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
        panel.add(hint, BorderLayout.NORTH);

        // ── QR Code + URL ──
        JPanel qrSection = new JPanel(new BorderLayout(0, 6));
        qrSection.setBorder(BorderFactory.createEmptyBorder(6, 0, 4, 0));

        qrLabel = new JLabel("", SwingConstants.CENTER);
        qrLabel.setPreferredSize(new Dimension(160, 160));
        qrSection.add(qrLabel, BorderLayout.CENTER);

        urlField = new JTextField();
        urlField.setEditable(false);
        urlField.setHorizontalAlignment(JTextField.CENTER);
        urlField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        qrSection.add(urlField, BorderLayout.SOUTH);

        panel.add(qrSection, BorderLayout.CENTER);

        // ── 請假系統 + 結束 ──
        JPanel leavePanel = new JPanel(new BorderLayout(0, 4));
        leavePanel.setBorder(BorderFactory.createTitledBorder("請假系統"));

        JPanel statusRow = new JPanel(new GridLayout(2, 1, 0, 2));
        leaveStatusLabel = new JLabel("狀態：專注中", SwingConstants.CENTER);
        leaveStatusLabel.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        countdownLabel = new JLabel(" ", SwingConstants.CENTER);
        countdownLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 13));
        statusRow.add(leaveStatusLabel);
        statusRow.add(countdownLabel);
        leavePanel.add(statusRow, BorderLayout.NORTH);

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
        leavePanel.add(endBtn, BorderLayout.SOUTH);

        panel.add(leavePanel, BorderLayout.SOUTH);
        return panel;
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
    }
}
