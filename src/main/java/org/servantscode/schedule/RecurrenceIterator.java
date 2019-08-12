package org.servantscode.schedule;

import org.servantscode.commons.DateUtils;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static org.servantscode.schedule.Recurrence.RecurrenceCycle.CUSTOM;
import static org.servantscode.schedule.Recurrence.RecurrenceCycle.WEEKLY;

public class RecurrenceIterator implements Iterator<ZonedDateTime> {

    private Recurrence r;
    private ZonedDateTime next;
    private ZonedDateTime end;
    private List<DayOfWeek> days;
    private Iterator<DayOfWeek> dayIter;



    public RecurrenceIterator(Recurrence r, ZonedDateTime startDate) {
        if(r.getCycle() == CUSTOM)
            throw new IllegalArgumentException("Cannot recur a custom recurrence. Please create/update by event list.");

        this.r = r;
        next = normalizeTimeZone(startDate);

        //Inclusive of last day
        end = r.getEndDate().plusDays(1).atStartOfDay(DateUtils.getTimeZone());

        // It's possible someone requested a start date that is not of of the recurring week days.
        // If so, skip to the next one.
        // NOTE: This may make for an empty iterator.
        if(r.getCycle() == WEEKLY) {

            days = new ArrayList<>(r.getWeeklyDays());
            days.sort(Comparator.comparingInt(DayOfWeek::getValue));
            resetDayIter();

            for(DayOfWeek day: days) {
                if(day.getValue() < next.getDayOfWeek().getValue())
                    dayIter.next();
            }

            //Reset iter if it's drained. Could happen if start date is AFTER last day of week in cycle.
            if(!dayIter.hasNext())
                resetDayIter();

            next = next.minusDays(1);
            calculateNext();
        }
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public ZonedDateTime next() {
        ZonedDateTime resp = next;
        calculateNext();
        return resp;
    }

    // ----- Private -----
    private void calculateNext() {
        DayOfWeek dayOfWeek = null;
        switch (r.getCycle()) {
            case DAILY:
                next = next.plusDays(r.getFrequency());
                break;
            case WEEKLY:
                if(!dayIter.hasNext()) {
                    next = next.plusWeeks(r.getFrequency() - 1);
                    resetDayIter();
                }
                next = next.with(TemporalAdjusters.next(dayIter.next()));
                break;
            case DAY_OF_MONTH:
                next = next.plusMonths(r.getFrequency());
                break;
            case WEEKDAY_OF_MONTH:
                dayOfWeek = next.getDayOfWeek();
                int weekInMonth = ((next.getDayOfMonth()-1)/7) + 1;
                next = next.plusMonths(r.getFrequency()).with(TemporalAdjusters.dayOfWeekInMonth(weekInMonth, dayOfWeek));
                break;
            case YEARLY:
                next = next.plusYears(r.getFrequency());
                break;
        };

        if(next.compareTo(end) >= 0)
            next = null;
        else if(r.getExceptionDates() != null && r.getExceptionDates().contains(next.toLocalDate()))
            calculateNext();
    }

    private void resetDayIter() {
        dayIter = days.iterator();
    }

    public static ZonedDateTime normalizeTimeZone(ZonedDateTime input) {
        return input.withZoneSameInstant(DateUtils.getTimeZone());
    }
}
