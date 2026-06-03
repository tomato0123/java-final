package model;

import java.time.LocalDateTime;
import java.util.UUID;

public class TodoItem {
    private final String id;
    private String title;
    private LocalDateTime dueDateTime;  // nullable
    private boolean completed;

    public TodoItem(String title, LocalDateTime dueDateTime) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.title = title;
        this.dueDateTime = dueDateTime;
        this.completed = false;
    }

    /** For loading from persistent storage */
    public TodoItem(String id, String title, LocalDateTime dueDateTime, boolean completed) {
        this.id = id;
        this.title = title;
        this.dueDateTime = dueDateTime;
        this.completed = completed;
    }

    public String        getId()          { return id; }
    public String        getTitle()       { return title; }
    public void          setTitle(String v) { this.title = v; }
    public LocalDateTime getDueDateTime() { return dueDateTime; }
    public void          setDueDateTime(LocalDateTime v) { this.dueDateTime = v; }
    public boolean       isCompleted()    { return completed; }
    public void          setCompleted(boolean v) { this.completed = v; }
}
