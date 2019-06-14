package org.servantscode.schedule.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.schedule.Event;
import org.servantscode.schedule.Reservation;
import org.servantscode.schedule.Room;
import org.servantscode.schedule.db.ReservationDB;
import org.servantscode.schedule.db.RoomDB;

import javax.ws.rs.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.servantscode.commons.DateUtils.parse;

@Path("/availability")
public class AvailabilitySvc {
    private static Logger LOG = LogManager.getLogger(AvailabilitySvc.class);

    private RoomDB roomDb;
    private ReservationDB resDb;

    public AvailabilitySvc() {
        this.roomDb = new RoomDB();
        this.resDb = new ReservationDB();
    }

    @GET @Path("/rooms") @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public List<Room> getAvailableRooms(@QueryParam("search") String searchString,
                                        @QueryParam("startTime") String startDateString,
                                        @QueryParam("endTime") String endDateString) {
        try {

            ZonedDateTime start = parse(startDateString);
            ZonedDateTime end = parse(endDateString);
            if (start == null || end == null)
                throw new BadRequestException();

            List<Room> rooms = roomDb.getRooms(searchString, "name", 0, 0);
            List<Reservation> reservations = resDb.getReservations(start, end, 0, 0, Reservation.ResourceType.ROOM, 0);

            return rooms.stream().filter(room -> !reservations.stream().anyMatch(res -> res.getResourceId() == room.getId())).collect(Collectors.toList());
        } catch (Throwable t) {
            LOG.error("Failed to find available rooms", t);
            throw t;
        }
    }
}
