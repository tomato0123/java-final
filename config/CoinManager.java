package config;

import java.io.*;
import java.util.Properties;

public class CoinManager {
    public static final int BASE_REWARD     = 50;   // coins per pomodoro at 1x
    public static final int RELAX_PASS_COST = 200;  // coins to buy 10-min relax pass

    private static final String FILE = "coins.properties";

    private int coins = 0;
    private int combo = 1;  // starts at 1x, increments with each successful pomodoro

    public CoinManager() { load(); }

    /** Call when a full pomodoro session ends without a forced break. */
    public void onPomodoroCompleted() {
        coins += BASE_REWARD * combo;
        combo++;
        save();
    }

    /** Resets combo to 1x (called on distraction or phone violation). */
    public void breakCombo() {
        if (combo > 1) { combo = 1; save(); }
    }

    /** Deducts coins. Returns false if balance is insufficient. */
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
        p.setProperty("combo", String.valueOf(combo));
        try (OutputStream out = new FileOutputStream(FILE)) {
            p.store(out, null);
        } catch (IOException ignored) {}
    }
}
