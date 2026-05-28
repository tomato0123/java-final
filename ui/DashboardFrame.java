package ui;

import config.ConfigManager;

import javax.swing.*;
import java.awt.*;

public class DashboardFrame extends JFrame {
    private RootFrame root;

    public DashboardFrame(RootFrame root) {
        this.root = root;

        setTitle("系統控制面板");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("寵物與外觀", createPetPanel());
        tabbedPane.addTab("專注與連動", createFocusPanel());

        add(tabbedPane);
    }

    private JPanel createPetPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("寵物種類：", SwingConstants.RIGHT), gbc);

        String[] petOptions = {"blue", "baseball"};
        JComboBox<String> petComboBox = new JComboBox<>(petOptions);
        petComboBox.setSelectedItem(ConfigManager.getPetType());
        gbc.gridx = 1;
        panel.add(petComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("隨機漫步：", SwingConstants.RIGHT), gbc);

        JCheckBox wanderCheckBox = new JCheckBox("允許寵物在桌面上走動");
        wanderCheckBox.setSelected(ConfigManager.isWanderAllowed());
        gbc.gridx = 1;
        panel.add(wanderCheckBox, gbc);

        JButton saveButton = new JButton("儲存並套用");
        saveButton.addActionListener(e -> {
            ConfigManager.setPetType((String) petComboBox.getSelectedItem());
            ConfigManager.setWanderAllowed(wanderCheckBox.isSelected());
            root.getPetPanel().reloadSettings();
            JOptionPane.showMessageDialog(this, "設定已更新！");
        });
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(saveButton, gbc);

        return panel;
    }

    private JPanel createFocusPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        JLabel desc = new JLabel("<html><center>啟動後會開啟本地伺服器並顯示 QR Code，<br>用手機掃描即可開始監控。</center></html>",
                SwingConstants.CENTER);
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(desc, gbc);

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
        gbc.gridy = 1;
        panel.add(focusBtn, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("申請請假：", SwingConstants.RIGHT), gbc);

        JPanel leavePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        for (int min : new int[]{1, 3, 5, 10}) {
            JButton btn = new JButton(min + " 分");
            final int m = min;
            btn.addActionListener(e -> root.applyForLeave(m));
            leavePanel.add(btn);
        }
        gbc.gridx = 1;
        panel.add(leavePanel, gbc);

        return panel;
    }
}
