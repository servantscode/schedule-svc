package org.servantscode.schedule;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

public class Event {
    private int id;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private String description;
    private int schedulerId;
    private String ministryName;
    private int ministryId;

    @JsonIgnore
    private int recurringMeetingId;

    private Recurrence recurrence;
    private List<Reservation> reservations;

    public Event() {}

    public Event(Event e) {
        this.startTime = e.startTime;
        this.endTime = e.endTime;
        this.description = e.description;
        this.schedulerId = e.schedulerId;
        this.ministryName = e.ministryName;
        this.ministryId = e.ministryId;
        this.recurringMeetingId = e.recurringMeetingId;
        this.recurrence = new Recurrence(e.recurrence);
        this.reservations = new LinkedList<>();
        for(Reservation r: e.reservations)
            this.reservations.add(new Reservation(r));
    }

    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public ZonedDateTime getStartTime() { return startTime; }
    public void setStartTime(ZonedDateTime startTime) { this.startTime = startTime; }

    public ZonedDateTime getEndTime() { return endTime; }
    public void setEndTime(ZonedDateTime endTime) { this.endTime = endTime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getSchedulerId() { return schedulerId; }
    public void setSchedulerId(int schedulerId) { this.schedulerId = schedulerId; }

    public String getMinistryName() { return ministryName; }
    public void setMinistryName(String ministryName) { this.ministryName = ministryName; }

    public int getMinistryId() { return ministryId; }
    public void setMinistryId(int ministryId) { this.ministryId = ministryId; }

    public int getRecurringMeetingId() { return recurringMeetingId; }
    public void setRecurringMeetingId(int recurringMeetingId) { this.recurringMeetingId = recurringMeetingId; }

    public Recurrence getRecurrence() { return recurrence; }
    public void setRecurrence(Recurrence recurrence) { this.recurrence = recurrence; }

    public List<Reservation> getReservations() { return reservations; }
    public void setReservations(List<Reservation> reservations) { this.reservations = reservations; }
}
