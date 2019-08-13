package org.servantscode.schedule;

import java.util.List;

public class EventPrivatizer {
    private boolean canSeePrivate;
    private int userId;
    private boolean configured = false;

    public void configurePrivatizer(boolean canSeePrivate, int userId) {
        this.canSeePrivate = canSeePrivate;
        this.userId = userId;
        this.configured = true;
    }

    public Event privatize(Event e) {
        if(!configured)
            throw new IllegalStateException("Privatizer called without being configured.");

        if(!canSeePrivate &&
           e.isPrivateEvent() &&
           e.getContactId() != userId &&
           e.getSchedulerId() != userId) {

            e.setTitle("Private Event");
            e.setDescription(null);
            e.setMinistryId(0);
            e.setMinistryName(null);
            e.setContactId(0);
            e.setAttendees(0);
            e.setDepartmentIds(null);
            e.setDepartments(null);
            e.setCategoryIds(null);
            e.setCategories(null);
            privatizeReservations(e.getReservations());
        }
        return e;
    }

    public List<Event> privatizeEvents(List<Event> events) {
        events.forEach(this::privatize);
        return events;
    }

    public Reservation privatize(Reservation r) {
        if(!configured)
            throw new IllegalStateException("Privatizer called without being configured.");

        if(!canSeePrivate &&
           r.isPrivateEvent() &&
           r.getReservingPersonId() != userId &&
           r.getSchedulerId() != userId) {

            r.setEventTitle("Private Event");
        }
        return r;
    }

    public List<Reservation> privatizeReservations(List<Reservation> reservations) {
        reservations.forEach(this::privatize);
        return reservations;
    }

    public EventConflict privatize(EventConflict ec) {
        if(!configured)
            throw new IllegalStateException("Privatizer called without being configured.");

        privatize(ec.getEvent());
        return ec;
    }
}
