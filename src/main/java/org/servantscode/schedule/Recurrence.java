package org.servantscode.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public class Recurrence {
    public enum RecurrenceCycle {DAILY, WEEKLY, DAY_OF_MONTH, WEEKDAY_OF_MONTH, YEARLY, CUSTOM};

    private int id;
    private RecurrenceCycle cycle;
    private int frequency;
    private LocalDate endDate;
    private List<DayOfWeek> weeklyDays;

    private List<LocalDate> exceptionDates;

    public Recurrence() {}

    public Recurrence(RecurrenceCycle cycle, int frequency, LocalDate endDate) {
        this.cycle = cycle;
        this.frequency = frequency;
        this.endDate = endDate;
    }

    public Recurrence(RecurrenceCycle cycle, int frequency, LocalDate endDate, List<DayOfWeek> weeklyDays) {
        this.cycle = cycle;
        this.frequency = frequency;
        this.endDate = endDate;
        this.weeklyDays = weeklyDays;
    }

    public Recurrence(Recurrence r) {
        this.id = r.id;
        this.cycle = r.cycle;
        this.frequency = r.frequency;
        this.endDate = r.endDate;
        this.weeklyDays = r.weeklyDays;
    }

    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public RecurrenceCycle getCycle() { return cycle; }
    public void setCycle(RecurrenceCycle cycle) { this.cycle = cycle; }

    public int getFrequency() { return frequency; }
    public void setFrequency(int frequency) { this.frequency = frequency; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public List<DayOfWeek> getWeeklyDays() { return weeklyDays; }
    public void setWeeklyDays(List<DayOfWeek> weeklyDays) { this.weeklyDays = weeklyDays; }

    public List<LocalDate> getExceptionDates() { return exceptionDates; }
    public void setExceptionDates(List<LocalDate> exceptionDates) { this.exceptionDates = exceptionDates; }
}
