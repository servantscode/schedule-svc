package org.servantscode.schedule;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.schedule.db.EventDB;
import org.servantscode.schedule.db.RecurrenceDB;
import org.servantscode.schedule.db.ReservationDB;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Collections.singletonList;

public class RecurrenceManager {
    private static final Logger LOG = LogManager.getLogger(RecurrenceManager.class);

    private EventDB db;
    private ReservationDB resDb;
    private RecurrenceDB recurDb;
    private EventManager eventMan;

    public RecurrenceManager() {
        db = new EventDB();
        resDb = new ReservationDB();
        recurDb = new RecurrenceDB();
        eventMan = new EventManager();
    }

    public RecurrenceManager(EventManager eventMan) {
        this.eventMan = eventMan;
    }

    public Event createRecurringEvent(Event event) {
        Recurrence r = event.getRecurrence();
        if(r.getCycle() == Recurrence.RecurrenceCycle.WEEKLY && r.getWeeklyDays().isEmpty())
            throw new IllegalArgumentException();

        if(r.getEndDate() == null)
            throw new IllegalArgumentException();

        //Store Recurrence and sanitize object structure
        r = recurDb.create(r);
        event.setRecurringMeetingId(r.getId());
        event.setRecurrence(r);

        List<Event> futureEvents = generateEventSeries(event);

        LinkedList<Event> createdEvents = new LinkedList<>();
        LinkedList<Event> failedEvents = new LinkedList<>();

        for(Event newEvent: futureEvents) {
            try {
                createdEvents.add(eventMan.createEvent(newEvent));
            } catch (Exception e) {
                LOG.error("Could not create event for: " + newEvent.getStartTime().format(ISO_OFFSET_DATE_TIME), e);
                failedEvents.add(newEvent);
            }
        }

        LOG.info(String.format("Created recurring reservation %d. (created:%d, failed:%d)",
                event.getRecurringMeetingId(),  createdEvents.size(), failedEvents.size()));
        return createdEvents.isEmpty()? null: createdEvents.get(0);
    }

    public Event updateRecurringEvent(Event event) {
        Recurrence r = event.getRecurrence();
        if(r.getCycle() == Recurrence.RecurrenceCycle.WEEKLY && r.getWeeklyDays().isEmpty())
            throw new IllegalArgumentException();

        if(r.getEndDate() == null)
            throw new IllegalArgumentException();

        //Store Recurrence and sanitize object structure
        if(r.getId() == 0) {
            r = recurDb.create(r);
            event.setRecurrence(r);
        } else {
            recurDb.update(r);
        }
        event.setRecurringMeetingId(r.getId());

        Event existingEvent = db.getEvent(event.getId());
        List<Event> existingEvents;
        if(existingEvent.getRecurringMeetingId() > 0) {
            existingEvents = db.getUpcomingRecurringEvents(existingEvent.getRecurringMeetingId(), existingEvent.getStartTime());
        } else {
            existingEvents = singletonList(existingEvent);
        }

        List<Event> futureEvents = generateEventSeries(event);

        LinkedList<Event> updatedEvents = new LinkedList<>();
        LinkedList<Event> createdEvents = new LinkedList<>();
        LinkedList<Event> failedEvents = new LinkedList<>(); //Not sure what to do with these yet...

        Iterator<Event> existingIter = existingEvents.iterator();
        for(Event newEvent: futureEvents) {
            try {
                if(existingIter.hasNext()) {
                    //Reclaim existing event/reservations if possible
                    Event existing = existingIter.next();
                    newEvent.setId(existing.getId());

                    List<Reservation> existingReserations = resDb.getReservationsForEvent(existing.getId());
                    for(Reservation reservation: newEvent.getReservations()) {
                        Optional<Reservation> existingRes = existingReserations.stream().filter(reservation::isSameResource).findFirst();
                        if(existingRes.isPresent())
                            reservation.setId(existingRes.get().getId());
                    }

                    updatedEvents.add(eventMan.updateEvent(newEvent));
                } else {
                    createdEvents.add(eventMan.createEvent(newEvent));
                }
            } catch (Exception e) {
                LOG.error("Could not create event for: " + newEvent.getStartTime().format(ISO_OFFSET_DATE_TIME), e);
                failedEvents.add(newEvent);
            }
        }

        int deleted = 0;
        while(existingIter.hasNext()) {
            eventMan.deleteEvent(existingIter.next());
            deleted++;
        }

        LOG.info(String.format("Updated recurring reservation %d. (updated:%d, created:%d, deleted:%d, failed:%d)",
                event.getRecurringMeetingId(), updatedEvents.size(), createdEvents.size(), deleted, failedEvents.size()));

        return !updatedEvents.isEmpty()? updatedEvents.get(0): !createdEvents.isEmpty()? createdEvents.get(0): null;
    }

    public boolean deleteRecurringEvent(Event event) {
        Recurrence r = recurDb.getRecurrence(event.getRecurringMeetingId());

        List<Event> futureEvents = db.getUpcomingRecurringEvents(event.getRecurringMeetingId(), event.getStartTime());
        int deleted = 0;
        for(Event e: futureEvents) {
            if(eventMan.deleteEvent(e))
                deleted++;
        }

        LOG.info(String.format("Deleting recurring reservation %d. (deleted:%d)", event.getRecurringMeetingId(), deleted));
        return recurDb.trimEndDate(r);
    }

    public Recurrence getRecurrence(int recurrenceId) {
        return recurDb.getRecurrence(recurrenceId);
    }

    public void populateRecurrences(List<Event> events, List<Recurrence> recurrences) {
        recurrences.forEach(r -> {
            events.stream().filter(e -> r.getId() == e.getRecurringMeetingId()).forEach(e -> {
                e.setRecurrence(r);
            });
        });
    }

    public List<ZonedDateTime> getFutureTimes(Recurrence r, ZonedDateTime startTime) {
        RecurrenceIterator iter = new RecurrenceIterator(r, startTime);
        List<ZonedDateTime> futures = new LinkedList<>();
        iter.forEachRemaining(futures::add);
        return futures;
    }

    // ----- Private -----
    private List<Event> generateEventSeries(Event e) {
        LinkedList<Event> eventSeries = new LinkedList<>();
        RecurrenceIterator iter = new RecurrenceIterator(e.getRecurrence(), e.getStartTime());
        while(iter.hasNext())
            eventSeries.add(cloneToDate(e, iter.next()));

        return eventSeries;
    }

    private Event cloneToDate(Event e, ZonedDateTime date) {
        Event newEvent = new Event(e);
        Duration period = Duration.between(e.getStartTime(), date);

        newEvent.setStartTime(newEvent.getStartTime().plus(period));
        newEvent.setEndTime(e.getEndTime().plus(period));
        for(Reservation r: newEvent.getReservations()) {
            r.setStartTime(r.getStartTime().plus(period));
            r.setEndTime(r.getEndTime().plus(period));
        }

        return newEvent;
    }
}
