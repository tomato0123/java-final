package ui;

import config.TodoManager;
import model.TodoItem;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TodoTab extends JPanel {
    private static final DateTimeFormatter DT_NEAR = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    private static final DateTimeFormatter DT_FAR  = DateTimeFormatter.ofPattern("MM/dd");

    private final TodoManager manager;
    private JTextField titleField;
    private JCheckBox  dueDateCheck;
    private JSpinner   yearSpin, monthSpin, daySpin, hourSpin, minSpin;
    private JPanel     listPanel;
    private JCheckBox  showCompletedBox;

    public TodoTab(TodoManager manager) {
        this.manager = manager;
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildAddForm(),   BorderLayout.NORTH);
        add(buildListPanel(), BorderLayout.CENTER);
        refreshList();
    }

    // ── 新增表單 ────────────────────────────────
    private JPanel buildAddForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("新增待辦事項"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.fill   = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0;
        panel.add(new JLabel("任務名稱："), g);
        titleField = new JTextField(18);
        g.gridx = 1; g.gridwidth = 3;
        panel.add(titleField, g);

        g.gridwidth = 4; g.gridx = 0; g.gridy = 1;
        dueDateCheck = new JCheckBox("設定截止時間");
        dueDateCheck.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        panel.add(dueDateCheck, g);

        g.gridwidth = 1; g.gridx = 0; g.gridy = 2;
        panel.add(new JLabel("截止時間："), g);

        LocalDateTime init = LocalDateTime.now().plusDays(1).withHour(23).withMinute(59);
        yearSpin  = new JSpinner(new SpinnerNumberModel(init.getYear(), 2024, 2030, 1));
        monthSpin = new JSpinner(new SpinnerNumberModel(init.getMonthValue(), 1, 12, 1));
        daySpin   = new JSpinner(new SpinnerNumberModel(init.getDayOfMonth(), 1, 31, 1));
        hourSpin  = new JSpinner(new SpinnerNumberModel(23, 0, 23, 1));
        minSpin   = new JSpinner(new SpinnerNumberModel(59, 0, 59, 1));
        for (JSpinner s : new JSpinner[]{yearSpin}) s.setPreferredSize(new Dimension(70, 26));
        for (JSpinner s : new JSpinner[]{monthSpin, daySpin, hourSpin, minSpin})
            s.setPreferredSize(new Dimension(52, 26));

        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        dateRow.add(yearSpin);  dateRow.add(new JLabel("年"));
        dateRow.add(monthSpin); dateRow.add(new JLabel("月"));
        dateRow.add(daySpin);   dateRow.add(new JLabel("日"));
        dateRow.add(hourSpin);  dateRow.add(new JLabel(":"));
        dateRow.add(minSpin);
        g.gridx = 1; g.gridwidth = 3;
        panel.add(dateRow, g);

        setDateRowEnabled(false);
        dueDateCheck.addActionListener(e -> setDateRowEnabled(dueDateCheck.isSelected()));

        JButton addBtn = new JButton("＋ 新增");
        addBtn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 12));
        addBtn.addActionListener(e -> addItem());
        g.gridx = 3; g.gridy = 3; g.gridwidth = 1;
        panel.add(addBtn, g);

        return panel;
    }

    private void setDateRowEnabled(boolean on) {
        yearSpin.setEnabled(on);
        monthSpin.setEnabled(on);
        daySpin.setEnabled(on);
        hourSpin.setEnabled(on);
        minSpin.setEnabled(on);
    }

    // ── 列表區 ──────────────────────────────────
    private JPanel buildListPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createTitledBorder("待辦清單"));
        wrapper.add(scroll, BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        showCompletedBox = new JCheckBox("顯示已完成");
        showCompletedBox.addActionListener(e -> refreshList());
        bar.add(showCompletedBox);
        wrapper.add(bar, BorderLayout.SOUTH);
        return wrapper;
    }

    // ── 邏輯 ────────────────────────────────────
    private void addItem() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) { JOptionPane.showMessageDialog(this, "請輸入任務名稱！"); return; }
        LocalDateTime dt = null;
        if (dueDateCheck.isSelected()) {
            try {
                dt = LocalDateTime.of(
                    (int) yearSpin.getValue(), (int) monthSpin.getValue(),
                    (int) daySpin.getValue(),  (int) hourSpin.getValue(),
                    (int) minSpin.getValue());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "日期格式不正確！");
                return;
            }
        }
        manager.addItem(title, dt);
        titleField.setText("");
        dueDateCheck.setSelected(false);
        setDateRowEnabled(false);
        refreshList();
    }

    public void refreshList() {
        listPanel.removeAll();
        List<TodoItem> items = manager.getAll();

        // Sort: incomplete first (nearest due date first), then completed
        items.sort((a, b) -> {
            if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
            if (a.getDueDateTime() != null && b.getDueDateTime() != null)
                return a.getDueDateTime().compareTo(b.getDueDateTime());
            if (a.getDueDateTime() != null) return -1;
            if (b.getDueDateTime() != null) return 1;
            return 0;
        });

        boolean anyShown = false;
        for (TodoItem item : items) {
            if (item.isCompleted() && !showCompletedBox.isSelected()) continue;
            listPanel.add(buildRow(item));
            anyShown = true;
        }
        if (!anyShown) {
            JLabel empty = new JLabel("太棒了！目前沒有待辦事項 ✓", SwingConstants.CENTER);
            empty.setForeground(Color.GRAY);
            empty.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            listPanel.add(empty);
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel buildRow(TodoItem item) {
        boolean overdue = !item.isCompleted()
            && item.getDueDateTime() != null
            && item.getDueDateTime().isBefore(LocalDateTime.now());

        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0,
                item.isCompleted() ? Color.LIGHT_GRAY
                    : overdue       ? new Color(200, 50, 50)
                    : new Color(60, 130, 240)),
            BorderFactory.createEmptyBorder(4, 8, 4, 6)));
        row.setBackground(item.isCompleted() ? new Color(242, 242, 242) : Color.WHITE);

        // 左：勾選框
        JCheckBox cb = new JCheckBox();
        cb.setSelected(item.isCompleted());
        cb.setOpaque(false);
        cb.addActionListener(e -> { manager.setCompleted(item.getId(), cb.isSelected()); refreshList(); });
        row.add(cb, BorderLayout.WEST);

        // 中：名稱 + 截止時間
        JLabel titleLbl = new JLabel(item.getTitle());
        titleLbl.setFont(new Font("Microsoft JhengHei", Font.BOLD, 13));
        if (item.isCompleted()) titleLbl.setForeground(new Color(160, 160, 160));
        else if (overdue)       titleLbl.setForeground(new Color(200, 50, 50));

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        center.setOpaque(false);
        center.add(titleLbl);
        if (item.getDueDateTime() != null) {
            boolean near = item.getDueDateTime().isBefore(LocalDateTime.now().plusDays(2));
            JLabel dtLbl = new JLabel(item.getDueDateTime().format(near ? DT_NEAR : DT_FAR));
            dtLbl.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 11));
            dtLbl.setForeground(overdue ? new Color(200, 50, 50) : Color.GRAY);
            center.add(dtLbl);
        }
        row.add(center, BorderLayout.CENTER);

        // 右：編輯 + 刪除
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        btns.setOpaque(false);
        if (!item.isCompleted()) {
            JButton editBtn = new JButton("編輯");
            editBtn.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 11));
            editBtn.addActionListener(e -> openEditDialog(item));
            btns.add(editBtn);
        }
        JButton delBtn = new JButton("✕");
        delBtn.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        delBtn.setMargin(new Insets(0, 4, 0, 4));
        delBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "確定要刪除「" + item.getTitle() + "」嗎？", "確認刪除", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) { manager.deleteItem(item.getId()); refreshList(); }
        });
        btns.add(delBtn);
        row.add(btns, BorderLayout.EAST);
        return row;
    }

    private void openEditDialog(TodoItem item) {
        JTextField tf = new JTextField(item.getTitle(), 22);
        LocalDateTime ex = item.getDueDateTime() != null ? item.getDueDateTime()
            : LocalDateTime.now().plusDays(1).withHour(23).withMinute(59);
        JCheckBox hasDue = new JCheckBox("設定截止時間", item.getDueDateTime() != null);
        JSpinner ys = new JSpinner(new SpinnerNumberModel(ex.getYear(), 2024, 2030, 1));
        JSpinner ms = new JSpinner(new SpinnerNumberModel(ex.getMonthValue(), 1, 12, 1));
        JSpinner ds = new JSpinner(new SpinnerNumberModel(ex.getDayOfMonth(), 1, 31, 1));
        JSpinner hs = new JSpinner(new SpinnerNumberModel(ex.getHour(), 0, 23, 1));
        JSpinner ns = new JSpinner(new SpinnerNumberModel(ex.getMinute(), 0, 59, 1));
        ys.setPreferredSize(new Dimension(70, 26));
        for (JSpinner s : new JSpinner[]{ms, ds, hs, ns}) s.setPreferredSize(new Dimension(52, 26));

        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        dateRow.add(ys); dateRow.add(new JLabel("年"));
        dateRow.add(ms); dateRow.add(new JLabel("月"));
        dateRow.add(ds); dateRow.add(new JLabel("日"));
        dateRow.add(hs); dateRow.add(new JLabel(":"));
        dateRow.add(ns);

        Runnable toggleEnabled = () -> {
            boolean en = hasDue.isSelected();
            for (JSpinner s : new JSpinner[]{ys, ms, ds, hs, ns}) s.setEnabled(en);
        };
        hasDue.addActionListener(e -> toggleEnabled.run());
        toggleEnabled.run();

        JPanel content = new JPanel(new GridLayout(3, 1, 4, 6));
        content.add(tf);
        content.add(hasDue);
        content.add(dateRow);

        int result = JOptionPane.showConfirmDialog(this, content, "編輯待辦事項",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String title = tf.getText().trim();
        if (title.isEmpty()) { JOptionPane.showMessageDialog(this, "名稱不能為空！"); return; }
        LocalDateTime dt = null;
        if (hasDue.isSelected()) {
            try {
                dt = LocalDateTime.of((int)ys.getValue(), (int)ms.getValue(),
                    (int)ds.getValue(), (int)hs.getValue(), (int)ns.getValue());
            } catch (Exception ex2) {
                JOptionPane.showMessageDialog(this, "日期格式不正確！");
                return;
            }
        }
        manager.updateItem(item.getId(), title, dt);
        refreshList();
    }
}
