package model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class CountdownEvent {
    private final String    id;
    private final String    name;
    private final LocalDate targetDate;
    private int    progressCurrent;
    private int    progressTotal;
    private boolean archived;

    public CountdownEvent(String name, LocalDate targetDate) {
        this.id          = UUID.randomUUID().toString().substring(0, 8);
        this.name        = name;
        this.targetDate  = targetDate;
        this.archived    = false;
    }

    /** 距離目標日的剩餘天數（負數代表已過期） */
    public long getDaysRemaining() {
        return ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
    }

    public boolean isPast() {
        return LocalDate.now().isAfter(targetDate);
    }

    public void setProgress(int current, int total) {
        this.progressCurrent = current;
        this.progressTotal   = total;
    }

    public String    getId()              { return id; }
    public String    getName()            { return name; }
    public LocalDate getTargetDate()      { return targetDate; }
    public int       getProgressCurrent() { return progressCurrent; }
    public int       getProgressTotal()   { return progressTotal; }
    public boolean   isArchived()         { return archived; }
    public void      setArchived(boolean v) { archived = v; }
}
