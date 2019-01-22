package org.servantscode.schedule.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.schedule.Reservation;
import org.servantscode.schedule.db.ReservationDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.*;
import java.util.Date;
import java.util.List;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.servantscode.commons.StringUtils.isEmpty;

@Path("/reservation")
public class ReservationSvc {
    private static final Logger LOG = LogManager.getLogger(ReservationSvc.class);

    ReservationDB db;

    public ReservationSvc() {
        db = new ReservationDB();
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public List<Reservation> getReservations(@QueryParam("startTime") String startDateString,
                                             @QueryParam("endTime") String endDateString,
                                             @QueryParam("eventId")int eventId,
                                             @QueryParam("reservingPerson") int reservingPersonId,
                                             @QueryParam("resourceType") Reservation.ResourceType resourceType,
                                             @QueryParam("resourceId") int resourceId) {

        if(resourceId != 0 && resourceType == null)
            throw new BadRequestException();

        ZonedDateTime start = parseDate(startDateString);
        ZonedDateTime end = parseDate(endDateString);
        if((start == null) != (end == null))
            throw new BadRequestException();

        if(resourceType == null && resourceId == 0 && eventId == 0 && reservingPersonId == 0 && start == null && end == null)
            throw new BadRequestException();

        try {
            LOG.trace(String.format("Retrieving reservations"));
            return db.getReservations(start, end, eventId, reservingPersonId, resourceType, resourceId);
        } catch (Throwable t) {
            LOG.error("Retrieving reservations failed:", t);
        }
        return null;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Reservation createReservation(Reservation reservation) {
        try {
            Reservation resp = db.create(reservation);
            LOG.info("Created " + toString(reservation));
            return resp;
        } catch (Throwable t) {
            LOG.error("Creation failed:" + toString(reservation), t);
            throw t;
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Reservation updateReservation(Reservation reservation) {
        try {
            Reservation resp = db.updateReservation(reservation);
            LOG.info("Edited " + toString(reservation));
            return resp;
        } catch (Throwable t) {
            LOG.error("Update failed: " + toString(reservation), t);
            throw t;
        }
    }

    @DELETE @Path("/{id}")
    public void deleteReservation(@PathParam("id") int id) {
        if(id <= 0)
            throw new NotFoundException();
        try {
            Reservation reservation = db.getReservation(id);
            if(reservation == null || db.deleteReservation(id))
                throw new NotFoundException();
            LOG.info("Deleted: " + toString(reservation));
        } catch (Throwable t) {
            LOG.error("Deleting reservation failed:", t);
            throw t;
        }
    }

    // ----- Private -----
    private String toString(Reservation reservation) {
        return String.format("Reservation(%d) of %s:%d", reservation.getId(), reservation.getResourceType(), reservation.getResourceId()) +
                (reservation.getEventId() > 0? "for event:" + reservation.getEventId(): "") +
                "by reserver:" + reservation.getReservingPersonId();
    }

    private ZonedDateTime parseDate(String input) {
        return !isEmpty(input) ?
                ZonedDateTime.parse(input):
                null;
    }
}
