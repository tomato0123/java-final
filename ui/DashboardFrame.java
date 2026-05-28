package ui;

import config.ConfigManager;

import javax.swing.*;
import java.awt.*;

public class DashboardFrame extends JFrame {
    private final RootFrame root;

    public DashboardFrame(RootFrame root) {
        this.root = root;

        setTitle("系統控制面板");
        setSize(480, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("寵物與外觀", createPetPanel());
        tabs.addTab("專注與連動", createFocusPanel());
        tabs.addTab("定時提醒",   new ReminderTab(root.getReminderManager()));
        tabs.addTab("倒數計時",   new CountdownTab(root.getReminderManager()));

        add(tabs);
    }

    // ── 寵物與外觀 ────────────────────────────────
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

    // ── 專注與連動 ────────────────────────────────
    private JPanel createFocusPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill   = GridBagConstraints.HORIZONTAL;
        g.gridwidth = 2;

        JLabel desc = new JLabel(
            "<html><center>啟動後會開啟本地伺服器並顯示 QR Code，<br>用手機掃描即可開始監控。</center></html>",
            SwingConstants.CENTER);
        g.gridx = 0; g.gridy = 0;
        panel.add(desc, g);

        JButton focusBtn = new JButton(root.isFocusActive() ? "⏸ 結束專注" : "▶ 開始專注");
        focusBtn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 15));
        focusBtn.addActionListener(e -> {
            if (root.isFocusActive()) {
                root.stopFocusSession();
                focusBtn.setText("▶ 開始專注");
            } else {
                root.startFocusSession();
                focusBtn.setText("⏸ 結束專注");
            }
        });
        g.gridy = 1;
        panel.add(focusBtn, g);

        g.gridwidth = 1; g.gridx = 0; g.gridy = 2;
        panel.add(new JLabel("申請請假：", SwingConstants.RIGHT), g);
        JPanel leaveRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        for (int min : new int[]{1, 3, 5, 10}) {
            JButton b = new JButton(min + " 分");
            final int m = min;
            b.addActionListener(e -> root.applyForLeave(m));
            leaveRow.add(b);
        }
        g.gridx = 1;
        panel.add(leaveRow, g);

        return panel;
    }
}
