package org.servantscode.schedule;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Event {
    public enum SacramentType {MASS, BAPTISM, RECONCILIATION};

    private int id;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private String title;
    private String description;
    private boolean privateEvent;
    private int schedulerId;
    private int contactId;
    private String contactName;
    private int ministryId;
    private String ministryName;
    private int attendees;
    private List<String> departments;
    private List<Integer> departmentIds;
    private List<String> categories;
    private List<Integer> categoryIds;
    private SacramentType sacramentType;

    @JsonIgnore
    private ZonedDateTime createdTime;
    @JsonIgnore
    private ZonedDateTime modifiedTime;
    @JsonIgnore
    private int sequenceNumber;
    @JsonIgnore
    private String contactEmail;

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
        this.contactName = e.contactName;
        this.contactEmail = e.contactEmail;
        this.ministryName = e.ministryName;
        this.ministryId = e.ministryId;
        this.attendees = e.attendees;
        this.recurringMeetingId = e.recurringMeetingId;
        this.recurrence = new Recurrence(e.recurrence);
        this.reservations = new LinkedList<>();
        if(e.departments != null)
            this.departments = new ArrayList<>(e.departments);
        if(e.departmentIds != null)
            this.departmentIds = new ArrayList<>(e.departmentIds);
        if(e.categories != null)
            this.categories = new ArrayList<>(e.categories);
        if(e.categoryIds != null)
            this.categoryIds = new ArrayList<>(e.categoryIds);
        if(e.reservations != null)
            for(Reservation r: e.reservations)
                this.reservations.add(new Reservation(r));

        this.createdTime = e.createdTime;
        this.modifiedTime = e.modifiedTime;
        this.sacramentType = e.sacramentType;
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

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getMinistryName() { return ministryName; }
    public void setMinistryName(String ministryName) { this.ministryName = ministryName; }

    public int getMinistryId() { return ministryId; }
    public void setMinistryId(int ministryId) { this.ministryId = ministryId; }

    public int getAttendees() { return attendees; }
    public void setAttendees(int attendees) { this.attendees = attendees; }

    public int getRecurringMeetingId() { return recurringMeetingId; }
    public void setRecurringMeetingId(int recurringMeetingId) { this.recurringMeetingId = recurringMeetingId; }

    public Recurrence getRecurrence() { return recurrence; }
    public void setRecurrence(Recurrence recurrence) { this.recurrence = recurrence; }

    public List<String> getDepartments() { return departments; }
    public void setDepartments(List<String> departments) { this.departments = departments; }

    public List<Integer> getDepartmentIds() { return departmentIds; }
    public void setDepartmentIds(List<Integer> departmentIds) { this.departmentIds = departmentIds; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public List<Integer> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Integer> categoryIds) { this.categoryIds = categoryIds; }

    public List<Reservation> getReservations() { return reservations; }
    public void setReservations(List<Reservation> reservations) { this.reservations = reservations; }

    public ZonedDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(ZonedDateTime createdTime) { this.createdTime = createdTime; }

    public ZonedDateTime getModifiedTime() { return modifiedTime; }
    public void setModifiedTime(ZonedDateTime modifiedTime) { this.modifiedTime = modifiedTime; }

    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public void incrementSequenceNumber() { this.sequenceNumber++; }

    public SacramentType getSacramentType() { return sacramentType; }
    public void setSacramentType(SacramentType sacramentType) { this.sacramentType = sacramentType; }
}
