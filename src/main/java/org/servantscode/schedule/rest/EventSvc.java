package org.servantscode.schedule.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.schedule.Event;
import org.servantscode.schedule.ReservationManager;
import org.servantscode.schedule.db.EventDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.ZonedDateTime;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.servantscode.commons.DateUtils.parse;

@Path("/event")
public class EventSvc {
    private static final Logger LOG = LogManager.getLogger(EventSvc.class);

    private EventDB db;
    private ReservationManager resMan;

    public EventSvc() {
        db = new EventDB();
        resMan = new ReservationManager();
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public List<Event> getEvents(@QueryParam("start_date") String startDateString,
                                 @QueryParam("end_date") String endDateString,
                                 @QueryParam("partial_description") @DefaultValue("") String search) {

        try {
            ZonedDateTime start = parse(startDateString, firstDayOfMonth());
            ZonedDateTime end = parse(endDateString, lastDayOfMonth());

            LOG.trace(String.format("Retrieving events [%s, %s], search: %s",
                    start.format(ISO_OFFSET_DATE_TIME), end.format(ISO_OFFSET_DATE_TIME), search));

            List<Event> events = db.getEvents(start, end, search);
            for(Event event: events)
                event.setReservations(resMan.getReservationsForEvent(event.getId()));
            return events;
        } catch (Throwable t) {
            LOG.error("Retrieving events failed:", t);
        }
        return null;
    }

    @GET @Path("/ministry/{ministryId}") @Produces(MediaType.APPLICATION_JSON)
    public List<Event> getUpcomingEvents(@PathParam("ministryId") int ministryId,
                                         @QueryParam("count") @DefaultValue("10") int count) {

        try {
            List<Event> events = db.getUpcomingMinistryEvents(ministryId, count);
            for(Event event: events)
                event.setReservations(resMan.getReservationsForEvent(event.getId()));
            return events;
        } catch (Throwable t) {
            LOG.error("Retrieving events failed:", t);
        }
        return null;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Event createEvent(Event event) {
        try {
            LOG.debug("Creating event for: " + event.getStartTime().toString());
            Event resp = db.create(event);
            resMan.createReservationsForEvent(event.getReservations(), resp.getId());
            LOG.info("Created event: " + event.getDescription());
            return resp;
        } catch (Throwable t) {
            LOG.error("Creating event failed:", t);
            throw t;
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Event updateEvent(Event event) {
        try {
            Event resp = db.updateEvent(event);
            resMan.updateRservationsForEvent(event.getReservations(), resp.getId());
            LOG.info("Edited event: " + event.getDescription());
            return resp;
        } catch (Throwable t) {
            LOG.error("Updating event failed:", t);
            throw t;
        }
    }

    @DELETE @Path("/{id}")
    public void deleteEvent(@PathParam("id") int id) {
        if(id <= 0)
            throw new NotFoundException();
        try {
            Event event = db.getEvent(id);
            if(event == null || db.deleteEvent(id))
                throw new NotFoundException();
            resMan.deleteReservationsForEvent(id);
            LOG.info("Deleted event: " + event.getDescription());
        } catch (Throwable t) {
            LOG.error("Deleting event failed:", t);
            throw t;
        }
    }
}