package ui;

import config.BlacklistManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BlacklistTab extends JPanel {
    private final BlacklistManager manager;
    private JPanel  keywordListPanel;
    private JLabel  researchStatusLbl;
    private Timer   clockTimer;

    public BlacklistTab(BlacklistManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildKeywordSection(), BorderLayout.CENTER);
        add(buildResearchSection(), BorderLayout.SOUTH);
    }

    // ── 黑名單關鍵字管理 ─────────────────────────────
    private JPanel buildKeywordSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder("黑名單關鍵字"));

        // 輸入列
        JPanel inputRow = new JPanel(new BorderLayout(4, 0));
        JTextField kwField = new JTextField();
        kwField.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
        JButton addBtn = new JButton("新增");
        addBtn.addActionListener(e -> {
            String kw = kwField.getText().trim();
            if (!kw.isEmpty()) {
                manager.addKeyword(kw);
                kwField.setText("");
                refreshList();
            }
        });
        kwField.addActionListener(e -> addBtn.doClick());
        inputRow.add(kwField, BorderLayout.CENTER);
        inputRow.add(addBtn, BorderLayout.EAST);
        panel.add(inputRow, BorderLayout.NORTH);

        // 關鍵字列表
        keywordListPanel = new JPanel();
        keywordListPanel.setLayout(new BoxLayout(keywordListPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(keywordListPanel);
        scroll.setPreferredSize(new Dimension(0, 160));
        panel.add(scroll, BorderLayout.CENTER);

        refreshList();
        return panel;
    }

    private void refreshList() {
        keywordListPanel.removeAll();
        List<String> kws = manager.getKeywords();
        for (String kw : kws) {
            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            row.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            row.setBackground(new Color(240, 245, 255));

            JLabel lbl = new JLabel(kw);
            lbl.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
            row.add(lbl, BorderLayout.CENTER);

            JButton del = new JButton("✕");
            del.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            del.setMargin(new Insets(0, 4, 0, 4));
            del.addActionListener(e -> {
                manager.removeKeyword(kw);
                refreshList();
            });
            row.add(del, BorderLayout.EAST);

            keywordListPanel.add(row);
        }
        keywordListPanel.revalidate();
        keywordListPanel.repaint();
    }

    // ── 學習模式豁免 ──────────────────────────────────
    private JPanel buildResearchSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder("學習模式豁免（查資料用）"));

        JLabel desc = new JLabel(
            "<html><div style='width:300px'>開啟後黑名單暫停偵測，允許前往 YouTube 等查資料。</div></html>");
        desc.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        panel.add(desc, BorderLayout.NORTH);

        // 快速時間按鈕
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        btnRow.add(new JLabel("申請時間："));
        for (int min : new int[]{1, 3, 5, 10}) {
            JButton b = new JButton(min + " 分鐘");
            b.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
            final int m = min;
            b.addActionListener(e -> {
                manager.startResearchMode(m);
                updateResearchStatus();
            });
            btnRow.add(b);
        }
        JButton cancelBtn = new JButton("取消豁免");
        cancelBtn.setForeground(new Color(160, 0, 0));
        cancelBtn.addActionListener(e -> {
            manager.cancelResearchMode();
            updateResearchStatus();
        });
        btnRow.add(cancelBtn);
        panel.add(btnRow, BorderLayout.CENTER);

        // 狀態顯示
        researchStatusLbl = new JLabel("狀態：未啟動");
        researchStatusLbl.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        panel.add(researchStatusLbl, BorderLayout.SOUTH);

        // 每秒更新倒數
        clockTimer = new Timer(1000, e -> updateResearchStatus());
        clockTimer.start();

        return panel;
    }

    private void updateResearchStatus() {
        if (manager.isResearchModeActive()) {
            long remaining = manager.getResearchRemainingMs();
            long secs = remaining / 1000;
            researchStatusLbl.setText(String.format(
                "✅ 學習模式啟動中，剩餘 %02d:%02d", secs / 60, secs % 60));
            researchStatusLbl.setForeground(new Color(0, 130, 60));
        } else {
            researchStatusLbl.setText("狀態：未啟動");
            researchStatusLbl.setForeground(Color.DARK_GRAY);
        }
    }
}
