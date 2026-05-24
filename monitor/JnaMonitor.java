package monitor;

import ui.RootFrame;

public class JnaMonitor implements Runnable {
    private RootFrame root;
    private boolean isRunning = true;

    public JnaMonitor(RootFrame root) {
        this.root = root;
    }

    @Override
    public void run() {
        System.out.println("系統監控模組已啟動...");
        
        while (isRunning) {
            try {
                // 每秒偵測一次
                Thread.sleep(1000);
                
                // TODO: 這裡之後要換成真正的 JNA 程式碼去抓視窗標題
                // String activeWindowTitle = JnaUtil.getActiveWindowTitle();
                
                // ［模擬測試］：假設有 5% 的機率模擬使用者打開了黑名單視窗
                if (Math.random() < 0.05) {
                    System.out.println("[監控通知] 偵測到使用者打開了分心軟體！");
                    root.updatePetState("distracted", "電腦打開 Twitch");
                }
                
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopMonitoring() {
        this.isRunning = false;
    }
}