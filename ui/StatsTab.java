package ui;

import config.CoinManager;
import config.FocusStatsManager;

import javax.swing.*;
import java.awt.*;

public class StatsTab extends JPanel {

    private final RootFrame         root;
    private final FocusStatsManager stats;
    private final CoinManager       coinManager;
    private JLabel     todayFocusLabel;
    private JLabel     todayDistLabel;
    private JLabel     coinsLabel;
    private ChartPanel chartPanel;

    public StatsTab(RootFrame root) {
        this.root        = root;
        this.stats       = root.getStatsManager();
        this.coinManager = root.getCoinManager();
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(buildTodaySection());
        northPanel.add(Box.createVerticalStrut(8));
        northPanel.add(buildCoinSection());
        add(northPanel, BorderLayout.NORTH);
        add(buildChartSection(), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton refreshBtn = new JButton("⟳ 重新整理");
        refreshBtn.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        refreshBtn.addActionListener(e -> refresh());
        bottom.add(refreshBtn);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        long focusMs = stats.getTodayFocusMs();
        int  dist    = stats.getTodayDistractionCount();
        long secs    = focusMs / 1000;
        todayFocusLabel.setText(String.format("%02d:%02d:%02d",
            secs / 3600, (secs % 3600) / 60, secs % 60));
        todayDistLabel.setText(dist + " 次");
        coinsLabel.setText(coinManager.getCoins() + " 枚");
        chartPanel.repaint();
    }

    // ════════════════════════════════════════════
    //  專注金幣
    // ════════════════════════════════════════════
    private JPanel buildCoinSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder("專注金幣"));
        panel.setOpaque(false);

        coinsLabel = new JLabel(coinManager.getCoins() + " 枚", SwingConstants.CENTER);
        coinsLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 22));
        coinsLabel.setForeground(new Color(180, 120, 0));
        panel.add(makeCard("💰 金幣餘額", coinsLabel), BorderLayout.CENTER);

        return panel;
    }

    // ════════════════════════════════════════════
    //  今日統計
    // ════════════════════════════════════════════
    private JPanel buildTodaySection() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setBorder(BorderFactory.createTitledBorder("今日統計"));
        panel.setOpaque(false);

        todayFocusLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        todayFocusLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 24));
        todayFocusLabel.setForeground(new Color(0, 160, 100));

        todayDistLabel = new JLabel("0 次", SwingConstants.CENTER);
        todayDistLabel.setFont(new Font("Microsoft JhengHei", Font.BOLD, 24));
        todayDistLabel.setForeground(new Color(200, 60, 60));

        panel.add(makeCard("⏱ 專注時間", todayFocusLabel));
        panel.add(makeCard("⚡ 分心次數", todayDistLabel));
        return panel;
    }

    private JPanel makeCard(String title, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 215, 240)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        card.setBackground(new Color(245, 248, 255));
        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 11));
        titleLbl.setForeground(Color.GRAY);
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    // ════════════════════════════════════════════
    //  近 7 日走勢圖
    // ════════════════════════════════════════════
    private JPanel buildChartSection() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createTitledBorder("近 7 日專注趨勢（分鐘）"));
        chartPanel = new ChartPanel();
        wrapper.add(chartPanel, BorderLayout.CENTER);
        return wrapper;
    }

    private class ChartPanel extends JPanel {
        private static final int PAD_L = 38;
        private static final int PAD_R = 8;
        private static final int PAD_T = 12;
        private static final int PAD_B = 22;

        ChartPanel() {
            setPreferredSize(new Dimension(0, 170));
            setBackground(new Color(245, 248, 255));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            long[]   focusMs = stats.getWeeklyFocusMs();
            String[] labels  = stats.getWeeklyLabels();

            int W  = getWidth()  - PAD_L - PAD_R;
            int H  = getHeight() - PAD_T - PAD_B;
            int x0 = PAD_L;
            int y0 = PAD_T;
            if (W <= 0 || H <= 0) return;

            long maxMs = 30L * 60_000;
            for (long v : focusMs) if (v > maxMs) maxMs = v;

            Font smallFont = new Font("Microsoft JhengHei", Font.PLAIN, 10);
            g2.setFont(smallFont);

            // Grid lines + Y labels
            int gridCount = 4;
            for (int i = 0; i <= gridCount; i++) {
                int    gy  = y0 + H - (int)((double) i / gridCount * H);
                long   val = maxMs * i / gridCount / 60_000;
                String lbl = val + "m";

                g2.setColor(new Color(218, 226, 240));
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{4f, 3f}, 0f));
                g2.drawLine(x0, gy, x0 + W, gy);

                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(140, 155, 180));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(lbl, x0 - fm.stringWidth(lbl) - 3, gy + fm.getAscent() / 2);
            }

            // Bars + day labels
            int barW = W / 7;
            int gap  = Math.max(2, barW / 7);
            g2.setStroke(new BasicStroke(1f));
            for (int i = 0; i < 7; i++) {
                int barH = (int)((double) focusMs[i] / maxMs * H);
                int bx   = x0 + i * barW + gap;
                int bw   = barW - gap * 2;
                int by   = y0 + H - barH;

                // Bar fill
                if (barH > 0) {
                    g2.setColor(i == 6 ? new Color(0, 160, 230) : new Color(50, 110, 200));
                    g2.fillRoundRect(bx, by, bw, barH, 4, 4);
                } else {
                    g2.setColor(new Color(200, 210, 230));
                    g2.fillRect(bx, y0 + H - 1, bw, 1);
                }

                // Day label (today = bold blue)
                Font dayFont = (i == 6)
                    ? new Font("Microsoft JhengHei", Font.BOLD, 11)
                    : smallFont;
                g2.setFont(dayFont);
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(i == 6 ? new Color(0, 120, 210) : new Color(90, 110, 145));
                int lx = bx + (bw - fm.stringWidth(labels[i])) / 2;
                g2.drawString(labels[i], lx, y0 + H + fm.getAscent() + 4);

                // Value label above bar
                if (focusMs[i] > 0 && by > y0 + 10) {
                    long   mins = focusMs[i] / 60_000;
                    String val  = mins < 60 ? mins + "m" : (mins / 60) + "h" + (mins % 60 > 0 ? (mins % 60) + "m" : "");
                    g2.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 9));
                    fm = g2.getFontMetrics();
                    g2.setColor(new Color(50, 90, 160));
                    g2.drawString(val, bx + (bw - fm.stringWidth(val)) / 2, by - 2);
                }
            }

            // X-axis line
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(160, 175, 210));
            g2.drawLine(x0, y0 + H, x0 + W, y0 + H);
        }
    }
}
