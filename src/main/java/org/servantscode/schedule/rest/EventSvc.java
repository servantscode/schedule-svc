package org.servantscode.schedule.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.schedule.*;
import org.servantscode.schedule.db.EventDB;
import org.servantscode.schedule.db.RecurrenceDB;
import org.servantscode.schedule.db.ReservationDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.ZonedDateTime;
import java.util.List;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static org.servantscode.commons.DateUtils.parse;
import static org.servantscode.commons.StringUtils.isSet;

@Path("/event")
public class EventSvc extends SCServiceBase {
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
        verifyUserAccess("event.read");
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
    public PaginatedResponse<Event> getEvents(@QueryParam("start") @DefaultValue("0") int start,
                                              @QueryParam("count") @DefaultValue("32768") int count,
                                              @QueryParam("sort_field") @DefaultValue("start_time") String sortField,
                                              @QueryParam("search") @DefaultValue("") String search,
                                              @QueryParam("start_date") String startDateString,
                                              @QueryParam("end_date") String endDateString) {

        verifyUserAccess("event.list");

        try {
            String finalSearch = "";
            ZonedDateTime startDate = parse(startDateString, firstDayOfMonth());
            if(isSet(startDateString)) {
                finalSearch += String.format(" endTime:[%s TO *]", startDateString);
            }

            ZonedDateTime endDate = parse(endDateString, lastDayOfMonth());
            if(isSet(endDateString)) {
                finalSearch += String.format(" startTime:[* TO %s]", endDateString);
            }

            finalSearch += " " + search;

            LOG.trace(String.format("Retrieving events (%s, %s, page: %d; %d)", finalSearch, sortField, start, count));
            int totalPeople = db.getCount(finalSearch);

            List<Event> events ;
            events = db.getEvents(finalSearch, sortField, start, count);
            List<Reservation> reservations = new ReservationDB().getEventReservations(finalSearch);
            List<Recurrence> recurrences = new RecurrenceDB().getEventRecurrences(finalSearch);

            resMan.populateRservations(events, reservations);
            recurMan.populateRecurrences(events, recurrences);

            return new PaginatedResponse<>(start, events.size(), totalPeople, events);
        } catch (Throwable t) {
            LOG.error("Retrieving events failed:", t);
            throw t;
        }
    }

//    @GET @Produces(MediaType.APPLICATION_JSON)
//    public List<Event> getEvents(@QueryParam("start_date") String startDateString,
//                                 @QueryParam("end_date") String endDateString,
//                                 @QueryParam("partial_description") @DefaultValue("") String search) {
//
//        verifyUserAccess("event.list");
//        try {
//            ZonedDateTime start = parse(startDateString, firstDayOfMonth());
//            ZonedDateTime end = parse(endDateString, lastDayOfMonth());
//
//            LOG.trace(String.format("Retrieving events [%s, %s], search: %s",
//                    start.format(ISO_OFFSET_DATE_TIME), end.format(ISO_OFFSET_DATE_TIME), search));
//
//            List<Event> events = db.getEvents(start, end, search);
//            List<Reservation> reservations = new ReservationDB().getEventReservations(start, end, search);
//            List<Recurrence> recurrences = new RecurrenceDB().getEventRecurrences(start, end, search);
//
//            resMan.populateRservations(events, reservations);
//            recurMan.populateRecurrences(events, recurrences);
//            return events;
//        } catch (Throwable t) {
//            LOG.error("Retrieving events failed:", t);
//        }
//        return null;
//    }

    @GET @Path("/ministry/{ministryId}") @Produces(MediaType.APPLICATION_JSON)
    public List<Event> getUpcomingEvents(@PathParam("ministryId") int ministryId,
                                         @QueryParam("count") @DefaultValue("10") int count) {

        verifyUserAccess("event.list");
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
        verifyUserAccess("event.create");
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
        verifyUserAccess("event.update");
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
        verifyUserAccess("event.delete");
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