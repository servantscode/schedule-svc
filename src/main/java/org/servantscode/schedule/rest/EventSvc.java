package org.servantscode.schedule.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.schedule.Event;
import org.servantscode.schedule.db.EventDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.servantscode.commons.StringUtils.isEmpty;

@Path("/event")
public class EventSvc {
    private static final Logger LOG = LogManager.getLogger(EventSvc.class);

    @GET @Produces(MediaType.APPLICATION_JSON)
    public List<Event> getEvents(@QueryParam("start_date") String startDateString,
                                 @QueryParam("end_date") String endDateString,
                                 @QueryParam("partial_description") @DefaultValue("") String search) {

        try {
            ZonedDateTime start = !isEmpty(startDateString) ?
                    ZonedDateTime.parse(startDateString):
                    ZonedDateTime.ofInstant(Instant.now().with(firstDayOfMonth()), ZoneId.systemDefault());
            ZonedDateTime end = !isEmpty(endDateString) ?
                    ZonedDateTime.parse(endDateString):
                    ZonedDateTime.ofInstant(Instant.now().with(lastDayOfMonth()), ZoneId.systemDefault());

            LOG.trace(String.format("Retrieving events [%s, %s], search: %s",
                    start.format(ISO_OFFSET_DATE_TIME), end.format(ISO_OFFSET_DATE_TIME), search));
            return new EventDB().getEvents(start, end, search);
        } catch (Throwable t) {
            LOG.error("Retrieving events failed:", t);
        }
        return null;
    }

    @GET @Path("/ministry/{ministryId}") @Produces(MediaType.APPLICATION_JSON)
    public List<Event> getUpcomingEvents(@PathParam("ministryId") int ministryId,
                                         @QueryParam("count") @DefaultValue("10") int count) {

        try {
            return new EventDB().getUpcomingMinistryEvents(ministryId, count);
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
            Event resp = new EventDB().create(event);
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
            Event resp = new EventDB().updateEvent(event);
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
            EventDB db = new EventDB();
            Event event = db.getEvent(id);
            if(event == null || db.deleteEvent(id))
                throw new NotFoundException();
            LOG.info("Deleted event: " + event.getDescription());
        } catch (Throwable t) {
            LOG.error("Deleting event failed:", t);
            throw t;
        }
    }
}
