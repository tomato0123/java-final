package ui;

import config.ConfigManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class PetPanel extends JPanel {
    private String currentState = "normal";
    private String bubbleText   = "今天也要好好寫程式！";
    private String currentPetType;

    private BufferedImage[] normalFrames     = new BufferedImage[8];
    private BufferedImage[] happyFrames      = new BufferedImage[8];
    private BufferedImage[] sleepFrames      = new BufferedImage[8];
    private BufferedImage[] walkRightFrames  = new BufferedImage[8];
    private BufferedImage[] walkLeftFrames   = new BufferedImage[8];

    private int     currentFrameIndex = 0;
    private Timer   animationTimer;
    private Random  random = new Random();

    private int        initialClickX, initialClickY;
    private RootFrame  root;
    private boolean    isWanderingAllowed;
    private JPopupMenu popupMenu;

    // ── 警告紅色疊層 ──
    private boolean alertActive = false;
    private Timer   alertTimer;

    public PetPanel(RootFrame root) {
        this.root = root;
        setOpaque(false);

        reloadSettings();
        setupPopupMenu();

        animationTimer = new Timer(150, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updatePetBehavior();
                repaint();
            }
        });
        animationTimer.start();

        MouseAdapter dragAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialClickX = e.getX();
                initialClickY = e.getY();
                root.onPetActivity(); // 任何按下都算互動，恢復透明度
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                int xMoved = e.getX() - initialClickX;
                int yMoved = e.getY() - initialClickY;
                root.setLocation(root.getLocation().x + xMoved,
                                 root.getLocation().y + yMoved);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                root.onPetActivity();
                if (SwingUtilities.isRightMouseButton(e)) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                } else {
                    if ("happy".equals(currentState)) {
                        setState("normal", "戳我幹嘛？快去讀書！");
                    } else {
                        setState("happy", "太棒了！繼續保持專注！");
                    }
                }
            }
        };
        addMouseListener(dragAdapter);
        addMouseMotionListener(dragAdapter);
    }

    // ════════════════════════════════════════════
    //  公開 API
    // ════════════════════════════════════════════

    /** 顯示 2.5 秒紅色警告疊層 */
    public void showAlert() {
        alertActive = true;
        if (alertTimer != null) alertTimer.stop();
        alertTimer = new Timer(2500, e -> {
            alertActive = false;
            repaint();
            ((Timer) e.getSource()).stop();
        });
        alertTimer.start();
        repaint();
    }

    /** 違規時使用：切換至 angry 狀態 + 觸發紅色警告 */
    public void triggerViolation(String message) {
        setState("angry", message);
        root.triggerAlert();
    }

    public void setState(String state, String message) {
        this.currentState     = state;
        this.bubbleText       = message;
        this.currentFrameIndex = 0;
        repaint();
    }

    public void reloadSettings() {
        this.currentPetType    = ConfigManager.getPetType();
        this.isWanderingAllowed = ConfigManager.isWanderAllowed();
        loadImages();
        repaint();
    }

    // ════════════════════════════════════════════
    //  彈出選單
    // ════════════════════════════════════════════
    private void setupPopupMenu() {
        popupMenu = new JPopupMenu();

        JMenuItem dashboardItem = new JMenuItem("開啟控制面板");
        dashboardItem.addActionListener(e -> new DashboardFrame(root).setVisible(true));

        JMenuItem focusItem = new JMenuItem("開始專注");
        focusItem.addActionListener(e -> root.startFocusSession());

        JMenu leaveMenu = new JMenu("申請請假");
        for (int min : new int[]{1, 3, 5, 10}) {
            final int m = min;
            JMenuItem item = new JMenuItem(m + " 分鐘");
            item.addActionListener(e -> root.applyForLeave(m));
            leaveMenu.add(item);
        }

        JMenuItem sleepItem = new JMenuItem("進入休眠");
        sleepItem.addActionListener(e -> setState("sleep", "Zzz... 系統閒置中..."));

        JMenuItem trayItem = new JMenuItem("最小化到工作列");
        trayItem.addActionListener(e -> root.hideToTray());

        JMenuItem exitItem = new JMenuItem("關閉程式");
        exitItem.addActionListener(e -> System.exit(0));

        popupMenu.add(dashboardItem);
        popupMenu.add(focusItem);
        popupMenu.add(leaveMenu);
        popupMenu.add(sleepItem);
        popupMenu.add(trayItem);
        popupMenu.addSeparator();
        popupMenu.add(exitItem);
    }

    // ════════════════════════════════════════════
    //  圖片載入
    // ════════════════════════════════════════════
    private String getFolderName(String petType) {
        switch (petType) {
            case "blue":  return "blueSprite";
            case "fire":  return "fireSprite";
            case "water": return "waterSprite";
            default:      return petType;
        }
    }

    private void loadImages() {
        String folder = "resources/" + getFolderName(currentPetType) + "/";
        String prefix = currentPetType + "-";
        try {
            for (int i = 0; i < 8; i++) {
                normalFrames[i]    = ImageIO.read(new File(folder + prefix + "normal" + (i+1) + ".png"));
                happyFrames[i]     = ImageIO.read(new File(folder + prefix + "happy"  + (i+1) + ".png"));
                sleepFrames[i]     = ImageIO.read(new File(folder + prefix + "sleep"  + (i+1) + ".png"));
                walkRightFrames[i] = ImageIO.read(new File(folder + prefix + "walkR"  + (i+1) + ".png"));
                walkLeftFrames[i]  = ImageIO.read(new File(folder + prefix + "walkL"  + (i+1) + ".png"));
            }
        } catch (IOException e) {
            System.err.println("載入圖片失敗（" + currentPetType + "）: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════
    //  動畫邏輯
    // ════════════════════════════════════════════
    private void updatePetBehavior() {
        if (!isWanderingAllowed &&
                (currentState.equals("walk_left") || currentState.equals("walk_right"))) {
            currentState      = "normal";
            currentFrameIndex = 0;
        }

        if ("happy".equals(currentState) || "sleep".equals(currentState)) {
            currentFrameIndex = (currentFrameIndex + 1) % 8;
        } else if ("normal".equals(currentState)) {
            currentFrameIndex = (currentFrameIndex + 1) % 8;
            if (isWanderingAllowed && random.nextInt(100) < 5) {
                currentState      = random.nextBoolean() ? "walk_left" : "walk_right";
                currentFrameIndex = 0;
            }
        } else if ("walk_left".equals(currentState)) {
            currentFrameIndex = (currentFrameIndex + 1) % 8;
            root.setLocation(root.getLocation().x - 5, root.getLocation().y);
            if (random.nextInt(100) < 5) { currentState = "normal"; currentFrameIndex = 0; }
        } else if ("walk_right".equals(currentState)) {
            currentFrameIndex = (currentFrameIndex + 1) % 8;
            root.setLocation(root.getLocation().x + 5, root.getLocation().y);
            if (random.nextInt(100) < 5) { currentState = "normal"; currentFrameIndex = 0; }
        }
    }

    // ════════════════════════════════════════════
    //  繪圖
    // ════════════════════════════════════════════
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ── 套用淡出透明度 ──
        float opacity = root.getPetOpacity();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

        // ── 繪製角色圖片 ──
        BufferedImage currentImage = null;
        if      ("normal".equals(currentState)     && normalFrames[currentFrameIndex]    != null)
            currentImage = normalFrames[currentFrameIndex];
        else if ("happy".equals(currentState)      && happyFrames[currentFrameIndex]     != null)
            currentImage = happyFrames[currentFrameIndex];
        else if ("sleep".equals(currentState)      && sleepFrames[currentFrameIndex]     != null)
            currentImage = sleepFrames[currentFrameIndex];
        else if ("walk_left".equals(currentState)  && walkLeftFrames[currentFrameIndex]  != null)
            currentImage = walkLeftFrames[currentFrameIndex];
        else if ("walk_right".equals(currentState) && walkRightFrames[currentFrameIndex] != null)
            currentImage = walkRightFrames[currentFrameIndex];

        if (currentImage != null) {
            int drawX = (300 - currentImage.getWidth())  / 2;
            int drawY = 280  - currentImage.getHeight();
            g2d.drawImage(currentImage, drawX, drawY, null);
        } else {
            g2d.setColor(Color.RED);
            g2d.fillRect(100, 100, 100, 100);
            g2d.setColor(Color.BLACK);
            g2d.drawString("無素材", 130, 150);
        }

        // ── 對話氣泡 ──
        g2d.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 16));
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillRoundRect(30, 40, 250, 40, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawString(bubbleText, 45, 65);

        // ── 紅色警告疊層（恢復全不透明後疊加） ──
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        if (alertActive) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
            g2d.setColor(new Color(220, 0, 0));
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
        }
    }
}
