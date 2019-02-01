package org.servantscode.schedule.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.schedule.Event;
import org.servantscode.schedule.Reservation;

import java.sql.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

@SuppressWarnings("SqlNoDataSourceInspection")
public class ReservationDB extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(ReservationDB.class);

    public List<Reservation> getReservations(ZonedDateTime start, ZonedDateTime end, int eventId, int personId,
                                             Reservation.ResourceType resourceType, int resourceId) {

        OptionalAnd optAnd = new OptionalAnd();
        String sql = "SELECT r.*, COALESCE(ro.name, e.name) as resource_name FROM reservations r " +
                    "LEFT JOIN rooms ro ON ro.id = r.resource_id AND r.resource_type='ROOM' " +
                    "LEFT JOIN equipment e ON e.id = r.resource_id AND r.resource_type='EQUIPMENT' " +
                    "WHERE " +
                    (start != null? optAnd.next() + "start_time < ? AND end_time > ? ": "") +
                    (eventId > 0? optAnd.next() + "event_id=? ": "") +
                    (personId > 0? optAnd.next() + "reserving_person_id=? " : "") +
                    (resourceId > 0? optAnd.next() + "resource_type=? AND resource_id=?": "");
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
        ) {

            int index = 0;
            if(start != null) {
                LOG.trace("Searching for reservations from " + start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) +
                          " to " + end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                stmt.setTimestamp(++index, convert(end));
                stmt.setTimestamp(++index, convert(start));
            }

            if(eventId > 0) stmt.setInt(++index, eventId);
            if(personId > 0) stmt.setInt(++index, personId);

            if(resourceId > 0) {
                stmt.setString(++index, resourceType.toString());
                stmt.setInt(++index, resourceId);
            }

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve reservations for reserver: " + personId, e);
        }
    }

    public List<Reservation> getEventReservations(ZonedDateTime startDate, ZonedDateTime endDate, String search) {
        String sql = format("SELECT r.*, COALESCE(ro.name, e.name) as resource_name FROM reservations r " +
                "LEFT JOIN rooms ro ON ro.id = r.resource_id AND r.resource_type='ROOM' " +
                "LEFT JOIN equipment e ON e.id = r.resource_id AND r.resource_type='EQUIPMENT' " +
                "WHERE r.event_id IN (SELECT id FROM events WHERE%s end_time > ? AND start_time < ?)", EventDB.optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            stmt.setTimestamp(1,  convert(startDate));
            stmt.setTimestamp(2,  convert(endDate));

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve event reservations.", e);
        }
    }

    public List<Reservation> getReservationsForPerson(int personId) {
        String sql = "SELECT * FROM reservations WHERE reserving_person_id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
        ) {

            stmt.setInt(1, personId);

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve reservations for reserver: " + personId, e);
        }
    }

    public List<Reservation> getReservationsForEvent(int eventId) {
        String sql = "SELECT r.*, COALESCE(ro.name, e.name) as resource_name FROM reservations r " +
                    "LEFT JOIN rooms ro ON ro.id = r.resource_id AND r.resource_type='ROOM' " +
                    "LEFT JOIN equipment e ON e.id = r.resource_id AND r.resource_type='EQUIPMENT' " +
                    "WHERE r.event_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
        ) {

            stmt.setInt(1, eventId);

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve reservations for event: " + eventId, e);
        }
    }

    public List<Reservation> getReservationsForResource(Reservation.ResourceType type, int id) {
        String sql = "SELECT r.*, COALESCE(ro.name, e.name) as resource_name FROM reservations r " +
                    "LEFT JOIN rooms ro ON ro.id = r.resource_id AND r.resource_type='ROOM' " +
                    "LEFT JOIN equipment e ON e.id = r.resource_id AND r.resource_type='EQUIPMENT' " +
                    "WHERE resource_type=? AND resource_id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
        ) {

            stmt.setString(1, type.toString());
            stmt.setInt(2, id);

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve reservations for " + type + ": " + id, e);
        }
    }

    public Reservation getReservation(int id) {
        String sql = "SELECT r.*, COALESCE(ro.name, e.name) as resource_name FROM reservations r " +
                    "LEFT JOIN rooms ro ON ro.id = r.resource_id AND r.resource_type='ROOM' " +
                    "LEFT JOIN equipment e ON e.id = r.resource_id AND r.resource_type='EQUIPMENT' " +
                    "WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
        ) {

            stmt.setInt(1, id);

            List<Reservation> reservations = processResults(stmt);
            if(reservations.isEmpty())
                return null;

            return reservations.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve reservation: " + id, e);
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
                res.setEventId(rs.getInt("event_id"));
                res.setStartTime(convert(rs.getTimestamp("start_time")));
                res.setEndTime(convert(rs.getTimestamp("end_time")));
                reservations.add(res);
            }
            return reservations;
        }
    }

    private static class OptionalAnd {
        private int called = 0;

        public String next() {
            return called++ > 0? "AND ": "";
        }
    }
}
