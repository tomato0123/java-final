package ui;

import config.ConfigManager;

import javax.swing.*;
import java.awt.*;

public class DashboardFrame extends JFrame {
    private PetPanel petPanel;

    public DashboardFrame(PetPanel petPanel) {
        this.petPanel = petPanel;
        
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

        JLabel typeLabel = new JLabel("寵物種類：", SwingConstants.RIGHT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(typeLabel, gbc);

        String[] petOptions = {"blue", "baseball"};
        JComboBox<String> petComboBox = new JComboBox<>(petOptions);
        petComboBox.setSelectedItem(ConfigManager.getPetType());
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(petComboBox, gbc);

        JLabel wanderLabel = new JLabel("隨機漫步：", SwingConstants.RIGHT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(wanderLabel, gbc);

        JCheckBox wanderCheckBox = new JCheckBox("允許寵物在桌面上走動");
        wanderCheckBox.setSelected(ConfigManager.isWanderAllowed());
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(wanderCheckBox, gbc);

        JButton saveButton = new JButton("儲存並套用");
        saveButton.addActionListener(e -> {
            String selectedPet = (String) petComboBox.getSelectedItem();
            boolean isAllowed = wanderCheckBox.isSelected();
            
            ConfigManager.setPetType(selectedPet);
            ConfigManager.setWanderAllowed(isAllowed);
            
            petPanel.reloadSettings();
            JOptionPane.showMessageDialog(this, "設定已更新！");
        });
        
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(saveButton, gbc);

        return panel;
    }

    private JPanel createFocusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("番茄鐘與 QR Code 功能開發中...", SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
}