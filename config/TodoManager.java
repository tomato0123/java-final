package config;

import model.TodoItem;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists todo items to todos.csv using | as delimiter to safely handle
 * commas in task titles. Format: id|title|dueDateTime|completed
 */
public class TodoManager {
    private static final String FILE   = "todos.csv";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final List<TodoItem> items = new ArrayList<>();

    public TodoManager() { load(); }

    public void addItem(String title, LocalDateTime dueDateTime) {
        items.add(new TodoItem(title, dueDateTime));
        save();
    }

    public void updateItem(String id, String title, LocalDateTime dueDateTime) {
        for (TodoItem item : items) {
            if (item.getId().equals(id)) {
                item.setTitle(title);
                item.setDueDateTime(dueDateTime);
                break;
            }
        }
        save();
    }

    public void deleteItem(String id) {
        items.removeIf(i -> i.getId().equals(id));
        save();
    }

    public void setCompleted(String id, boolean completed) {
        for (TodoItem item : items) {
            if (item.getId().equals(id)) {
                item.setCompleted(completed);
                break;
            }
        }
        save();
    }

    public List<TodoItem> getAll() { return new ArrayList<>(items); }

    public List<TodoItem> getActive() {
        List<TodoItem> active = new ArrayList<>();
        for (TodoItem item : items)
            if (!item.isCompleted()) active.add(item);
        return active;
    }

    private void load() {
        Path path = Paths.get(FILE);
        if (!Files.exists(path)) return;
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] p = line.split("\\|", 4);
                if (p.length < 4) continue;
                LocalDateTime dt = p[2].isEmpty() ? null : LocalDateTime.parse(p[2], DT_FMT);
                items.add(new TodoItem(p[0], p[1], dt, Boolean.parseBoolean(p[3])));
            }
        } catch (IOException ignored) {}
    }

    private void save() {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(FILE), StandardCharsets.UTF_8)) {
            for (TodoItem item : items) {
                String dt = item.getDueDateTime() == null ? "" : item.getDueDateTime().format(DT_FMT);
                bw.write(item.getId() + "|" + item.getTitle() + "|" + dt + "|" + item.isCompleted());
                bw.newLine();
            }
        } catch (IOException ignored) {}
    }
}
