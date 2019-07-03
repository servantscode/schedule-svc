package org.servantscode.schedule;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.schedule.db.EventDB;

import javax.ws.rs.NotFoundException;
import java.util.List;

public class EventManager {
    private static final Logger LOG = LogManager.getLogger(EventManager.class);

    private EventDB db;
    private ReservationManager resMan;

    public EventManager() {
        db = new EventDB();
        resMan = new ReservationManager();
    }

    public EventManager(EventDB db, ReservationManager resMan) {
        this.db = db;
        this.resMan = resMan;
    }

    public Event createEvent(Event event) {
        Event resp = db.create(event);
        List<Reservation> reservations = event.getReservations();
        if(reservations != null) {
            for (Reservation res : reservations)
                res.setEventId(resp.getId());
        }
        resMan.createReservationsForEvent(reservations, resp.getId());
        LOG.info("Created event: " + event.getDescription());
        return resp;
    }

    public Event updateEvent(Event event) {
        Event resp = db.updateEvent(event);
        resMan.updateRservationsForEvent(event.getReservations(), resp.getId());
        LOG.info("Edited event: " + event.getDescription());
        return resp;
    }

    public boolean deleteEvent(Event event) {
        boolean success = db.deleteEvent(event.getId());
        resMan.deleteReservationsForEvent(event.getId());
        LOG.info("Deleted event: " + event.getDescription());
        return success;
    }
}
