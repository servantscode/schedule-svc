package org.servantscode.schedule.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.schedule.AvailabilityResponse;
import org.servantscode.schedule.Reservation;
import org.servantscode.schedule.db.ReservationDB;

import javax.ws.rs.*;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.servantscode.commons.DateUtils.parse;
import static org.servantscode.commons.DateUtils.toUTC;

@Path("/reservation")
public class ReservationSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(ReservationSvc.class);

    ReservationDB db;

    public ReservationSvc() {
        db = new ReservationDB();
    }

    @GET @Produces(APPLICATION_JSON)
    public List<Reservation> getReservations(@QueryParam("startTime") String startDateString,
                                             @QueryParam("endTime") String endDateString,
                                             @QueryParam("eventId")int eventId,
                                             @QueryParam("reservingPerson") int reservingPersonId,
                                             @QueryParam("resourceType") Reservation.ResourceType resourceType,
                                             @QueryParam("resourceId") int resourceId) {

        verifyUserAccess("reservation.list");
        if(resourceId != 0 && resourceType == null)
            throw new BadRequestException();

        ZonedDateTime start = parse(startDateString);
        ZonedDateTime end = parse(endDateString);
        if((start == null) != (end == null))
            throw new BadRequestException();

        if(resourceType == null && resourceId == 0 && eventId == 0 && reservingPersonId == 0 && start == null && end == null)
            throw new BadRequestException();

        try {
            LOG.trace("Retrieving reservations");
            return db.getReservations(start, end, eventId, reservingPersonId, resourceType, resourceId);
        } catch (Throwable t) {
            LOG.error("Retrieving reservations failed:", t);
        }
        return null;
    }

    @GET @Path("/availability") @Produces(APPLICATION_JSON)
    public AvailabilityResponse checkAvailabilty(@QueryParam("startTime") String startDateString,
                                                 @QueryParam("endTime") String endDateString,
                                                 @QueryParam("resourceType")Reservation.ResourceType resourceType,
                                                 @QueryParam("resourceId") int resourceId,
                                                 @QueryParam("eventId") int eventId) {

        verifyUserAccess("reservation.list");
        if(resourceId == 0 || resourceType == null)
            throw new BadRequestException();

        ZonedDateTime start = toUTC(parse(startDateString));
        ZonedDateTime end = toUTC(parse(endDateString));
        if(start == null || end == null || start.compareTo(end)>0)
            throw new BadRequestException();

        ZonedDateTime startOfDay = start.toLocalDate().atStartOfDay(start.getZone());
        ZonedDateTime endOfDay = end.toLocalDate().plusDays(1).atStartOfDay(end.getZone());

        try {
            LOG.trace("Retrieving reservations");
            List<Reservation> reservations =
                    db.getReservations(startOfDay, endOfDay, 0,0, resourceType, resourceId);

            reservations.sort(Comparator.comparing(Reservation::getStartTime));

            AvailabilityResponse resp = new AvailabilityResponse(resourceType, resourceId);
            ZonedDateTime endTime = startOfDay;
            for(Reservation res: reservations) {
                if(res.getEventId() == eventId)
                    continue;

                ZonedDateTime resStart = res.getStartTime();
                ZonedDateTime resEnd = res.getEndTime();

                //Handle overlapping reservations
                if(endTime.compareTo(resStart) < 0)
                    resp.setAvailableWindow(endTime, resStart);

                //Check availability
                if(resStart.compareTo(start)<=0 != resEnd.compareTo(start)<=0 ||
                    resStart.compareTo(end)>=0 != resEnd.compareTo(end)>=0)
                    resp.setAvailable(false);

                if(endTime.compareTo(resEnd) < 0)
                    endTime = resEnd;
            }
            if(endTime.compareTo(endOfDay) < 0)
                resp.setAvailableWindow(endTime, endOfDay);

            return resp;
        } catch (Throwable t) {
            LOG.error("Retrieving reservations failed:", t);
        }
        return null;
    }

    @POST
    @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Reservation createReservation(Reservation reservation) {
        verifyUserAccess("reservation.create");
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
    @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Reservation updateReservation(Reservation reservation) {
        verifyUserAccess("reservation.update");
        try {
            Reservation resp = db.update(reservation);
            LOG.info("Edited " + toString(reservation));
            return resp;
        } catch (Throwable t) {
            LOG.error("Update failed: " + toString(reservation), t);
            throw t;
        }
    }

    @DELETE @Path("/{id}")
    public void deleteReservation(@PathParam("id") int id) {
        verifyUserAccess("reservation.delete");
        if(id <= 0)
            throw new NotFoundException();
        try {
            Reservation reservation = db.getReservation(id);
            if(reservation == null || db.delete(id))
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
}
