package org.servantscode.schedule;

import java.time.ZonedDateTime;

public class Reservation {
    public enum ResourceType { ROOM, EQUIPMENT };

    private int id;
    private ResourceType resourceType;
    private int resourceId;
    private String resourceName;
    private int reservingPersonId;
    private int eventId;

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

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

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public ZonedDateTime getStartTime() { return startTime; }
    public void setStartTime(ZonedDateTime startTime) { this.startTime = startTime; }

    public ZonedDateTime getEndTime() { return endTime; }
    public void setEndTime(ZonedDateTime endTime) { this.endTime = endTime; }
}
