package model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReminderItem {
    public enum Priority { NORMAL, IMPORTANT, URGENT }
    public enum Repeat   { ONCE, DAILY, WEEKDAYS }

    private final String id;
    private final String label;
    private final LocalDateTime triggerTime;
    private final Priority priority;
    private final Repeat repeat;
    private boolean active;

    public ReminderItem(String label, LocalDateTime triggerTime, Priority priority, Repeat repeat) {
        this.id          = UUID.randomUUID().toString().substring(0, 8);
        this.label       = label;
        this.triggerTime = triggerTime;
        this.priority    = priority;
        this.repeat      = repeat;
        this.active      = true;
    }

    public String        getId()          { return id; }
    public String        getLabel()       { return label; }
    public LocalDateTime getTriggerTime() { return triggerTime; }
    public Priority      getPriority()    { return priority; }
    public Repeat        getRepeat()      { return repeat; }
    public boolean       isActive()       { return active; }
    public void          setActive(boolean v) { active = v; }
}
