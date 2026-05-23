package ui;

import javax.swing.*;
import java.awt.*;

public class RootFrame extends JFrame {
    private JLabel statusLabel;

    public RootFrame() {
        // 基本視窗設定
        setTitle("硬核專注力守護獸");
        setSize(300, 300);
        setLocation(100, 100); // 測試用，之後可改為螢幕右下角
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // 核心亮點設定：讓視窗永遠置頂
        setAlwaysOnTop(true);
        
        // 移除作業系統預設邊框 (如果要實作透明去背，這行是必需的)
        // setUndecorated(true); 

        // 簡單放個標籤顯示狀態
        statusLabel = new JLabel("寵物狀態：專注中 😐", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 18));
        add(statusLabel);
    }

    // 提供給監控模組與網路模組呼叫的方法
    public void updatePetState(String state, String reason) {
        // 確保在 Swing 的執行緒中更新 UI
        SwingUtilities.invokeLater(() -> {
            if ("distracted".equals(state)) {
                statusLabel.setText("<html><center>抓到分心了！<br>原因: " + reason + " 💢</center></html>");
                getContentPane().setBackground(Color.RED);
            } else {
                statusLabel.setText("寵物狀態：專注中 😐");
                getContentPane().setBackground(SystemColor.window);
            }
        });
    }
}