package com.example.lunara;

/**
 * Model for a custom user-created reminder.
 * Stored as a JSON array in SharedPreferences.
 */
public class ReminderModel {
    public int id;           // unique alarm ID
    public String title;
    public long triggerTime; // epoch millis
    public boolean isSilent; // true = notification only, false = alarm + sound
    public boolean enabled;  // whether the alarm is currently active

    public ReminderModel() {}

    public ReminderModel(int id, String title, long triggerTime, boolean isSilent, boolean enabled) {
        this.id = id;
        this.title = title;
        this.triggerTime = triggerTime;
        this.isSilent = isSilent;
        this.enabled = enabled;
    }
}
