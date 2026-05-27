package ui;

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
    private String bubbleText = "今天也要好好寫程式！";

    private BufferedImage[] normalFrames = new BufferedImage[8];
    private BufferedImage[] happyFrames  = new BufferedImage[8];
    private BufferedImage[] sleepFrames  = new BufferedImage[8];
    private BufferedImage[] walkRightFrames = new BufferedImage[8];
    private BufferedImage[] walkLeftFrames  = new BufferedImage[8];

    private int currentFrameIndex = 0;
    private Timer animationTimer;
    private Random random = new Random();

    private int initialClickX;
    private int initialClickY;
    private JFrame root;
    private boolean isWanderingAllowed = true;

    public PetPanel(JFrame root) {
        this.root = root;
        setOpaque(false);
        loadImages();

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
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                int thisX = root.getLocation().x;
                int thisY = root.getLocation().y;
                int xMoved = e.getX() - initialClickX;
                int yMoved = e.getY() - initialClickY;
                root.setLocation(thisX + xMoved, thisY + yMoved);
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                // 判斷是否為「按下滑鼠右鍵」
                if (SwingUtilities.isRightMouseButton(e)) {
                    setState("sleep", "Zzz... 系統閒置中...");
                } 
                // 否則就是「按下滑鼠左鍵」
                else {
                    // 如果現在是開心，就切回正常；反之則切換到開心
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

    private void loadImages() {
        try {
            for(int i = 0; i < 8; i++) {
                normalFrames[i]    = ImageIO.read(new File("resources/blue-normal" + (i+1) + ".png"));
                happyFrames[i]     = ImageIO.read(new File("resources/blue-happy"  + (i+1) + ".png"));
                sleepFrames[i]     = ImageIO.read(new File("resources/blue-sleep"  + (i+1) + ".png"));
                walkRightFrames[i] = ImageIO.read(new File("resources/blue-walkR"  + (i+1) + ".png"));
                walkLeftFrames[i]  = ImageIO.read(new File("resources/blue-walkL"  + (i+1) + ".png"));
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void updatePetBehavior() {
        if (!isWanderingAllowed && (currentState.equals("walk_left") || currentState.equals("walk_right"))) {
            currentState = "normal";
            currentFrameIndex = 0;
        }

        if ("happy".equals(currentState)) {
            currentFrameIndex = (currentFrameIndex + 1) % 8;
        } else if ("sleep".equals(currentState)) {
            currentFrameIndex = (currentFrameIndex + 1) % 8;
        } else if ("normal".equals(currentState)) {
            currentFrameIndex = (currentFrameIndex + 1) % 8;
            if (isWanderingAllowed && random.nextInt(100) < 5) {
                currentState = random.nextBoolean() ? "walk_left" : "walk_right";
                currentFrameIndex = 0;
            }
        } else if ("walk_left".equals(currentState)) {
            currentFrameIndex = (currentFrameIndex + 1) % 8;
            root.setLocation(root.getLocation().x - 5, root.getLocation().y);
            if (random.nextInt(100) < 5) {
                currentState = "normal";
                currentFrameIndex = 0;
            }
        } else if ("walk_right".equals(currentState)) {
            currentFrameIndex = (currentFrameIndex + 1) % 8;
            root.setLocation(root.getLocation().x + 5, root.getLocation().y);
            if (random.nextInt(100) < 5) {
                currentState = "normal";
                currentFrameIndex = 0;
            }
        }
    }

    public void setState(String state, String message) {
        this.currentState = state;
        this.bubbleText = message;
        this.currentFrameIndex = 0;
        repaint();
    }

    public void toggleWandering() {
        this.isWanderingAllowed = !this.isWanderingAllowed;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        BufferedImage currentImage = null;

        if ("normal".equals(currentState) && normalFrames[currentFrameIndex] != null) {
            currentImage = normalFrames[currentFrameIndex];
        } else if ("happy".equals(currentState) && happyFrames[currentFrameIndex] != null) {
            currentImage = happyFrames[currentFrameIndex];
        } else if ("sleep".equals(currentState) && sleepFrames[currentFrameIndex] != null) {
            currentImage = sleepFrames[currentFrameIndex];
        } else if ("walk_left".equals(currentState) && walkLeftFrames[currentFrameIndex] != null) {
            currentImage = walkLeftFrames[currentFrameIndex];
        } else if ("walk_right".equals(currentState) && walkRightFrames[currentFrameIndex] != null) {
            currentImage = walkRightFrames[currentFrameIndex];
        }

        if (currentImage != null) {
            int imgWidth = currentImage.getWidth();
            int imgHeight = currentImage.getHeight();
            int drawX = (300 - imgWidth) / 2;
            int drawY = 280 - imgHeight;
            g2d.drawImage(currentImage, drawX, drawY, null);
        } else {
            g2d.setColor(Color.RED);
            g2d.fillRect(100, 100, 100, 100);
            g2d.setColor(Color.BLACK);
            g2d.drawString("無素材", 130, 150);
        }

        g2d.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 16));
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillRoundRect(30, 40, 250, 40, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawString(bubbleText, 45, 65);
    }
}