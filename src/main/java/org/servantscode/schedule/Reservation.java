package org.servantscode.schedule;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.ZonedDateTime;

public class Reservation {
    public enum ResourceType { ROOM, EQUIPMENT };

    private int id;
    private ResourceType resourceType;
    private int resourceId;
    private String resourceName;
    private int reservingPersonId;
    private String reserverName;
    private int eventId;
    private String eventTitle;

    @JsonIgnore
    private boolean privateEvent;

    @JsonIgnore
    private int schedulerId;

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    public Reservation() {}

    public Reservation(Reservation r) {
        this.resourceType = r.resourceType;
        this.resourceId = r.resourceId;
        this.resourceName = r.resourceName;
        this.reservingPersonId = r.reservingPersonId;
        this.reserverName = r.reserverName;
        this.eventId = r.eventId;
        this.eventTitle = r.eventTitle;
        this.startTime = r.startTime;
        this.endTime = r.endTime;
        this.privateEvent = r.privateEvent;
        this.schedulerId = r.schedulerId;
    }

    public boolean isSameResource(Reservation other) {
        return this.resourceId == other.resourceId &&
                this.resourceType == other.resourceType;
    }

    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public ResourceType getResourceType() { return resourceType; }
    public void setResourceType(ResourceType resourceType) { this.resourceType = resourceType; }

    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public int getReservingPersonId() { return reservingPersonId; }
    public void setReservingPersonId(int reservingPersonId) { this.reservingPersonId = reservingPersonId; }

    public String getReserverName() { return reserverName; }
    public void setReserverName(String reserverName) { this.reserverName = reserverName; }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public boolean isPrivateEvent() { return privateEvent; }
    public void setPrivateEvent(boolean privateEvent) { this.privateEvent = privateEvent; }

    public int getSchedulerId() { return schedulerId; }
    public void setSchedulerId(int schedulerId) { this.schedulerId = schedulerId; }

    public ZonedDateTime getStartTime() { return startTime; }
    public void setStartTime(ZonedDateTime startTime) { this.startTime = startTime; }

    public ZonedDateTime getEndTime() { return endTime; }
    public void setEndTime(ZonedDateTime endTime) { this.endTime = endTime; }
}
