package ui;

import config.ReminderManager;
import model.ReminderItem;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReminderTab extends JPanel {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ReminderManager manager;
    private JLabel    periodicStatusLbl;
    private JSpinner  hourSpinner, minSpinner;
    private JTextField labelField;
    private JComboBox<String> priorityBox, repeatBox;
    private JPanel    listPanel;

    public ReminderTab(ReminderManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildPeriodicSection(), BorderLayout.NORTH);
        add(buildCustomSection(),   BorderLayout.CENTER);
    }

    // ── 定時提醒 ──────────────────────────────────
    private JPanel buildPeriodicSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder("定時提醒（循環）"));

        // 快速情境按鈕
        JPanel presets = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        String[][] options = {
            {"🍅 番茄鐘", "25", "專注 25 分鐘到，休息一下！"},
            {"💧 喝水", "60", "咕嚕咕嚕！記得喝水！"},
            {"🧘 拉筋", "120", "站起來伸個懶腰！"},
        };
        for (String[] opt : options) {
            JButton btn = new JButton(opt[0]);
            btn.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
            int mins = Integer.parseInt(opt[1]);
            String msg = opt[2];
            btn.addActionListener(e -> startPeriodic(mins, msg));
            presets.add(btn);
        }

        // 自訂間隔
        JPanel customRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        customRow.add(new JLabel("自訂間隔："));
        JSpinner intervalSpin = new JSpinner(new SpinnerNumberModel(30, 1, 480, 5));
        intervalSpin.setPreferredSize(new Dimension(65, 26));
        customRow.add(intervalSpin);
        customRow.add(new JLabel("分鐘"));
        JTextField customMsg = new JTextField("該休息囉！", 12);
        customRow.add(customMsg);
        JButton startBtn = new JButton("啟動");
        startBtn.addActionListener(e -> startPeriodic(
            (int) intervalSpin.getValue(), customMsg.getText().trim()));
        customRow.add(startBtn);
        presets.add(customRow);
        panel.add(presets, BorderLayout.CENTER);

        // 狀態列
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        periodicStatusLbl = new JLabel("狀態：未啟動");
        periodicStatusLbl.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        JButton stopBtn = new JButton("停止");
        stopBtn.addActionListener(e -> {
            manager.stopPeriodicReminder();
            periodicStatusLbl.setText("狀態：未啟動");
            periodicStatusLbl.setForeground(Color.DARK_GRAY);
        });
        statusRow.add(periodicStatusLbl);
        statusRow.add(stopBtn);
        panel.add(statusRow, BorderLayout.SOUTH);

        return panel;
    }

    private void startPeriodic(int mins, String msg) {
        manager.startPeriodicReminder(mins, msg);
        periodicStatusLbl.setText("⏰ 已啟動：每 " + mins + " 分鐘");
        periodicStatusLbl.setForeground(new Color(0, 130, 60));
    }

    // ── 自訂提醒 ──────────────────────────────────
    private JPanel buildCustomSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder("自訂提醒"));

        // 輸入區
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 3, 3, 3);
        g.fill = GridBagConstraints.HORIZONTAL;

        // 快速時間按鈕
        g.gridx = 0; g.gridy = 0;
        form.add(new JLabel("快速："), g);
        JPanel quickBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        int[]    quickMins  = {10, 30, 60, 120};
        String[] quickLabels = {"10分後", "30分後", "1時後", "2時後"};
        for (int i = 0; i < quickMins.length; i++) {
            JButton b = new JButton(quickLabels[i]);
            b.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 11));
            final int d = quickMins[i];
            b.addActionListener(e -> applyQuickTime(d));
            quickBtns.add(b);
        }
        g.gridx = 1; g.gridwidth = 3;
        form.add(quickBtns, g);

        // HH:MM spinners
        g.gridwidth = 1; g.gridx = 0; g.gridy = 1;
        form.add(new JLabel("時間："), g);
        JPanel timeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        hourSpinner = new JSpinner(new SpinnerNumberModel(LocalDateTime.now().getHour(), 0, 23, 1));
        minSpinner  = new JSpinner(new SpinnerNumberModel(LocalDateTime.now().getMinute(), 0, 59, 1));
        hourSpinner.setPreferredSize(new Dimension(55, 26));
        minSpinner.setPreferredSize(new Dimension(55, 26));
        timeRow.add(hourSpinner);
        timeRow.add(new JLabel(":"));
        timeRow.add(minSpinner);
        g.gridx = 1; g.gridwidth = 3;
        form.add(timeRow, g);

        // 標籤
        g.gridwidth = 1; g.gridx = 0; g.gridy = 2;
        form.add(new JLabel("內容："), g);
        labelField = new JTextField(18);
        g.gridx = 1; g.gridwidth = 3;
        form.add(labelField, g);

        // 重要度 + 重複
        g.gridwidth = 1; g.gridx = 0; g.gridy = 3;
        form.add(new JLabel("重要度："), g);
        priorityBox = new JComboBox<>(new String[]{"🔵 一般", "🟡 重要", "🔴 極重要"});
        g.gridx = 1;
        form.add(priorityBox, g);
        g.gridx = 2;
        form.add(new JLabel("重複："), g);
        repeatBox = new JComboBox<>(new String[]{"僅一次", "每天", "平日（一～五）"});
        g.gridx = 3;
        form.add(repeatBox, g);

        // 新增按鈕
        JButton addBtn = new JButton("＋ 新增提醒");
        addBtn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
        addBtn.addActionListener(e -> addReminder());
        g.gridx = 3; g.gridy = 4;
        g.gridwidth = 1;
        form.add(addBtn, g);

        panel.add(form, BorderLayout.NORTH);

        // 提醒列表
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setPreferredSize(new Dimension(0, 120));
        scroll.setBorder(BorderFactory.createTitledBorder("待觸發提醒"));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void applyQuickTime(int addMinutes) {
        LocalDateTime t = LocalDateTime.now().plusMinutes(addMinutes);
        hourSpinner.setValue(t.getHour());
        minSpinner.setValue(t.getMinute());
    }

    private void addReminder() {
        String label = labelField.getText().trim();
        if (label.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請輸入提醒內容！");
            return;
        }
        int h = (int) hourSpinner.getValue();
        int m = (int) minSpinner.getValue();
        LocalDateTime now    = LocalDateTime.now();
        LocalDateTime target = now.withHour(h).withMinute(m).withSecond(0).withNano(0);

        // 防呆：過去的時間
        if (target.isBefore(now)) {
            int ans = JOptionPane.showConfirmDialog(this,
                "設定的時間已過，是指明天 " + target.format(FMT) + " 嗎？",
                "時間確認", JOptionPane.YES_NO_OPTION);
            if (ans != JOptionPane.YES_OPTION) return;
            target = target.plusDays(1);
        }

        ReminderItem.Priority priority = ReminderItem.Priority.values()[priorityBox.getSelectedIndex()];
        ReminderItem.Repeat   repeat   = ReminderItem.Repeat.values()[repeatBox.getSelectedIndex()];

        ReminderItem item = manager.addCustomReminder(label, target, priority, repeat);
        labelField.setText("");
        addRowToList(item);
    }

    private void addRowToList(ReminderItem item) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        Color bg = priorityColor(item.getPriority());
        row.setBackground(bg);

        JLabel info = new JLabel(item.getTriggerTime().format(FMT) + "  " + item.getLabel());
        info.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        row.add(info, BorderLayout.CENTER);

        JButton del = new JButton("✕");
        del.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        del.setMargin(new Insets(0, 4, 0, 4));
        del.addActionListener(e -> {
            manager.removeCustomReminder(item.getId());
            listPanel.remove(row);
            listPanel.revalidate();
            listPanel.repaint();
        });
        row.add(del, BorderLayout.EAST);

        listPanel.add(row);
        listPanel.revalidate();
        listPanel.repaint();
    }

    private Color priorityColor(ReminderItem.Priority p) {
        switch (p) {
            case URGENT:    return new Color(255, 220, 220);
            case IMPORTANT: return new Color(255, 250, 210);
            default:        return new Color(230, 240, 255);
        }
    }
}
