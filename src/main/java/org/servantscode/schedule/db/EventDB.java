package org.servantscode.schedule.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.schedule.Event;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

public class EventDB extends DBAccess {

    public Event getEvent(int id) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM events WHERE id=?")
        ) {

            stmt.setInt(1, id);

            List<Event> events = processResults(stmt);
            if(events.isEmpty())
                return null;

            return events.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve event: " + id, e);
        }
    }

    public List<Event> getEvents(Date startDate, Date endDate, String search) {
        String sql = format("SELECT * FROM events WHERE%s event_datetime > ? AND event_datetime < ?", optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            stmt.setDate(1, convert(startDate));
            stmt.setDate(2, convert(endDate));

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve events.", e);
        }
    }

    public Event create(Event event) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO events(event_datetime, description, scheduler_id, ministry_id) values (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){
            stmt.setTimestamp(1, new Timestamp(event.getEventDate().getTime()));
            stmt.setString(2, event.getDescription());
            stmt.setInt(3, event.getSchedulerId());
            stmt.setInt(4, event.getMinistryId());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create event: " + event.getDescription());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    event.setId(rs.getInt(1));
            }
            return event;
        } catch (SQLException e) {
            throw new RuntimeException("Could not add event: " + event.getDescription(), e);
        }
    }


    public Event updateEvent(Event event) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE events SET event_datetime=?, description=?, scheduler_id=?, ministry_id=? WHERE id=?")
        ) {

            stmt.setTimestamp(1, new Timestamp(event.getEventDate().getTime()));
            stmt.setString(2, event.getDescription());
            stmt.setInt(3, event.getSchedulerId());
            stmt.setInt(4, event.getMinistryId());
            stmt.setInt(5, event.getId());

            if (stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update event: " + event.getDescription());

            return event;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update event: " + event.getDescription(), e);
        }
    }

    public boolean deleteEvent(int id) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM evenets WHERE id=?")
        ) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete event: " + id, e);
        }
    }

    // ----- Private -----
    private List<Event> processResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            List<Event> events = new ArrayList<>();
            while (rs.next()) {
                Event e = new Event();
                e.setId(rs.getInt("id"));
                e.setEventDate(new Date(rs.getTimestamp("event_datetime").getTime()));
                e.setDescription(rs.getString("description"));
                e.setSchedulerId(rs.getInt("scheduler_id"));
                e.setMinistryId(rs.getInt("ministry_id"));
                events.add(e);
            }
            return events;
        }
    }

    private String optionalWhereClause(String search) {
        return !isEmpty(search) ? format(" description ILIKE '%%%s%%'", search.replace("'", "''")) : "";
    }

}
