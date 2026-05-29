package monitor;

import config.BlacklistManager;
import ui.RootFrame;
import ui.ToastNotification;

import javax.swing.*;
import java.awt.*;

/**
 * 漸進式違規警告：
 *   Stage 1 (@2s)  — 寵物憤怒 + Toast 通知
 *   Stage 2 (@5s)  — 系統嗶聲 + 寵物移到螢幕中央
 *   Stage 3 (@10s) — 觸發「專注失敗」懲罰
 * 非專注模式時只在 Stage 1 提醒。
 */
public class JnaMonitor implements WindowMonitor.WindowTitleListener {

    private final RootFrame     root;
    private final WindowMonitor windowMonitor;

    private int  warnStage       = 0;
    private long distractionStart = 0;

    public JnaMonitor(RootFrame root) {
        this.root          = root;
        this.windowMonitor = new WindowMonitor();
        windowMonitor.addListener(this);
    }

    public void start() { windowMonitor.start(); }
    public void stop()  { windowMonitor.stop();  }

    @Override
    public void onWindowTitle(String title) {
        if (!root.isFocusActive()) {
            resetWarning();
            return;
        }
        BlacklistManager bm = root.getBlacklistManager();
        if (bm.isDistraction(title)) {
            if (distractionStart == 0) distractionStart = System.currentTimeMillis();
            long elapsed = System.currentTimeMillis() - distractionStart;
            escalate(elapsed, bm.matchedKeyword(title));
        } else {
            resetWarning();
        }
    }

    private void escalate(long elapsedMs, String keyword) {
        boolean focusActive = root.isFocusActive();

        if (focusActive && elapsedMs >= 10_000 && warnStage < 3) {
            warnStage = 3;
            SwingUtilities.invokeLater(() -> root.triggerFocusFailed(keyword));

        } else if (focusActive && elapsedMs >= 5_000 && warnStage < 2) {
            warnStage = 2;
            Toolkit.getDefaultToolkit().beep();
            SwingUtilities.invokeLater(() -> root.moveToCenter());

        } else if (elapsedMs >= 2_000 && warnStage < 1) {
            warnStage = 1;
            SwingUtilities.invokeLater(() -> {
                root.triggerAlert();
                root.getPetPanel().setState("angry", "偵測到分心軟體：" + keyword + "！");
                ToastNotification.show(
                    "⚠ 分心警告",
                    "偵測到「" + keyword + "」，請回到學習！",
                    () -> {},
                    () -> {}
                );
            });
        }
    }

    private void resetWarning() {
        if (distractionStart != 0) {
            warnStage        = 0;
            distractionStart = 0;
            SwingUtilities.invokeLater(() ->
                root.getPetPanel().setState("normal", "很好！繼續保持專注！"));
        }
    }
}
