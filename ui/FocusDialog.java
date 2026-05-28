package ui;

import network.WebHookHandler;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;

public class FocusDialog extends JDialog {
    private RootFrame root;
    private JLabel leaveStatusLabel;
    private JLabel countdownLabel;
    private Timer countdownTimer;

    public FocusDialog(RootFrame root, String localUrl) {
        super(root, "專注模式", false);
        this.root = root;

        setSize(360, 480);
        setLocationRelativeTo(root);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        add(buildQrPanel(localUrl), BorderLayout.CENTER);
        add(buildLeavePanel(), BorderLayout.SOUTH);

        countdownTimer = new Timer(500, e -> updateCountdown());
        countdownTimer.start();
    }

    private JPanel buildQrPanel(String localUrl) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 6, 14));

        JLabel title = new JLabel("用手機掃描 QR Code 開始監控", SwingConstants.CENTER);
        title.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        panel.add(title, BorderLayout.NORTH);

        JLabel qrLabel = new JLabel("載入 QR Code 中...", SwingConstants.CENTER);
        qrLabel.setPreferredSize(new Dimension(220, 220));
        panel.add(qrLabel, BorderLayout.CENTER);

        // 非同步從 qrserver.com 取得 QR Code 圖片
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                String encoded = URLEncoder.encode(localUrl, "UTF-8");
                URI apiUri = URI.create("https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + encoded);
                return new ImageIcon(ImageIO.read(apiUri.toURL()));
            }
            @Override
            protected void done() {
                try {
                    qrLabel.setIcon(get());
                    qrLabel.setText(null);
                } catch (Exception ex) {
                    qrLabel.setText("<html><center>無法載入 QR Code<br>（需要網路連線）<br>請手動輸入以下網址</center></html>");
                }
            }
        }.execute();

        JTextField urlField = new JTextField(localUrl);
        urlField.setEditable(false);
        urlField.setHorizontalAlignment(JTextField.CENTER);
        urlField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        panel.add(urlField, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildLeavePanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("請假系統"));

        // 狀態與倒數
        JPanel statusPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        leaveStatusLabel = new JLabel("狀態：專注中", SwingConstants.CENTER);
        leaveStatusLabel.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        countdownLabel = new JLabel(" ", SwingConstants.CENTER);
        countdownLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 14));
        statusPanel.add(leaveStatusLabel);
        statusPanel.add(countdownLabel);
        panel.add(statusPanel, BorderLayout.NORTH);

        // 請假按鈕
        JPanel btnPanel = new JPanel(new GridLayout(1, 4, 4, 0));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        for (int min : new int[]{1, 3, 5, 10}) {
            JButton btn = new JButton(min + " 分鐘");
            final int m = min;
            btn.addActionListener(e -> {
                root.applyForLeave(m);
                leaveStatusLabel.setText("請假中（" + m + " 分鐘）");
            });
            btnPanel.add(btn);
        }
        panel.add(btnPanel, BorderLayout.CENTER);

        // 結束專注
        JButton endBtn = new JButton("結束專注");
        endBtn.setForeground(new Color(180, 0, 0));
        endBtn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 13));
        endBtn.addActionListener(e -> {
            countdownTimer.stop();
            root.stopFocusSession();
            dispose();
        });
        panel.add(endBtn, BorderLayout.SOUTH);

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
