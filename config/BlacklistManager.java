package config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class BlacklistManager {
    private static final String FILE = "blacklist.txt";
    private static final List<String> DEFAULTS = Arrays.asList(
        "YouTube", "Facebook", "Instagram", "Netflix",
        "Twitter", "TikTok", "Reddit", "Twitch", "PTT"
    );

    private final List<String> keywords = new ArrayList<>();

    // 學習模式豁免
    private boolean researchMode = false;
    private long    researchEndMs = 0;

    public BlacklistManager() {
        load();
    }

    // ── 黑名單管理 ────────────────────────────────
    public List<String> getKeywords() {
        return Collections.unmodifiableList(keywords);
    }

    public void addKeyword(String kw) {
        if (!kw.isBlank() && !keywords.contains(kw)) {
            keywords.add(kw);
            save();
        }
    }

    public void removeKeyword(String kw) {
        keywords.remove(kw);
        save();
    }

    /** 檢查視窗標題是否符合黑名單（學習模式期間豁免） */
    public boolean isDistraction(String windowTitle) {
        if (windowTitle == null || windowTitle.isBlank()) return false;
        if (isResearchModeActive()) return false;
        String lower = windowTitle.toLowerCase();
        return keywords.stream().anyMatch(k -> lower.contains(k.toLowerCase()));
    }

    /** 取得匹配到的關鍵字（用於顯示）*/
    public String matchedKeyword(String windowTitle) {
        if (windowTitle == null) return "";
        String lower = windowTitle.toLowerCase();
        return keywords.stream()
            .filter(k -> lower.contains(k.toLowerCase()))
            .findFirst().orElse("");
    }

    // ── 學習模式豁免 ──────────────────────────────
    public void startResearchMode(int minutes) {
        researchMode   = true;
        researchEndMs  = System.currentTimeMillis() + minutes * 60_000L;
    }

    public void cancelResearchMode() {
        researchMode = false;
    }

    public boolean isResearchModeActive() {
        if (!researchMode) return false;
        if (System.currentTimeMillis() > researchEndMs) {
            researchMode = false;
            return false;
        }
        return true;
    }

    public long getResearchRemainingMs() {
        return researchMode ? Math.max(0, researchEndMs - System.currentTimeMillis()) : 0;
    }

    // ── 持久化 ────────────────────────────────────
    private void save() {
        try {
            Files.write(Paths.get(FILE), keywords, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("無法儲存黑名單: " + e.getMessage());
        }
    }

    private void load() {
        Path p = Paths.get(FILE);
        if (Files.exists(p)) {
            try {
                Files.lines(p, StandardCharsets.UTF_8)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .forEach(keywords::add);
            } catch (IOException e) {
                System.err.println("無法讀取黑名單: " + e.getMessage());
            }
        }
        if (keywords.isEmpty()) {
            keywords.addAll(DEFAULTS);
            save();
        }
    }
}
