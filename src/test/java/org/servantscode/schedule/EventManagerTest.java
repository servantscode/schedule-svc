package org.servantscode.schedule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.servantscode.schedule.db.EventDB;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EventManagerTest {

    @Mock
    EventDB db;
    @Mock
    ReservationManager resMan;

    private EventManager ev;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void initialise() {
        ev = new EventManager(db, resMan);
    }

    @Test(expected = NullPointerException.class)
    public void createEventTestNullObject() {
        when(db.create(any(Event.class))).thenReturn(getAnswerEvent());
        ev.createEvent(null);
    }

    @Test
    public void createEventTestNullValues() {
        Event ans = new Event();
        ans.setId(10);
        when(db.create(any(Event.class))).thenReturn(ans);

        Event e = new Event();
        Event resp = ev.createEvent(e);
        verify(db, times(1)).create(e);
        verify(resMan, times(1)).createReservationsForEvent(e.getReservations(), 10);

        assertEquals("Difference between returned and expected Event values: Id", 10, resp.getId());
        assertNull("Difference between returned and expected Event values: startTime", resp.getStartTime());
        assertNull("Difference between returned and expected Event values: endTime", resp.getEndTime());
        assertNull("Difference between returned and expected Event values: title", resp.getTitle());
        assertNull("Difference between returned and expected Event values: description", resp.getDescription());
        assertFalse("Difference between returned and expected Event Values: isPrivateEvent", resp.isPrivateEvent());
        assertEquals("Difference between returned and expected Event values: schedulerId", 0, resp.getSchedulerId());
        assertEquals("Difference between returned and expected Event values: contactId", 0, resp.getContactId());

        assertNull("Returned a department list where null was expected", resp.getDepartments());

        assertNull("Returned a category list where null was expected", resp.getCategories());

        assertNull("Returned a reservation list where null was expected", resp.getReservations());
    }

    @Test
    public void createEventTestFilled() {
        when(db.create(any(Event.class))).thenReturn(getAnswerEvent());

        Event e = getTestEvent();
        Event resp = ev.createEvent(e);

        verify(db, times(1)).create(e);
        verify(resMan, times(1)).createReservationsForEvent(e.getReservations(), 10);

        checkEvent(e, resp);
        assertEquals("Difference in reservation value: eventId", 10, e.getReservations().get(0).getEventId());
    }

    @Test
    public void updateEvent() {
        when(db.updateEvent(any(Event.class))).thenReturn(getAnswerEvent());

        Event e = getTestEvent();
        Event resp = ev.updateEvent(e);

        verify(db, times(1)).updateEvent(e);
        verify(resMan, times(1)).updateRservationsForEvent(e.getReservations(), 10);

        checkEvent(e, resp);
        assertEquals("Difference in reservation value: eventId", 1, e.getReservations().get(0).getEventId());
    }

    @Test
    public void deleteEvent() {
        when(db.deleteEvent(anyInt())).thenReturn(true);

        Event e = getTestEvent();
        boolean resp = ev.deleteEvent(e);

        verify(db, times(1)).deleteEvent(e.getId());
        verify(resMan, times(1)).deleteReservationsForEvent(e.getId());

        assertTrue("Wrong response", resp);
    }

    private void checkEvent(Event e, Event resp) {
        ZonedDateTime zd = ZonedDateTime.of(LocalDateTime.of(LocalDate.of(2015, 10, 21), LocalTime.of(16, 49, 20)), ZoneId.of("Etc/GMT-7"));
        Reservation r = e.getReservations().get(0);

        assertEquals("Difference between returned and expected Event values: Id", 10, resp.getId());
        assertTrue("Difference between returned and expected Event values: startTime", zd.isEqual(resp.getStartTime()));
        assertTrue("Difference between returned and expected Event values: endTime", zd.plusHours(3).isEqual(resp.getEndTime()));
        assertEquals("Difference between returned and expected Event values: title", "Destination", resp.getTitle());
        assertEquals("Difference between returned and expected Event values: description", "Just a test", resp.getDescription());
        assertFalse("Difference between returned and expected Event Values: isPrivateEvent", resp.isPrivateEvent());
        assertEquals("Difference between returned and expected Event values: schedulerId", 1, resp.getSchedulerId());
        assertEquals("Difference between returned and expected Event values: contactId", 1, resp.getContactId());

        List<String> departments = resp.getDepartments();
        assertEquals("Wrong size of department list", 2, departments.size());
        assertEquals("Difference between returned and expected Department values", "Department of Time Travel", departments.get(0));
        assertEquals("Difference between returned and expected Department values", "Department of Safety", departments.get(1));

        List<String> categories = resp.getCategories();
        assertEquals("Wrong size of category list", 3, categories.size());
        assertEquals("Difference between returned and expected category values", "Time", categories.get(0));
        assertEquals("Difference between returned and expected category values", "Marty McFly", categories.get(1));
        assertEquals("Difference between returned and expected category values", "Routine", categories.get(2));

        Reservation res = resp.getReservations().get(0);
        assertEquals("Difference in reservation value: Id", 2, res.getId());
        assertEquals("Difference in reservation enumerator", Reservation.ResourceType.EQUIPMENT, res.getResourceType());
        assertEquals("Difference in reservation value: resourceId", 2, res.getResourceId());
        assertEquals("Difference in reservation value: resourceName", "DeLorean", res.getResourceName());
        assertEquals("Difference in reservation value: reservingPersonId", 1, r.getReservingPersonId());
        assertEquals("Difference in reservation value: reserverName", "Dr. Emmett Brown", res.getReserverName());
        assertEquals("Difference in reservation value: eventTitle", "Test", res.getEventTitle());
    }

    private Event getAnswerEvent() {
        Event e = new Event();
        ZonedDateTime zd = ZonedDateTime.of(LocalDateTime.of(LocalDate.of(2015, 10, 21), LocalTime.of(16, 49, 20)), ZoneId.of("Etc/GMT-7"));
        e.setId(10);
        e.setStartTime(zd);
        e.setEndTime(zd.plusHours(3));
        e.setTitle("Destination");
        e.setDescription("Just a test");
        e.setPrivateEvent(false);
        e.setSchedulerId(1);
        e.setContactId(1);
        e.setDepartments(new ArrayList<>());
        e.getDepartments().add("Department of Time Travel");
        e.getDepartments().add("Department of Safety");
        e.setCategories(new ArrayList<>());
        e.getCategories().add("Time");
        e.getCategories().add("Marty McFly");
        e.getCategories().add("Routine");
        Reservation r = new Reservation();
        r.setId(2);
        r.setResourceType(Reservation.ResourceType.EQUIPMENT);
        r.setResourceId(2);
        r.setResourceName("DeLorean");
        r.setReservingPersonId(1);
        r.setReserverName("Dr. Emmett Brown");
        r.setEventId(1);
        r.setEventTitle("Test");
        r.setStartTime(e.getStartTime());
        r.setEndTime(e.getEndTime());
        e.setReservations(new ArrayList<>());
        e.getReservations().add(r);

        return e;
    }

    private Event getTestEvent() {
        Event e = new Event();
        ZonedDateTime zd = ZonedDateTime.of(LocalDateTime.of(LocalDate.of(2015, 10, 21), LocalTime.of(16, 49, 20)), ZoneId.of("Etc/GMT-7"));
        e.setStartTime(zd);
        e.setEndTime(zd.plusHours(3));
        e.setTitle("Destination");
        e.setDescription("Just a test");
        e.setPrivateEvent(false);
        e.setSchedulerId(1);
        e.setContactId(1);
        e.setDepartments(new ArrayList<>());
        e.getDepartments().add("Department of Time Travel");
        e.getDepartments().add("Department of Safety");
        e.setCategories(new ArrayList<>());
        e.getCategories().add("Time");
        e.getCategories().add("Marty McFly");
        e.getCategories().add("Routine");
        Reservation r = new Reservation();
        r.setId(2);
        r.setResourceType(Reservation.ResourceType.EQUIPMENT);
        r.setResourceId(2);
        r.setResourceName("DeLorean");
        r.setReservingPersonId(1);
        r.setReserverName("Dr. Emmett Brown");
        r.setEventId(1);
        r.setEventTitle("Test");
        r.setStartTime(e.getStartTime());
        r.setEndTime(e.getEndTime());
        e.setReservations(new ArrayList<>());
        e.getReservations().add(r);

        return e;
    }
}
