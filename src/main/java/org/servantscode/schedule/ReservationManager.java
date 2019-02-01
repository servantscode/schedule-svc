package org.servantscode.schedule;

import org.servantscode.schedule.db.ReservationDB;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// Service layer helper to manage reservations for events.
public class ReservationManager {
    private ReservationDB db;

    public ReservationManager() {
        db = new ReservationDB();
    }

    public List<Reservation> getReservationsForEvent(int eventId) {
        return db.getReservationsForEvent(eventId);
    }

    public void populateRservations(List<Event> events, List<Reservation> reservations) {
        Map<Integer, List<Reservation>> resMap = new HashMap<>(events.size());
        reservations.forEach((res) -> {
            resMap.computeIfAbsent(res.getEventId(), k -> new LinkedList<>());
            resMap.get(res.getEventId()).add(res);
        });

        events.forEach(e -> e.setReservations(resMap.get(e.getId())));
    }

    public void createReservationsForEvent(List<Reservation> reservations, int eventId) {
        //TODO: Look for conflicts, reject/override by permissions
        for(Reservation r: reservations) {
            r.setId(eventId); //Just to be sure
            db.create(r);
        }
    }

    public void updateRservationsForEvent(List<Reservation> reservations, int eventId) {
        //TODO: Look for conflicts, reject/override by permissions
        //TODO: Consider better matching on poorly id'd input
        List<Reservation> existing = db.getReservationsForEvent(eventId);
        for(Reservation r: reservations) {
            if(r.getId() > 0) {
                r.setEventId(eventId); //Just to be sure
                db.update(r);
                existing.removeIf((res) -> res.getId() == r.getId());
            } else {
                r.setEventId(eventId); //Just to be sure
                db.create(r);
            }
        }

        // Delete removed reservations
        for(Reservation r: existing)
            db.delete(r.getId());
    }

    public void deleteReservationsForEvent(int eventId) {
        db.deleteReservationsByEvent(eventId);
    }
}
