package org.servantscode.schedule;

import org.servantscode.schedule.db.EquipmentDB;
import org.servantscode.schedule.db.ReservationDB;
import org.servantscode.schedule.db.RoomDB;

import java.util.List;

// Service layer helper to manage reservations for events.
public class ReservationManager {
    private ReservationDB db;
    private EquipmentDB equipDb;
    private RoomDB roomDb;

    public ReservationManager() {
        db = new ReservationDB();
        equipDb = new EquipmentDB();
        roomDb = new RoomDB();
    }

    // TODO: Clean this. I think I'm getting sick and it's affecting my brain.
    public List<Reservation> getReservationsForEvent(int eventId) {
        List<Reservation> reservations = db.getReservationsForEvent(eventId);
        for(Reservation res: reservations) {
            if(res.getResourceType() == Reservation.ResourceType.ROOM) {
                Room room = roomDb.getRoom(res.getResourceId());
                if(room != null)
                    res.setResourceName(room.getName());
            } else if(res.getResourceType() == Reservation.ResourceType.EQUIPMENT) {
                Equipment equip = equipDb.getEquipment(res.getResourceId());
                if(equip != null)
                    res.setResourceName(equip.getName());
            }
        }
        return reservations;
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
