package org.servantscode.schedule.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.schedule.Reservation;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("SqlNoDataSourceInspection")
public class ReservationDB extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(ReservationDB.class);

    private QueryBuilder queryData() {
        return select("r.*", "COALESCE(ro.name, e.name) as resource_name", "ev.title", "p.name AS reserver_name")
                .from("reservations r")
                .join("LEFT JOIN rooms ro ON ro.id = r.resource_id AND r.resource_type='ROOM' ")
                .join("LEFT JOIN equipment e ON e.id = r.resource_id AND r.resource_type='EQUIPMENT' ")
                .join("LEFT JOIN events ev ON r.event_id = ev.id ")
                .join("LEFT JOIN people p ON r.reserving_person_id = p.id ");
    }

    public List<Reservation> getReservations(ZonedDateTime start, ZonedDateTime end, int eventId, int personId,
                                             Reservation.ResourceType resourceType, int resourceId) {
        QueryBuilder query = queryData();
        if(start != null) query.where("r.start_time < ?", convert(end)).where("r.end_time > ?", convert(start));
        if(eventId > 0) query.where("event_id=?", eventId);
        if(personId > 0) query.where("reserving_person_id=?", personId);
        if(resourceId > 0) query.where("resource_type=?", resourceType.toString()).where("resource_id=?", resourceId);
        query.sort("start_time");

        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
        ) {
            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve reservations for reserver: " + personId, e);
        }
    }

    public List<Reservation> getEventReservations(String search) {
        QueryBuilder query = queryData()
                .whereIdIn("r.event_id", select("id").from("events").search(EventDB.parseSearch(search)));
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn)
        ) {
            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve event reservations.", e);
        }
    }

    public List<Reservation> getReservationsForEvent(int eventId) {
        QueryBuilder query = queryData() .where("r.event_id=?", eventId);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
        ) {

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve reservations for event: " + eventId, e);
        }
    }

    public List<Reservation> getReservationsForResource(Reservation.ResourceType type, int id) {
        QueryBuilder query = queryData().where("resource_type=?", type.toString()).where("resource_id=?", id);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
        ) {
            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve reservations for " + type + ": " + id, e);
        }
    }

    public Reservation getReservation(int id) {
        QueryBuilder query = queryData().where("r.id=?", id);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
        ) {
            List<Reservation> reservations = processResults(stmt);
            return reservations.isEmpty()? null: reservations.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve reservation: " + id, e);
        }
    }

    public List<Reservation> getConflicts(Reservation res, int recurrenceId) {
        QueryBuilder query = queryData()
                .where("NOT (r.start_time <= ? AND r.end_time <= ?) AND NOT (r.start_time >= ? AND r.end_time >= ?)",
                        res.getStartTime(), res.getStartTime(), res.getEndTime(), res.getEndTime())
                .where("r.resource_type = ?", res.getResourceType().toString())
                .where("r.resource_id = ?", res.getResourceId())
                .where("ev.recurring_meeting_id <> ?", recurrenceId)
                .sort("r.start_time");
        LOG.trace("Query is: " + query.getSql());
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
        ) {
            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve conflicts for recurring event: " + recurrenceId, e);
        }
    }

    public Reservation create(Reservation reservation) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO reservations(resource_type, resource_id, reserving_person_id, event_id, start_time, end_time) values (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){

            stmt.setString(1, reservation.getResourceType().toString());
            stmt.setInt(2, reservation.getResourceId());
            stmt.setInt(3, reservation.getReservingPersonId());
            stmt.setInt(4, reservation.getEventId());
            stmt.setTimestamp(5, convert(reservation.getStartTime()));
            stmt.setTimestamp(6, convert(reservation.getEndTime()));

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create reservation for " + reservation.getResourceType() + ": " + reservation.getResourceId());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    reservation.setId(rs.getInt(1));
            }
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Could not create reservation for " + reservation.getResourceType() + ": " + reservation.getResourceId());
        }
    }

    public Reservation update(Reservation reservation) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE reservations SET resource_type=?, resource_id=?, reserving_person_id=?, event_id=?, start_time=?, end_time=? WHERE id=?")
        ) {

            stmt.setString(1, reservation.getResourceType().toString());
            stmt.setInt(2, reservation.getResourceId());
            stmt.setInt(3, reservation.getReservingPersonId());
            stmt.setInt(4, reservation.getEventId());
            stmt.setTimestamp(5, convert(reservation.getStartTime()));
            stmt.setTimestamp(6, convert(reservation.getEndTime()));
            stmt.setInt(7, reservation.getId());

            if (stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update reservation for " + reservation.getResourceType() + ": " + reservation.getResourceId());

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update reservation for " + reservation.getResourceType() + ": " + reservation.getResourceId());
        }
    }

    public boolean delete(int id) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM reservations WHERE id=?")
        ) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete reservation: " + id, e);
        }
    }

    public boolean deleteReservationsByEvent(int eventId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM reservations WHERE event_id=?")
        ) {
            stmt.setInt(1, eventId);
            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete reservations for event: " + eventId, e);
        }
    }

    // ----- Private -----
    private List<Reservation> processResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            List<Reservation> reservations = new ArrayList<>();
            while (rs.next()) {
                Reservation res = new Reservation();
                res.setId(rs.getInt("id"));
                res.setResourceType(Reservation.ResourceType.valueOf(rs.getString("resource_type")));
                res.setResourceId(rs.getInt("resource_id"));
                res.setResourceName(rs.getString("resource_name"));
                res.setReservingPersonId(rs.getInt("reserving_person_id"));
                res.setReserverName(rs.getString("reserver_name"));
                res.setEventId(rs.getInt("event_id"));
                res.setEventTitle(rs.getString("title"));
                res.setStartTime(convert(rs.getTimestamp("start_time")));
                res.setEndTime(convert(rs.getTimestamp("end_time")));
                reservations.add(res);
            }
            return reservations;
        }
    }
}
