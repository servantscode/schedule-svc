package org.servantscode.schedule;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

public class AvailabilityResponse {
    private List<AvailablityWindow> availability;
    private Reservation.ResourceType resourceType;
    private int resourceId;
    private boolean isAvailable;

    public AvailabilityResponse(Reservation.ResourceType type, int id) {
        this.availability = new LinkedList<>();
        this.resourceType = type;
        this.resourceId = id;
        this.isAvailable = true;
    }

    public void setAvailableWindow(ZonedDateTime start, ZonedDateTime end) {
        availability.add(new AvailablityWindow(start, end));
    }

    // ----- Accessors -----
    public List<AvailablityWindow> getAvailability() { return availability; }
    public void setAvailability(List<AvailablityWindow> availability) { this.availability = availability; }

    public Reservation.ResourceType getResourceType() { return resourceType; }
    public void setResourceType(Reservation.ResourceType resourceType) { this.resourceType = resourceType; }

    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    // ----- Private -----
    public static class AvailablityWindow {
        private ZonedDateTime startTime;
        private ZonedDateTime endTime;

        public AvailablityWindow(ZonedDateTime start, ZonedDateTime end) {
            this.startTime = start;
            this.endTime = end;
        }

        // ----- Accessors -----
        public ZonedDateTime getStartTime() { return startTime; }
        public void setStartTime(ZonedDateTime startTime) { this.startTime = startTime; }

        public ZonedDateTime getEndTime() { return endTime; }
        public void setEndTime(ZonedDateTime endTime) { this.endTime = endTime; }
    }
}
