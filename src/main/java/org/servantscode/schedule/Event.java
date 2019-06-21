package org.servantscode.schedule;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Event {
    private int id;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private String title;
    private String description;
    private boolean privateEvent;
    private int schedulerId;
    private int contactId;
    private String ministryName;
    private int ministryId;
    private List<String> departments;
    private List<String> categories;

    @JsonIgnore
    private int recurringMeetingId;

    private Recurrence recurrence;
    private List<Reservation> reservations;

    public Event() {}
    public Event(Event e) {
        this.startTime = e.startTime;
        this.endTime = e.endTime;
        this.title = e.title;
        this.description = e.description;
        this.privateEvent = e.privateEvent;
        this.schedulerId = e.schedulerId;
        this.contactId = e.contactId;
        this.ministryName = e.ministryName;
        this.ministryId = e.ministryId;
        this.recurringMeetingId = e.recurringMeetingId;
        this.recurrence = new Recurrence(e.recurrence);
        this.reservations = new LinkedList<>();
        if(e.departments != null)
            this.departments = new ArrayList<>(e.departments);
        if(e.categories != null)
            this.categories = new ArrayList<>(e.categories);
        if(e.reservations != null)
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

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isPrivateEvent() { return privateEvent; }
    public void setPrivateEvent(boolean privateEvent) { this.privateEvent = privateEvent; }

    public int getSchedulerId() { return schedulerId; }
    public void setSchedulerId(int schedulerId) { this.schedulerId = schedulerId; }

    public int getContactId() { return contactId; }
    public void setContactId(int contactId) { this.contactId = contactId; }

    public String getMinistryName() { return ministryName; }
    public void setMinistryName(String ministryName) { this.ministryName = ministryName; }

    public int getMinistryId() { return ministryId; }
    public void setMinistryId(int ministryId) { this.ministryId = ministryId; }

    public int getRecurringMeetingId() { return recurringMeetingId; }
    public void setRecurringMeetingId(int recurringMeetingId) { this.recurringMeetingId = recurringMeetingId; }

    public Recurrence getRecurrence() { return recurrence; }
    public void setRecurrence(Recurrence recurrence) { this.recurrence = recurrence; }

    public List<String> getDepartments() { return departments; }
    public void setDepartments(List<String> departments) { this.departments = departments; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public List<Reservation> getReservations() { return reservations; }
    public void setReservations(List<Reservation> reservations) { this.reservations = reservations; }
}
