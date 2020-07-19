package org.servantscode.schedule.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.schedule.*;
import org.servantscode.schedule.Event.SacramentType;
import org.servantscode.schedule.db.EventDB;
import org.servantscode.schedule.db.RecurrenceDB;
import org.servantscode.schedule.db.ReservationDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.servantscode.commons.StringUtils.isSet;
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
                                                                        "category_names",
                                                                        "sacrament_type");

    private EventDB db;
    private RecurrenceDB recurDb;

    private ReservationManager resMan;
    private EventManager eventMan;
    private RecurrenceManager recurMan;
    private EventPrivatizer privatizer = new EventPrivatizer();

    public EventSvc() {
        db = new EventDB();
        recurDb = new RecurrenceDB();
        resMan = new ReservationManager();
        eventMan = new EventManager();
        recurMan = new RecurrenceManager();
    }

    @GET @Path("/{id}") @Produces(APPLICATION_JSON)
    public Event getEvent(@PathParam("id") int id) {
        return processRequest(() -> {
            verifyUserAccess("event.read");
            privatizer.configurePrivatizer(userHasAccess("event.private.read"), getUserId());

            Event event = db.getEvent(id);
            event.setReservations(resMan.getReservationsForEvent(event.getId()));
            event.setRecurrence(recurMan.getRecurrence(event.getRecurringMeetingId()));
            return privatizer.privatize(event);
        });
    }

    @GET @Path("/{id}/futureEvents") @Produces(APPLICATION_JSON)
    public List<Event> getFutureEvents(@PathParam("id") int id) {
        return processRequest(() -> {
            verifyUserAccess("event.read");
            privatizer.configurePrivatizer(userHasAccess("event.private.read"), getUserId());
            if (id <= 0)
                throw new NotFoundException();

            Event dbEvent = db.getEvent(id);
            if (dbEvent == null || dbEvent.getRecurringMeetingId() <= 0)
                throw new NotFoundException();

            List<Event> events = db.getUpcomingRecurringEvents(dbEvent.getRecurringMeetingId(), dbEvent.getStartTime());
            addReservationsAndRecurrences(events);

            return privatizer.privatizeEvents(events);
        });
    }


    @POST @Path("/futureTimes") @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public List<ZonedDateTime> calculateFutureTimes(Event e) {
        return processRequest(() -> {
            if (e == null || e.getRecurrence() == null || e.getStartTime() == null)
                throw new BadRequestException();

            if (e.getRecurrence().getCycle() == CUSTOM)
                return db.getFutureEvents(e);
            return recurMan.getFutureTimes(e.getRecurrence(), e.getStartTime());
        });
    }

    @GET @Produces(APPLICATION_JSON)
    public PaginatedResponse<Event> getEvents(@QueryParam("start") @DefaultValue("0") int start,
                                              @QueryParam("count") @DefaultValue("32768") int count,
                                              @QueryParam("sort_field") @DefaultValue("start_time") String sortField,
                                              @QueryParam("search") @DefaultValue("") String search) {

        return processRequest(() -> {
            verifyUserAccess("event.list");
            privatizer.configurePrivatizer(userHasAccess("event.private.read"), getUserId());

            LOG.trace(String.format("Retrieving events (%s, %s, page: %d; %d)", search, sortField, start, count));
            int totalEvents = db.getCount(search);
            List<Event> events = db.getEvents(search, sortField, start, count);
            addReservationsAndRecurrences(search, events);

            return new PaginatedResponse<>(start, events.size(), totalEvents, privatizer.privatizeEvents(events));
        });
    }


    @GET @Path("/bulk") @Produces(APPLICATION_JSON)
    public List<Event> getEventsById(@QueryParam("ids") String idString) {
        return processRequest(() -> {
            verifyUserAccess("event.list");
            privatizer.configurePrivatizer(userHasAccess("event.private.read"), getUserId());

            List<Integer> ids = Stream.of(idString.split(",")).map(Integer::parseInt).collect(Collectors.toList());
            if(ids.size() > 100)
                throw new BadRequestException();

            LOG.trace(String.format("Retrieving %d events by id", ids.size()));
            List<Event> events = db.getEventsById(ids);

            List<Reservation> reservations = new ReservationDB().getEventReservationsById(ids);
            List<Recurrence> recurrences = new RecurrenceDB().getEventRecurrencesById(ids);

            resMan.populateRservations(events, reservations);
            recurMan.populateRecurrences(events, recurrences);

            return events;
        });
    }

    @GET @Path("/ministry/{ministryId}") @Produces(APPLICATION_JSON)
    public List<Event> getUpcomingEvents(@PathParam("ministryId") int ministryId,
                                         @QueryParam("count") @DefaultValue("10") int count) {

        return processRequest(() -> {
            verifyUserAccess("event.list");
            privatizer.configurePrivatizer(userHasAccess("event.private.read"), getUserId());
            List<Event> events = db.getUpcomingMinistryEvents(ministryId, count);
            addReservationsAndRecurrences(events);
            return privatizer.privatizeEvents(events);
        });
    }

    @GET @Path("/sacrament/{sacramentType}") @Produces(APPLICATION_JSON)
    public PaginatedResponse<Event> getSacraments(@PathParam("sacramentType") String typeString,
                                     @QueryParam("start") @DefaultValue("0") int start,
                                     @QueryParam("count") @DefaultValue("32768") int count,
                                     @QueryParam("sort_field") @DefaultValue("start_time") String sort,
                                     @QueryParam("search") @DefaultValue("") String search) {

        return processRequest(() -> {
            verifyUserAccess("event.list");
            SacramentType type = SacramentType.valueOf(typeString.toUpperCase());
            String fullSearch = (isSet(search)? search: "") + " sacrament_type:" + type.toString();

            int totalEvents = db.getCount(search);
            List<Event> events = db.getEvents(fullSearch, sort, start, count);
            addReservationsAndRecurrences(search, events);

            privatizer.configurePrivatizer(userHasAccess("event.private.read"), getUserId());
            return new PaginatedResponse<>(start, events.size(), totalEvents, privatizer.privatizeEvents(events));
        });
    }

    @GET @Path("/report") @Produces(MediaType.TEXT_PLAIN)
    public Response getEventReport(@QueryParam("search") @DefaultValue("") String search,
                                    @QueryParam("include_inactive") @DefaultValue("false") boolean includeInactive) {

        return processRequest(() -> {
            verifyUserAccess("event.export");
            return Response.ok(db.getReportReader(search, EXPORTABLE_FIELDS)).build();
        });
    }

    @POST
    @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Event createEvent(Event event) {
        return processRequest(() -> {
            verifyUserAccess("event.create");
            LOG.debug("Creating event for: " + event.getStartTime().toString());

            if(event.getRecurrence() != null)
                return recurMan.createRecurringEvent(event);
            return eventMan.createEvent(event);
        });
    }

    @POST @Path("/series")
    @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Event createEventSeries(List<Event> events) {
        return processRequest(() -> {
            verifyUserAccess("event.create");

            if(events.isEmpty())
                throw new BadRequestException();

            LOG.debug("Creating event for: " + events.get(0).getStartTime().toString());
            Recurrence recur = new Recurrence();
            recur.setCycle(CUSTOM);
            recurDb.create(recur); //Side effect: Sets Recurrence id
            events.forEach(event -> {
                event.setRecurrence(recur);
                event.setRecurringMeetingId(recur.getId());
            });

            return recurMan.createEventSeries(events);
        });
    }

    @PUT
    @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Event updateEvent(Event event) {
        return processRequest(() -> {
            verifyUserAccess("event.update");
            Event dbEvent = db.getEvent(event.getId());
            if(dbEvent == null)
                throw new NotFoundException();
            if(event.getSchedulerId() != getUserId() && !userHasAccess("admin.event.edit"))
                throw new ForbiddenException();

            if(event.getRecurrence() != null)
                return recurMan.updateRecurringEvent(event, dbEvent);
            return eventMan.updateEvent(event);
        });
    }

    @PUT @Path("/series")
    @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Event updateEventSeries(List<Event> events) {
        return processRequest(() -> {
            verifyUserAccess("event.update");

            int userId = getUserId();
            if(events.stream().anyMatch(event -> event.getSchedulerId() != userId) && !userHasAccess("admin.event.edit"))
                throw new ForbiddenException();

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
        });
    }

    @DELETE @Path("/{id}")
    public void deleteEvent(@PathParam("id") int id,
                            @QueryParam("deleteFutureEvents") boolean deleteFutureEvents) {
        processRequest(() -> {
            verifyUserAccess("event.delete");
            if(id <= 0)
                throw new NotFoundException();

            Event event = db.getEvent(id);
            if(event == null ||
                    event.getSchedulerId() != getUserId() && !userHasAccess("admin.event.delete"))
                throw new ForbiddenException();

            if(event.getRecurringMeetingId() > 0 && deleteFutureEvents)
                recurMan.deleteRecurringEvent(event);
            else
                eventMan.deleteEvent(event);
        });
    }

    // ----- Private -----
    private void addReservationsAndRecurrences(@DefaultValue("") @QueryParam("search") String search, List<Event> events) {
        List<Reservation> reservations = new ReservationDB().getEventReservations(search);
        List<Recurrence> recurrences = new RecurrenceDB().getEventRecurrences(search);
        resMan.populateRservations(events, reservations);
        recurMan.populateRecurrences(events, recurrences);
    }

    private void addReservationsAndRecurrences(List<Event> events) {
        events.forEach(event -> {
            event.setReservations(resMan.getReservationsForEvent(event.getId()));
            event.setRecurrence(recurMan.getRecurrence(event.getRecurringMeetingId()));
        });
    }
}
