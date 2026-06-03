package config;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class FocusStatsManager {
    private static final String STATS_FILE = "focus_stats.csv";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Map<String, long[]> dailyData = new LinkedHashMap<>();

    private long sessionStartMs      = 0;
    private int  sessionDistractions = 0;

    public FocusStatsManager() { load(); }

    public void onFocusStart() {
        sessionStartMs       = System.currentTimeMillis();
        sessionDistractions  = 0;
    }

    public void onDistraction() { sessionDistractions++; }

    public void onFocusEnd() {
        if (sessionStartMs == 0) return;
        long   duration = System.currentTimeMillis() - sessionStartMs;
        String today    = LocalDate.now().format(FMT);
        long[] data     = dailyData.computeIfAbsent(today, k -> new long[]{0, 0});
        data[0] += duration;
        data[1] += sessionDistractions;
        sessionStartMs       = 0;
        sessionDistractions  = 0;
        save();
    }

    public long getTodayFocusMs() {
        String today = LocalDate.now().format(FMT);
        long[] data  = dailyData.get(today);
        long   base  = (data != null) ? data[0] : 0;
        if (sessionStartMs > 0) base += System.currentTimeMillis() - sessionStartMs;
        return base;
    }

    public int getTodayDistractionCount() {
        String today = LocalDate.now().format(FMT);
        long[] data  = dailyData.get(today);
        int    base  = (data != null) ? (int) data[1] : 0;
        return base + sessionDistractions;
    }

    /** Index 0 = 6 days ago, index 6 = today. */
    public long[] getWeeklyFocusMs() {
        long[]    result = new long[7];
        LocalDate today  = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            String d    = today.minusDays(6 - i).format(FMT);
            long[] data = dailyData.get(d);
            result[i]   = (data != null) ? data[0] : 0;
        }
        return result;
    }

    public int[] getWeeklyDistractions() {
        int[]     result = new int[7];
        LocalDate today  = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            String d    = today.minusDays(6 - i).format(FMT);
            long[] data = dailyData.get(d);
            result[i]   = (data != null) ? (int) data[1] : 0;
        }
        return result;
    }

    /** Day labels 日一二三四五六, index 0 = oldest day. */
    public String[] getWeeklyLabels() {
        String[]  labels   = new String[7];
        String[]  dayNames = {"日","一","二","三","四","五","六"};
        LocalDate today    = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate d = today.minusDays(6 - i);
            labels[i]   = dayNames[d.getDayOfWeek().getValue() % 7]; // SUNDAY=7%7=0
        }
        return labels;
    }

    private void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(STATS_FILE))) {
            pw.println("date,focusMs,distractionCount");
            for (Map.Entry<String, long[]> e : dailyData.entrySet())
                pw.println(e.getKey() + "," + e.getValue()[0] + "," + e.getValue()[1]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        File f = new File(STATS_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String  line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    try {
                        dailyData.put(parts[0].trim(), new long[]{
                            Long.parseLong(parts[1].trim()),
                            Long.parseLong(parts[2].trim())
                        });
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
