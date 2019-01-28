package org.servantscode.schedule.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.schedule.*;
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
    private EventManager eventMan;
    private RecurrenceManager recurMan;

    public EventSvc() {
        db = new EventDB();
        resMan = new ReservationManager();
        eventMan = new EventManager();
        recurMan = new RecurrenceManager();
    }

    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Event getEvent(@PathParam("id") int id) {
        try {
            Event event = db.getEvent(id);
            event.setReservations(resMan.getReservationsForEvent(event.getId()));
            event.setRecurrence(recurMan.getRecurrence(event.getRecurringMeetingId()));
            return event;
        } catch (Throwable t) {
            LOG.error("Retrieving events failed:", t);
        }
        throw new NotFoundException();
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
            for(Event event: events) {
                event.setReservations(resMan.getReservationsForEvent(event.getId()));
                event.setRecurrence(recurMan.getRecurrence(event.getRecurringMeetingId()));
            }
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
            for(Event event: events) {
                event.setReservations(resMan.getReservationsForEvent(event.getId()));
                event.setRecurrence(recurMan.getRecurrence(event.getRecurringMeetingId()));
            }
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
            if(event.getRecurrence() != null)
                return recurMan.createRecurringEvent(event);
            return eventMan.createEvent(event);
        } catch (Throwable t) {
            LOG.error("Creating event failed:", t);
            throw t;
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Event updateEvent(Event event) {
        try {
            if(event.getRecurrence() != null)
                return recurMan.updateRecurringEvent(event);
            return eventMan.updateEvent(event);
        } catch (Throwable t) {
            LOG.error("Updating event failed:", t);
            throw t;
        }
    }

    @DELETE @Path("/{id}")
    public void deleteEvent(@PathParam("id") int id,
                            @QueryParam("deleteFutureEvents") boolean deleteFutureEvents) {
        if(id <= 0)
            throw new NotFoundException();
        try {
            Event event = db.getEvent(id);
            if(event == null)
                throw new NotFoundException();

            if(event.getRecurringMeetingId() > 0 && deleteFutureEvents)
                recurMan.deleteRecurringEvent(event);
            else
                eventMan.deleteEvent(event);
        } catch (Throwable t) {
            LOG.error("Deleting event failed:", t);
            throw t;
        }
    }
}