package ui;

import javax.swing.*;
import java.awt.*;

/**
 * 右下角浮動提醒，10 秒後自動消退，支援貪睡/跳過。
 */
public class ToastNotification extends JWindow {
    private static final int W    = 320;
    private static final int H    = 115;
    private static final int SECS = 10;

    private final Timer dismissTimer;
    private int remaining = SECS;

    public static void show(String title, String message,
                            Runnable onSnooze, Runnable onSkip) {
        SwingUtilities.invokeLater(
            () -> new ToastNotification(title, message, onSnooze, onSkip).setVisible(true));
    }

    private ToastNotification(String title, String message,
                               Runnable onSnooze, Runnable onSkip) {
        setAlwaysOnTop(true);
        setFocusableWindowState(false);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(screen.width - W - 16, screen.height - H - 52, W, H);

        // ── 外框面板 ──
        JPanel root = new JPanel(new BorderLayout(0, 6));
        root.setBackground(new Color(35, 39, 48));
        root.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 120, 210), 1),
            BorderFactory.createEmptyBorder(10, 14, 8, 14)));
        setContentPane(root);

        // 標題
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Microsoft JhengHei", Font.BOLD, 13));
        titleLbl.setForeground(new Color(140, 190, 255));
        root.add(titleLbl, BorderLayout.NORTH);

        // 訊息
        JLabel msgLbl = new JLabel("<html><body style='width:240px'>" + message + "</body></html>");
        msgLbl.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        msgLbl.setForeground(Color.WHITE);
        root.add(msgLbl, BorderLayout.CENTER);

        // 底部：按鈕 + 倒數進度條
        JPanel south = new JPanel(new BorderLayout(4, 3));
        south.setOpaque(false);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btns.setOpaque(false);

        JButton snoozeBtn = makeBtn("貪睡 5 分");
        snoozeBtn.addActionListener(e -> { dismiss(); onSnooze.run(); });

        JButton skipBtn = makeBtn("跳過");
        skipBtn.addActionListener(e -> { dismiss(); onSkip.run(); });

        btns.add(snoozeBtn);
        btns.add(skipBtn);

        JProgressBar bar = new JProgressBar(0, SECS);
        bar.setValue(SECS);
        bar.setPreferredSize(new Dimension(W, 4));
        bar.setForeground(new Color(90, 160, 255));
        bar.setBackground(new Color(55, 60, 72));
        bar.setBorderPainted(false);

        south.add(btns, BorderLayout.NORTH);
        south.add(bar, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        // 自動消退計時器
        dismissTimer = new Timer(1000, e -> {
            remaining--;
            bar.setValue(remaining);
            if (remaining <= 0) { dismiss(); onSkip.run(); }
        });
        dismissTimer.start();
    }

    private JButton makeBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 11));
        b.setFocusPainted(false);
        return b;
    }

    private void dismiss() {
        dismissTimer.stop();
        setVisible(false);
        dispose();
    }
}
