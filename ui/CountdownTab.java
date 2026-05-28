package ui;

import config.ReminderManager;
import model.CountdownEvent;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CountdownTab extends JPanel {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final ReminderManager manager;
    private JTextField nameField;
    private JSpinner   yearSpin, monthSpin, daySpin;
    private JPanel     listPanel;
    private JCheckBox  showArchivedBox;
    private Timer      refreshTimer;

    public CountdownTab(ReminderManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildAddForm(),   BorderLayout.NORTH);
        add(buildListPanel(), BorderLayout.CENTER);

        // 每 30 秒自動更新倒數顯示
        refreshTimer = new Timer(30_000, e -> refreshList());
        refreshTimer.start();
    }

    // ── 新增表單 ────────────────────────────────
    private JPanel buildAddForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("新增倒數事件"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.fill   = GridBagConstraints.HORIZONTAL;

        // 事件名稱
        g.gridx = 0; g.gridy = 0;
        panel.add(new JLabel("事件名稱："), g);
        nameField = new JTextField(16);
        g.gridx = 1; g.gridwidth = 3;
        panel.add(nameField, g);

        // 日期選擇
        g.gridwidth = 1; g.gridx = 0; g.gridy = 1;
        panel.add(new JLabel("目標日期："), g);

        LocalDate today = LocalDate.now();
        yearSpin  = new JSpinner(new SpinnerNumberModel(today.getYear(),  2024, 2030, 1));
        monthSpin = new JSpinner(new SpinnerNumberModel(today.getMonthValue(), 1, 12, 1));
        daySpin   = new JSpinner(new SpinnerNumberModel(today.getDayOfMonth(),  1, 31, 1));
        yearSpin.setPreferredSize(new Dimension(75, 26));
        monthSpin.setPreferredSize(new Dimension(55, 26));
        daySpin.setPreferredSize(new Dimension(55, 26));

        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        dateRow.add(yearSpin);  dateRow.add(new JLabel("年"));
        dateRow.add(monthSpin); dateRow.add(new JLabel("月"));
        dateRow.add(daySpin);   dateRow.add(new JLabel("日"));
        g.gridx = 1; g.gridwidth = 3;
        panel.add(dateRow, g);

        // 新增按鈕
        JButton addBtn = new JButton("＋ 新增");
        addBtn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
        addBtn.addActionListener(e -> addEvent());
        g.gridx = 3; g.gridy = 2; g.gridwidth = 1;
        panel.add(addBtn, g);

        return panel;
    }

    // ── 列表區 ──────────────────────────────────
    private JPanel buildListPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createTitledBorder("倒數列表"));
        wrapper.add(scroll, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        showArchivedBox = new JCheckBox("顯示已歸檔");
        showArchivedBox.addActionListener(e -> refreshList());
        bottomBar.add(showArchivedBox);
        wrapper.add(bottomBar, BorderLayout.SOUTH);

        return wrapper;
    }

    // ── 邏輯 ────────────────────────────────────
    private void addEvent() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "請輸入事件名稱！");
            return;
        }
        try {
            LocalDate target = LocalDate.of(
                (int) yearSpin.getValue(),
                (int) monthSpin.getValue(),
                (int) daySpin.getValue());

            if (target.isBefore(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, "目標日期已過！請選擇未來的日期。");
                return;
            }
            manager.addCountdownEvent(name, target);
            nameField.setText("");
            refreshList();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "日期格式不正確，請確認月份和日期的範圍。");
        }
    }

    public void refreshList() {
        listPanel.removeAll();
        List<CountdownEvent> events = manager.getCountdownEvents();

        // 按剩餘天數排序（最緊迫的在最上面）
        events.sort((a, b) -> Long.compare(a.getDaysRemaining(), b.getDaysRemaining()));

        boolean anyShown = false;
        for (CountdownEvent ev : events) {
            if (ev.isArchived() && !showArchivedBox.isSelected()) continue;
            listPanel.add(buildEventRow(ev));
            anyShown = true;
        }

        if (!anyShown) {
            JLabel empty = new JLabel("目前沒有倒數事件", SwingConstants.CENTER);
            empty.setForeground(Color.GRAY);
            empty.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
            listPanel.add(empty);
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel buildEventRow(CountdownEvent ev) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, urgencyColor(ev)),
            BorderFactory.createEmptyBorder(4, 8, 4, 6)));
        row.setBackground(ev.isArchived() ? new Color(230, 230, 230) : Color.WHITE);

        // 左：名稱 + 日期
        JLabel nameLbl = new JLabel(ev.getName());
        nameLbl.setFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
        if (ev.isArchived()) nameLbl.setForeground(Color.GRAY);

        JLabel dateLbl = new JLabel(ev.getTargetDate().format(DATE_FMT));
        dateLbl.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 11));
        dateLbl.setForeground(Color.GRAY);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(nameLbl);
        left.add(dateLbl);
        row.add(left, BorderLayout.WEST);

        // 中：倒數文字
        JLabel countLbl = new JLabel(countdownText(ev), SwingConstants.CENTER);
        countLbl.setFont(new Font("Microsoft JhengHei", Font.BOLD, 13));
        countLbl.setForeground(urgencyColor(ev));
        row.add(countLbl, BorderLayout.CENTER);

        // 右：按鈕
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        btns.setOpaque(false);
        if (!ev.isArchived()) {
            JButton archBtn = new JButton("歸檔");
            archBtn.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 11));
            archBtn.addActionListener(e -> { manager.archiveEvent(ev.getId()); refreshList(); });
            btns.add(archBtn);
        }
        JButton delBtn = new JButton("✕");
        delBtn.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        delBtn.setMargin(new Insets(0, 4, 0, 4));
        delBtn.addActionListener(e -> { manager.removeCountdownEvent(ev.getId()); refreshList(); });
        btns.add(delBtn);
        row.add(btns, BorderLayout.EAST);

        return row;
    }

    private String countdownText(CountdownEvent ev) {
        if (ev.isArchived()) return "已歸檔";
        long days = ev.getDaysRemaining();
        if (days < 0)  return "已過期";
        if (days == 0) return "⚡ 今天！";
        if (days < 2)  return "🔴 明天";
        if (days < 3)  return "🔴 剩 " + days + " 天";
        if (days < 7)  return "🟡 剩 " + days + " 天";
        return "🟢 剩 " + days + " 天";
    }

    private Color urgencyColor(CountdownEvent ev) {
        if (ev.isArchived()) return Color.GRAY;
        long days = ev.getDaysRemaining();
        if (days < 3)  return new Color(200, 50,  50);
        if (days < 7)  return new Color(200, 140,  0);
        return new Color(30, 140, 60);
    }
}
