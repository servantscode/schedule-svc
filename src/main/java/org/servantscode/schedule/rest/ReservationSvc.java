package org.servantscode.schedule.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.schedule.*;
import org.servantscode.schedule.db.ReservationDB;

import javax.ws.rs.*;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.servantscode.commons.DateUtils.parse;
import static org.servantscode.schedule.Recurrence.RecurrenceCycle.CUSTOM;

@Path("/reservation")
public class ReservationSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(ReservationSvc.class);

    ReservationDB db;

    public ReservationSvc() {
        db = new ReservationDB();
    }

    @GET
    @Produces(APPLICATION_JSON)
    public List<Reservation> getReservations(@QueryParam("startTime") String startDateString,
                                             @QueryParam("endTime") String endDateString,
                                             @QueryParam("eventId") int eventId,
                                             @QueryParam("reservingPerson") int reservingPersonId,
                                             @QueryParam("resourceType") Reservation.ResourceType resourceType,
                                             @QueryParam("resourceId") int resourceId) {

        verifyUserAccess("reservation.list");
        if (resourceId != 0 && resourceType == null)
            throw new BadRequestException();

        ZonedDateTime start = parse(startDateString);
        ZonedDateTime end = parse(endDateString);
        if ((start == null) != (end == null))
            throw new BadRequestException();

        if (resourceType == null && eventId == 0 && reservingPersonId == 0 && start == null)
            throw new BadRequestException();

        try {
            LOG.trace("Retrieving reservations");
            return db.getReservations(start, end, eventId, reservingPersonId, resourceType, resourceId);
        } catch (Throwable t) {
            throw new RuntimeException("Retrieving reservations failed:", t);
        }
    }

    @POST @Path("/recurring") @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public List<EventConflict> calculateConflicts(Event e) {
        verifyUserAccess("reservation.list");

        if(e == null || e.getRecurrence() == null)
            throw new BadRequestException();

        if(e.getStartTime() == null || e.getEndTime() == null || e.getEndTime().isBefore(e.getStartTime()))
            throw new BadRequestException();

        if(e.getRecurrence().getCycle() == null ||
            (e.getRecurrence().getEndDate() == null && e.getRecurrence().getCycle() != CUSTOM))
            throw new BadRequestException();

        if(e.getReservations().isEmpty())
            return Collections.emptyList();

        try {
            List<Event> events = new RecurrenceManager().generateEventSeries(e);
            return events.stream().map(event -> findConflicts(event, e.getRecurrence())).filter(Objects::nonNull).collect(Collectors.toList());
        } catch( Throwable t) {
            LOG.error("Conflict check failed.", t);
            throw t;
        }
    }

    @POST @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
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

    @PUT @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
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
            if(reservation == null || !db.delete(id))
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

    private EventConflict findConflicts(Event event, Recurrence r) {
        List<Reservation> conflicts = new LinkedList<>();
        event.getReservations().forEach(res -> conflicts.addAll(db.getConflicts(res, r.getId())));
        return conflicts.isEmpty()? null: new EventConflict(event, conflicts);
    }
}
