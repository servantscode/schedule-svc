package org.servantscode.schedule.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.db.EasyDB;
import org.servantscode.commons.search.InsertBuilder;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.UpdateBuilder;
import org.servantscode.schedule.Reservation;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("SqlNoDataSourceInspection")
public class ReservationDB extends EasyDB<Reservation> {
    private static final Logger LOG = LogManager.getLogger(ReservationDB.class);

    public ReservationDB() {
        super(Reservation.class, "ev.title");
    }

    private QueryBuilder queryData() {
        return select("r.*", "COALESCE(ro.name, e.name) as resource_name", "ev.title", "ev.private_event", "ev.scheduler_id", "p.name AS reserver_name")
                .from("reservations r")
                .join("LEFT JOIN rooms ro ON ro.id = r.resource_id AND r.resource_type='ROOM' ")
                .join("LEFT JOIN equipment e ON e.id = r.resource_id AND r.resource_type='EQUIPMENT' ")
                .join("LEFT JOIN events ev ON r.event_id = ev.id ")
                .join("LEFT JOIN people p ON r.reserving_person_id = p.id ");
    }

    public List<Reservation> getReservations(ZonedDateTime start, ZonedDateTime end, int eventId, int personId,
                                             Reservation.ResourceType resourceType, int resourceId) {
        QueryBuilder query = queryData();
        if(start != null)
            query.where("NOT (r.start_time <= ? AND r.end_time <= ?) AND NOT (r.start_time >= ? AND r.end_time >= ?)",
                start, start, end, end);
        if(eventId > 0) query.where("event_id=?", eventId);
        if(personId > 0) query.where("reserving_person_id=?", personId);
        if(resourceType != null) query.where("resource_type=?", resourceType.toString());
        if(resourceId > 0) query.where("resource_id=?", resourceId);
        query.sort("start_time");

        return get(query);
    }

    public List<Reservation> getEventReservations(String search) {
        QueryBuilder query = queryData()
                .whereIdIn("r.event_id", select("e.id").from("events e")
                        .join("LEFT JOIN (SELECT array_agg(d.id) AS department_ids, array_agg(d.name) AS department_names, event_id FROM departments d, event_departments ed WHERE d.id=ed.department_id GROUP BY event_id) depts ON depts.event_id=e.id")
                        .join("LEFT JOIN (SELECT array_agg(c.id) AS category_ids, array_agg(c.name) AS category_names, event_id FROM categories c, event_categories cd WHERE c.id=cd.category_id GROUP BY event_id) cats ON cats.event_id=e.id")
                        .join("LEFT JOIN ministries m ON ministry_id=m.id").inOrg("e.org_id").search(EventDB.parseSearch(search)));

        return get(query);
    }

    public List<Reservation> getReservationsForEvent(int eventId) {
        return get(queryData().with("r.event_id", eventId));
    }

    public List<Reservation> getReservationsForResource(Reservation.ResourceType type, int id) {
        return get(queryData().where("resource_type=?", type.toString()).where("resource_id=?", id));
    }

    public Reservation getReservation(int id) {
        return getOne(queryData().with("r.id", id));
    }

    public List<Reservation> getConflicts(Reservation res, int recurrenceId) {
        QueryBuilder query = queryData()
                .where("NOT (r.start_time <= ? AND r.end_time <= ?) AND NOT (r.start_time >= ? AND r.end_time >= ?)",
                        res.getStartTime(), res.getStartTime(), res.getEndTime(), res.getEndTime())
                .where("r.resource_type = ?", res.getResourceType().toString())
                .where("r.resource_id = ?", res.getResourceId())
                .where("ev.recurring_meeting_id <> ?", recurrenceId)
                .sort("r.start_time");

        return get(query);
    }

    public Reservation create(Reservation reservation) {
        InsertBuilder cmd = insertInto("reservations")
                .value("resource_type", reservation.getResourceType())
                .value("resource_id", reservation.getResourceId())
                .value("reserving_person_id", reservation.getReservingPersonId())
                .value("event_id", reservation.getEventId())
                .value("start_time", convert(reservation.getStartTime()))
                .value("end_time", convert(reservation.getEndTime()));
        reservation.setId(createAndReturnKey(cmd));
        return reservation;
    }

    public Reservation update(Reservation reservation) {
        UpdateBuilder cmd = update("reservations")
                .value("resource_type", reservation.getResourceType())
                .value("resource_id", reservation.getResourceId())
                .value("reserving_person_id", reservation.getReservingPersonId())
                .value("event_id", reservation.getEventId())
                .value("start_time", convert(reservation.getStartTime()))
                .value("end_time", convert(reservation.getEndTime()))
                .withId(reservation.getId());

        if (!update(cmd))
            throw new RuntimeException("Could not update reservation for " + reservation.getResourceType() + ": " + reservation.getResourceId());

        return reservation;
    }

    public boolean delete(int id) {
        return delete(deleteFrom("reservations").withId(id));
    }

    public boolean deleteReservationsByEvent(int eventId) {
        return delete(deleteFrom("reservations").with("event_id", eventId));
    }

    // ----- Private -----

    @Override
    protected Reservation processRow(ResultSet rs) throws SQLException {
        Reservation res = new Reservation();
        res.setId(rs.getInt("id"));
        res.setResourceType(Reservation.ResourceType.valueOf(rs.getString("resource_type")));
        res.setResourceId(rs.getInt("resource_id"));
        res.setResourceName(rs.getString("resource_name"));
        res.setReservingPersonId(rs.getInt("reserving_person_id"));
        res.setReserverName(rs.getString("reserver_name"));
        res.setEventId(rs.getInt("event_id"));
        res.setEventTitle(rs.getString("title"));
        res.setPrivateEvent(rs.getBoolean("private_event"));
        res.setStartTime(convert(rs.getTimestamp("start_time")));
        res.setEndTime(convert(rs.getTimestamp("end_time")));
        return res;
    }
}
