package ui;

import config.TodoManager;
import model.TodoItem;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Pre-focus modal dialog: user optionally picks ONE active todo as the current task.
 * Call isCancelled() to detect "X / Cancel". getSelectedTitle() returns chosen title or null.
 */
public class TaskSelectDialog extends JDialog {
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("MM/dd HH:mm");

    private String  selectedTitle = null;
    private boolean cancelled     = true;

    public TaskSelectDialog(Window parent, TodoManager todoManager) {
        super(parent, "選擇本次專注任務", ModalityType.APPLICATION_MODAL);
        setSize(400, 380);
        setLocationRelativeTo(null);  // center on screen
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        List<TodoItem> active = todoManager.getActive();

        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));

        // Header
        JLabel header = new JLabel(
            "<html><b style='font-size:13px'>選擇你現在要完成的任務</b><br>"
            + "<span style='color:#888;font-size:10px'>"
            + "選一項後，分心時寵物會精準提醒你喔！也可以跳過。</span></html>");
        header.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 13));
        main.add(header, BorderLayout.NORTH);

        // Task list
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        ButtonGroup group = new ButtonGroup();

        // "No task" option (default)
        JRadioButton noneBtn = buildRadio("不指定任務（自由專注）", false, group);
        noneBtn.setFont(new Font("Microsoft JhengHei", Font.ITALIC, 12));
        noneBtn.setSelected(true);
        noneBtn.addActionListener(e -> selectedTitle = null);
        listPanel.add(noneBtn);

        if (!active.isEmpty()) {
            listPanel.add(Box.createVerticalStrut(6));
            JSeparator sep = new JSeparator();
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            listPanel.add(sep);
            listPanel.add(Box.createVerticalStrut(6));

            for (TodoItem item : active) {
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);

                JRadioButton rb = buildRadio(item.getTitle(), true, group);
                rb.addActionListener(e -> selectedTitle = item.getTitle());
                row.add(rb, BorderLayout.CENTER);

                if (item.getDueDateTime() != null) {
                    JLabel dtLbl = new JLabel(item.getDueDateTime().format(DT_FMT));
                    dtLbl.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 11));
                    dtLbl.setForeground(Color.GRAY);
                    row.add(dtLbl, BorderLayout.EAST);
                }
                listPanel.add(row);
                listPanel.add(Box.createVerticalStrut(3));
            }
        } else {
            listPanel.add(Box.createVerticalStrut(8));
            JLabel noItems = new JLabel("（待辦清單是空的，可先到「待辦事項」頁籤新增）");
            noItems.setFont(new Font("Microsoft JhengHei", Font.ITALIC, 12));
            noItems.setForeground(Color.GRAY);
            noItems.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(noItems);
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210)));
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        main.add(scroll, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancelBtn = new JButton("取消");
        cancelBtn.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        cancelBtn.addActionListener(e -> dispose()); // cancelled stays true

        JButton startBtn = new JButton("▶ 開始專注");
        startBtn.setFont(new Font("Microsoft JhengHei", Font.BOLD, 13));
        startBtn.setForeground(new Color(0, 120, 0));
        startBtn.addActionListener(e -> { cancelled = false; dispose(); });

        btnPanel.add(cancelBtn);
        btnPanel.add(startBtn);
        main.add(btnPanel, BorderLayout.SOUTH);

        add(main);
    }

    private JRadioButton buildRadio(String text, boolean bold, ButtonGroup group) {
        JRadioButton rb = new JRadioButton(text);
        rb.setFont(new Font("Microsoft JhengHei", bold ? Font.BOLD : Font.PLAIN, 13));
        rb.setOpaque(false);
        rb.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(rb);
        return rb;
    }

    public boolean isCancelled()      { return cancelled; }
    public String  getSelectedTitle() { return selectedTitle; }
}
