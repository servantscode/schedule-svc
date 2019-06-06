package org.servantscode.schedule;

import java.util.List;

public class EventConflict {
    private Event event;
    private List<Reservation> conflicts;

    public EventConflict(Event event, List<Reservation> conflicts) {
        this.event = event;
        this.conflicts = conflicts;
    }

    //----- Accessors -----
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public List<Reservation> getConflicts() { return conflicts; }
    public void setConflicts(List<Reservation> conflicts) { this.conflicts = conflicts; }
}
