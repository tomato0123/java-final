package config;

import model.CountdownEvent;
import model.ReminderItem;
import ui.PetPanel;
import ui.ToastNotification;

import javax.swing.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ReminderManager {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private final PetPanel pet;

    private final List<ReminderItem>  customReminders  = new ArrayList<>();
    private final List<CountdownEvent> countdownEvents  = new ArrayList<>();
    private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

    // 目前執行中的定時提醒
    private ScheduledFuture<?> periodicTask;

    public ReminderManager(PetPanel pet) {
        this.pet = pet;
    }

    // ─────────────────────────────────────────────
    // 功能 1：定時提醒（循環）
    // ─────────────────────────────────────────────
    public void startPeriodicReminder(int intervalMinutes, String label) {
        stopPeriodicReminder();
        periodicTask = scheduler.scheduleAtFixedRate(
            () -> SwingUtilities.invokeLater(() -> firePeriodicReminder(label, intervalMinutes)),
            intervalMinutes, intervalMinutes, TimeUnit.MINUTES);
    }

    public void stopPeriodicReminder() {
        if (periodicTask != null && !periodicTask.isCancelled()) {
            periodicTask.cancel(false);
            periodicTask = null;
        }
    }

    public boolean isPeriodicActive() {
        return periodicTask != null && !periodicTask.isCancelled();
    }

    private void firePeriodicReminder(String label, int intervalMinutes) {
        pet.setState("happy", label);
        ToastNotification.show(
            "定時提醒", label,
            () -> scheduler.schedule(               // 貪睡：5 分鐘後再觸發一次
                () -> SwingUtilities.invokeLater(() -> firePeriodicReminder(label, intervalMinutes)),
                5, TimeUnit.MINUTES),
            () -> {}
        );
    }

    // ─────────────────────────────────────────────
    // 功能 2：自訂提醒（單次 / 重複）
    // ─────────────────────────────────────────────
    public ReminderItem addCustomReminder(String label, LocalDateTime triggerTime,
                                          ReminderItem.Priority priority,
                                          ReminderItem.Repeat repeat) {
        ReminderItem item = new ReminderItem(label, triggerTime, priority, repeat);
        customReminders.add(item);
        scheduleItem(item);
        return item;
    }

    private void scheduleItem(ReminderItem item) {
        long delayMs = java.time.Duration.between(LocalDateTime.now(), item.getTriggerTime()).toMillis();
        if (delayMs <= 0) return;
        ScheduledFuture<?> f = scheduler.schedule(
            () -> { if (item.isActive()) SwingUtilities.invokeLater(() -> fireCustomReminder(item)); },
            delayMs, TimeUnit.MILLISECONDS);
        futures.put(item.getId(), f);
    }

    private void fireCustomReminder(ReminderItem item) {
        String state = item.getPriority() == ReminderItem.Priority.URGENT ? "angry" : "happy";
        pet.setState(state, priorityEmoji(item.getPriority()) + " " + item.getLabel());

        ToastNotification.show(
            priorityLabel(item.getPriority()) + " 提醒",
            item.getLabel(),
            () -> {   // 貪睡：5 分後
                ReminderItem snoozed = new ReminderItem(item.getLabel(),
                    LocalDateTime.now().plusMinutes(5),
                    item.getPriority(), ReminderItem.Repeat.ONCE);
                customReminders.add(snoozed);
                scheduleItem(snoozed);
            },
            () -> scheduleNextIfRepeat(item)
        );
    }

    private void scheduleNextIfRepeat(ReminderItem item) {
        if (item.getRepeat() == ReminderItem.Repeat.DAILY) {
            ReminderItem next = new ReminderItem(item.getLabel(),
                item.getTriggerTime().plusDays(1), item.getPriority(), item.getRepeat());
            customReminders.add(next);
            scheduleItem(next);
        } else if (item.getRepeat() == ReminderItem.Repeat.WEEKDAYS) {
            LocalDateTime next = item.getTriggerTime().plusDays(1);
            while (next.getDayOfWeek().getValue() >= 6) next = next.plusDays(1);
            ReminderItem nextItem = new ReminderItem(item.getLabel(), next, item.getPriority(), item.getRepeat());
            customReminders.add(nextItem);
            scheduleItem(nextItem);
        }
    }

    public void removeCustomReminder(String id) {
        customReminders.removeIf(r -> r.getId().equals(id));
        ScheduledFuture<?> f = futures.remove(id);
        if (f != null) f.cancel(false);
    }

    public List<ReminderItem> getCustomReminders() {
        return new ArrayList<>(customReminders);
    }

    // ─────────────────────────────────────────────
    // 功能 3：倒數事件
    // ─────────────────────────────────────────────
    public CountdownEvent addCountdownEvent(String name, LocalDate targetDate) {
        CountdownEvent e = new CountdownEvent(name, targetDate);
        countdownEvents.add(e);
        return e;
    }

    public void removeCountdownEvent(String id) {
        countdownEvents.removeIf(e -> e.getId().equals(id));
    }

    public void archiveEvent(String id) {
        countdownEvents.stream().filter(e -> e.getId().equals(id))
            .findFirst().ifPresent(e -> e.setArchived(true));
    }

    public List<CountdownEvent> getCountdownEvents() {
        return new ArrayList<>(countdownEvents);
    }

    // ─────────────────────────────────────────────
    // 工具
    // ─────────────────────────────────────────────
    private String priorityLabel(ReminderItem.Priority p) {
        switch (p) {
            case URGENT:    return "🔴 極重要";
            case IMPORTANT: return "🟡 重要";
            default:        return "🔵 一般";
        }
    }

    private String priorityEmoji(ReminderItem.Priority p) {
        switch (p) {
            case URGENT:    return "🔴";
            case IMPORTANT: return "🟡";
            default:        return "🔵";
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
