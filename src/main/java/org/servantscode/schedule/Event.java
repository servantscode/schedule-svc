package org.servantscode.schedule;

import java.time.ZonedDateTime;
import java.util.List;

public class Event {
    private int id;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private String description;
    private int schedulerId;
    private String ministryName;
    private int ministryId;

    private List<Reservation> reservations;

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

    public List<Reservation> getReservations() { return reservations; }
    public void setReservations(List<Reservation> reservations) { this.reservations = reservations; }
}
