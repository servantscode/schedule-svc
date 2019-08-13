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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.servantscode.schedule.Recurrence.RecurrenceCycle.CUSTOM;

@Path("/event")
public class EventSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(EventSvc.class);

    private static final List<String> EXPORTABLE_FIELDS = Arrays.asList("id",
                                                                        "title",
                                                                        "description",
                                                                        "start_time",
                                                                        "end_time",
                                                                        "private_event",
                                                                        "scheduler_id",
                                                                        "contact_id",
                                                                        "ministry_name",
                                                                        "department_names",
                                                                        "category_names");

    private EventDB db;
    RecurrenceDB recurDb;
    private ReservationManager resMan;
    private EventManager eventMan;
    private RecurrenceManager recurMan;
    private EventPrivatizer privatizer = new EventPrivatizer();

    @Context SecurityContext securityContext;

    public EventSvc() {
        db = new EventDB();
        recurDb = new RecurrenceDB();
        resMan = new ReservationManager();
        eventMan = new EventManager();
        recurMan = new RecurrenceManager();
    }

    @GET @Path("/{id}") @Produces(APPLICATION_JSON)
    public Event getEvent(@PathParam("id") int id) {
        verifyUserAccess("event.read");
        privatizer.configurePrivatizer(userHasAccess("event.private.read"), getUserId(securityContext));

        try {
            Event event = db.getEvent(id);
            event.setReservations(resMan.getReservationsForEvent(event.getId()));
            event.setRecurrence(recurMan.getRecurrence(event.getRecurringMeetingId()));
            return privatizer.privatize(event);
        } catch (Throwable t) {
            LOG.error("Retrieving events failed:", t);
        }
        throw new NotFoundException();
    }

    @GET @Path("/{id}/futureEvents") @Produces(APPLICATION_JSON)
    public List<Event> getFutureEvents(@PathParam("id") int id) {
        verifyUserAccess("event.read");
        privatizer.configurePrivatizer(userHasAccess("event.private.read"), getUserId(securityContext));
        if(id <= 0)
            throw new NotFoundException();

        try {
            Event dbEvent = db.getEvent(id);
            if(dbEvent == null || dbEvent.getRecurringMeetingId() <= 0)
                throw new NotFoundException();

            List<Event> events = db.getUpcomingRecurringEvents(dbEvent.getRecurringMeetingId(), dbEvent.getStartTime());
            for(Event event: events) {
                event.setReservations(resMan.getReservationsForEvent(event.getId()));
                event.setRecurrence(recurMan.getRecurrence(event.getRecurringMeetingId()));
            }
            return privatizer.privatizeEvents(events);
        } catch (Throwable t) {
            LOG.error("Retrieving future events failed:", t);
            throw t;
        }
    }

    @POST @Path("/futureTimes") @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public List<ZonedDateTime> calculateFutureTimes(Event e) {
        if(e == null || e.getRecurrence() == null || e.getStartTime() == null)
            throw new BadRequestException();

        try {
            if(e.getRecurrence().getCycle() == CUSTOM)
                return db.getFutureEvents(e);
            return recurMan.getFutureTimes(e.getRecurrence(), e.getStartTime());
        } catch (Throwable t) {
            LOG.error("Retrieving future events failed:", t);
            throw t;
        }
    }

    @GET @Produces(APPLICATION_JSON)
    public PaginatedResponse<Event> getEvents(@QueryParam("start") @DefaultValue("0") int start,
                                              @QueryParam("count") @DefaultValue("32768") int count,
                                              @QueryParam("sort_field") @DefaultValue("start_time") String sortField,
                                              @QueryParam("search") @DefaultValue("") String search) {

        verifyUserAccess("event.list");
        privatizer.configurePrivatizer(userHasAccess("event.private.read"), getUserId(securityContext));

        try {
            LOG.trace(String.format("Retrieving events (%s, %s, page: %d; %d)", search, sortField, start, count));
            int totalEvents = db.getCount(search);

            List<Event> events;
            events = db.getEvents(search, sortField, start, count);
            List<Reservation> reservations = new ReservationDB().getEventReservations(search);
            List<Recurrence> recurrences = new RecurrenceDB().getEventRecurrences(search);

            resMan.populateRservations(events, reservations);
            recurMan.populateRecurrences(events, recurrences);

            return new PaginatedResponse<>(start, events.size(), totalEvents, privatizer.privatizeEvents(events));
        } catch (Throwable t) {
            LOG.error("Retrieving events failed:", t);
            throw t;
        }
    }

    @GET @Path("/ministry/{ministryId}") @Produces(APPLICATION_JSON)
    public List<Event> getUpcomingEvents(@PathParam("ministryId") int ministryId,
                                         @QueryParam("count") @DefaultValue("10") int count) {

        verifyUserAccess("event.list");
        privatizer.configurePrivatizer(userHasAccess("event.private.read"), getUserId(securityContext));
        try {
            List<Event> events = db.getUpcomingMinistryEvents(ministryId, count);
            for(Event event: events) {
                event.setReservations(resMan.getReservationsForEvent(event.getId()));
                event.setRecurrence(recurMan.getRecurrence(event.getRecurringMeetingId()));
            }
            return privatizer.privatizeEvents(events);
        } catch (Throwable t) {
            LOG.error("Retrieving events failed:", t);
        }
        return null;
    }

    @GET @Path("/report") @Produces(MediaType.TEXT_PLAIN)
    public Response getEventReport(@QueryParam("search") @DefaultValue("") String search,
                                    @QueryParam("include_inactive") @DefaultValue("false") boolean includeInactive) {

        verifyUserAccess("event.export");

        try {
            return Response.ok(db.getReportReader(search, EXPORTABLE_FIELDS)).build();
        } catch (Throwable t) {
            LOG.error("Retrieving event report failed:", t);
            throw t;
        }
    }

    @POST
    @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
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

    @POST @Path("/series")
    @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Event createEventSeries(List<Event> events) {
        verifyUserAccess("event.create");

        if(events.isEmpty())
            throw new BadRequestException();

        try {
            LOG.debug("Creating event for: " + events.get(0).getStartTime().toString());
            Recurrence recur = new Recurrence();
            recur.setCycle(CUSTOM);
            recurDb.create(recur); //Side effect: Sets Recurrence id
            events.forEach(event -> {
                event.setRecurrence(recur);
                event.setRecurringMeetingId(recur.getId());
            });

            return recurMan.createEventSeries(events);
        } catch (Throwable t) {
            LOG.error("Creating event series failed:", t);
            throw t;
        }
    }

    @PUT
    @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Event updateEvent(Event event,
                             @Context SecurityContext securityContext) {
        verifyUserAccess("event.update");
        try {
            Event dbEvent = db.getEvent(event.getId());
            if(dbEvent == null)
                throw new NotFoundException();
            if(event.getSchedulerId() != getUserId(securityContext) && !userHasAccess("admin.event.edit"))
                throw new ForbiddenException();

            if(event.getRecurrence() != null)
                return recurMan.updateRecurringEvent(event, dbEvent);
            return eventMan.updateEvent(event);
        } catch (Throwable t) {
            LOG.error("Updating event failed:", t);
            throw t;
        }
    }

    @PUT @Path("/series")
    @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Event updateEventSeries(List<Event> events,
                                   @Context SecurityContext securityContext) {
        verifyUserAccess("event.update");

        int userId = getUserId(securityContext);
        if(events.stream().anyMatch(event -> event.getSchedulerId() != userId) && !userHasAccess("admin.event.edit"))
            throw new ForbiddenException();

        try {
            Event dbEvent = db.getEvent(events.get(0).getId());
            if (dbEvent == null)
                throw new NotFoundException();

            Recurrence recur = new Recurrence();
            recur.setCycle(CUSTOM);
            recur.setId(dbEvent.getRecurringMeetingId());

            if (recur.getId() > 0)
                recurDb.update(recur);
            else
                recurDb.create(recur); //Side effect: Sets Recurrence id

            events.forEach(event -> {
                event.setRecurrence(recur);
                event.setRecurringMeetingId(recur.getId());
            });
            return recurMan.updateEventSeries(dbEvent, events);
        } catch (Throwable t) {
            LOG.error("Updating event series failed:", t);
            throw t;
        }
    }

    @DELETE @Path("/{id}")
    public void deleteEvent(@PathParam("id") int id,
                            @QueryParam("deleteFutureEvents") boolean deleteFutureEvents,
                            @Context SecurityContext securityContext) {
        verifyUserAccess("event.delete");
        if(id <= 0)
            throw new NotFoundException();
        try {
            Event event = db.getEvent(id);
            if(event == null)
                throw new NotFoundException();
            if(event.getSchedulerId() != getUserId(securityContext) && !userHasAccess("admin.event.delete"))
                throw new ForbiddenException();

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