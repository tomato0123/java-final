package monitor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 用持久的 PowerShell 子進程呼叫 user32.dll，
 * 每 700ms 輸出一次前景視窗標題。
 */
public class WindowMonitor {

    public interface WindowTitleListener {
        void onWindowTitle(String title);
    }

    private final List<WindowTitleListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean running = false;
    private Process psProcess;
    private Path    tempScript;

    public void addListener(WindowTitleListener l) { listeners.add(l); }

    public void start() {
        if (running) return;
        running = true;
        Thread t = new Thread(this::pollLoop, "WindowMonitor");
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        running = false;
        if (psProcess != null) psProcess.destroyForcibly();
        try {
            if (tempScript != null) Files.deleteIfExists(tempScript);
        } catch (IOException ignored) {}
    }

    private void pollLoop() {
        try {
            tempScript = Files.createTempFile("winmon_", ".ps1");

            // PowerShell here-string: closing "@ must be at column 0
            String ps1 =
                "Add-Type @\"\r\n" +
                "using System;\r\n" +
                "using System.Text;\r\n" +
                "using System.Runtime.InteropServices;\r\n" +
                "public class Win32 {\r\n" +
                "    [DllImport(\"user32.dll\")]\r\n" +
                "    public static extern IntPtr GetForegroundWindow();\r\n" +
                "    [DllImport(\"user32.dll\", CharSet=CharSet.Unicode)]\r\n" +
                "    public static extern int GetWindowText(IntPtr h, StringBuilder sb, int n);\r\n" +
                "}\r\n" +
                "\"@\r\n" +
                "while ($true) {\r\n" +
                "    $h  = [Win32]::GetForegroundWindow()\r\n" +
                "    $sb = New-Object System.Text.StringBuilder 512\r\n" +
                "    [Win32]::GetWindowText($h, $sb, 512) | Out-Null\r\n" +
                "    Write-Output $sb.ToString()\r\n" +
                "    [Console]::Out.Flush()\r\n" +
                "    Start-Sleep -Milliseconds 700\r\n" +
                "}\r\n";

            Files.write(tempScript, ps1.getBytes(StandardCharsets.UTF_8));

            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-NonInteractive",
                "-ExecutionPolicy", "Bypass",
                "-File", tempScript.toString());
            pb.redirectErrorStream(true);
            psProcess = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(psProcess.getInputStream(), StandardCharsets.UTF_8));

            String line;
            while (running && (line = reader.readLine()) != null) {
                String title = line.trim();
                if (!title.isEmpty()) {
                    for (WindowTitleListener l : listeners) l.onWindowTitle(title);
                }
            }
        } catch (IOException e) {
            if (running) System.err.println("WindowMonitor 錯誤: " + e.getMessage());
        }
    }
}
