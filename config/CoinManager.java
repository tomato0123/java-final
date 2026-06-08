package config;

import java.io.*;
import java.util.Properties;

public class CoinManager {
    public static final int BASE_REWARD     = 50;
    public static final int RELAX_PASS_COST = 200;

    private static final String FILE = "coins.properties";

    private int coins = 0;
    private int combo = 1;

    public CoinManager() { load(); }

    /** 每完成一個番茄鐘呼叫：依目前倍率發幣，然後 combo++。 */
    public void onPomodoroCompleted() {
        coins += BASE_REWARD * combo;
        combo++;
        save();
    }

    /** 分心或手機違規時重設連擊倍率為 1x。 */
    public void breakCombo() {
        if (combo > 1) { combo = 1; save(); }
    }

    /** 扣除金幣。餘額不足回傳 false。 */
    public boolean spendCoins(int amount) {
        if (coins < amount) return false;
        coins -= amount;
        save();
        return true;
    }

    public int getCoins()      { return coins; }
    public int getCombo()      { return combo; }
    public int getNextReward() { return BASE_REWARD * combo; }

    private void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(f)) {
            p.load(in);
            coins = Integer.parseInt(p.getProperty("coins", "0"));
            combo = Math.max(1, Integer.parseInt(p.getProperty("combo", "1")));
        } catch (Exception ignored) {}
    }

    private void save() {
        Properties p = new Properties();
        p.setProperty("coins", String.valueOf(coins));
        p.setProperty("combo",  String.valueOf(combo));
        try (OutputStream out = new FileOutputStream(FILE)) {
            p.store(out, null);
        } catch (IOException ignored) {}
    }
}
