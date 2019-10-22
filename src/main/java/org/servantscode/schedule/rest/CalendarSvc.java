package org.servantscode.schedule.rest;

import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Email;
import net.fortuna.ical4j.model.property.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.schedule.Event;
import org.servantscode.schedule.Reservation;
import org.servantscode.schedule.db.EventDB;
import org.servantscode.schedule.db.ReservationDB;

import javax.mail.internet.AddressException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;

@Path("/calendar")
public class CalendarSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(CalendarSvc.class);

    private final EventDB eventDb;
    private final ReservationDB resDb;

    static {
        System.getProperties().put("net.fortuna.ical4j.timezone.cache.impl", "net.fortuna.ical4j.util.MapTimeZoneCache");
    }

    public CalendarSvc() {
        this.eventDb = new EventDB();
        this.resDb = new ReservationDB();
    }

    @GET @Path("/public") @Produces("text/calendar")
    public String getPublicCalendar(){
        TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
        //Adjust everything to server time.
        TimeZone timezone = registry.getTimeZone(ZoneId.systemDefault().toString());
        VTimeZone tz = timezone.getVTimeZone();

        String host = OrganizationContext.getOrganization().getHostName();

        Calendar cal = new Calendar();
        cal.getProperties().add(new ProdId("-//Servant's Code//iCal4j 1.0//EN"));
        cal.getProperties().add(Version.VERSION_2_0);
        cal.getProperties().add(CalScale.GREGORIAN);
        cal.getComponents().add(tz);

        List<Event> events = eventDb.getEvents("privateEvent:false", "modified_time DESC", 0, 0);
        for(Event e: events) {
            VEvent event = new VEvent(convert(e.getStartTime()), convert(e.getEndTime()), e.getTitle());
            event.getProperties().add(new Uid(e.getId() + "@" + host + ".servantscode.org"));
            event.getProperties().add(tz.getTimeZoneId());
            event.getProperties().add(new Description(e.getDescription()));
            if(e.getCreatedTime() != null)
                event.getProperties().add(new Created(convert(e.getCreatedTime())));
            if(e.getModifiedTime() != null)
                event.getProperties().add(new LastModified(convert(e.getModifiedTime())));
            event.getProperties().add(new Sequence(e.getSequenceNumber()));

            Location location = getLocation(e.getId());
            if(location != null)
                event.getProperties().add(location);

            Organizer organizer = getOrganizer(e);
            if(organizer != null)
                event.getProperties().add(organizer);

            cal.getComponents().add(event);
        }

        cal.validate(true);
        return cal.toString();
    }

    private Organizer getOrganizer(Event e) {
        if(isEmpty(e.getContactName()) && isEmpty(e.getContactEmail()))
            return null;

        Organizer organizer = new Organizer();
        organizer.getParameters().add(new Cn(isSet(e.getContactName())? e.getContactName(): e.getContactEmail()));
        try {
            if(isSet(e.getContactEmail()))
                organizer.setValue(String.format("mailto:%s", e.getContactEmail()));
        } catch (URISyntaxException ex) {
            LOG.warn("Invalid email address found for person: " + e.getContactName(), ex);
            //Continue on... this does not invalidate the calendar.
        }
        return organizer;
    }

    private Location getLocation(int id) {
        List<Reservation> reservations = resDb.getReservationsForEvent(id);
        if(reservations.isEmpty())
            return null;

        String resString = reservations.stream()
                .filter(r -> r.getResourceType() == Reservation.ResourceType.ROOM)
                .map(Reservation::getResourceName)
                .collect(Collectors.joining(", "));
        return new Location(resString);
    }

    private DateTime convert(ZonedDateTime zdt) {
        return new DateTime(Date.from(zdt.toInstant()).getTime());
    }

//    public static void main(String[] args) {
//        OrganizationContext.enableOrganization("localhost");
//        System.err.println(new CalendarSvc().getCalendar());
//    }
}
