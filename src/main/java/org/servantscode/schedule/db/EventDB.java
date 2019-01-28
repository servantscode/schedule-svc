package org.servantscode.schedule.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.schedule.Event;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.sql.Types.INTEGER;
import static org.servantscode.commons.StringUtils.isEmpty;

@SuppressWarnings("SqlNoDataSourceInspection")
public class EventDB extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(EventDB.class);

    public Event getEvent(int id) {
        String sql = "SELECT e.*, m.name AS ministry_name FROM events e LEFT JOIN ministries m ON ministry_id=m.id WHERE e.id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
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

    public List<Event> getEvents(ZonedDateTime startDate, ZonedDateTime endDate, String search) {
        String sql = format("SELECT *, m.name AS ministry_name FROM events LEFT JOIN ministries m ON ministry_id=m.id WHERE%s end_time > ? AND start_time < ? ORDER BY start_time", optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            stmt.setTimestamp(1,  convert(startDate));
            stmt.setTimestamp(2,  convert(endDate));

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve events.", e);
        }
    }

    public List<Event> getUpcomingMinistryEvents(int ministryId, int count) {
        String sql = "SELECT *, m.name AS ministry_name FROM events LEFT JOIN ministries m ON ministry_id=m.id WHERE start_time > now() AND ministry_id=? ORDER BY start_time LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, ministryId);
            stmt.setInt(2, count);

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve events.", e);
        }
    }

    public List<Event> getUpcomingRecurringEvents(int recurrenceId, ZonedDateTime start) {
        String sql = "SELECT *, m.name AS ministry_name FROM events LEFT JOIN ministries m ON ministry_id=m.id WHERE recurring_meeting_id=? AND start_time >= ? ORDER BY start_time";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, recurrenceId);
            stmt.setTimestamp(2, convert(start));

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve events.", e);
        }
    }


    public Event create(Event event) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO events(recurring_meeting_id, start_time, end_time, description, scheduler_id, ministry_id) values (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){

            stmt.setInt(1, event.getRecurringMeetingId());
            stmt.setTimestamp(2, convert(event.getStartTime()));
            stmt.setTimestamp(3, convert(event.getEndTime()));
            stmt.setString(4, event.getDescription());
            stmt.setInt(5, event.getSchedulerId());
            if(event.getMinistryId()> 0) {
                stmt.setInt(6, event.getMinistryId());
            } else {
                stmt.setNull(6, INTEGER);
            }

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
             PreparedStatement stmt = conn.prepareStatement("UPDATE events SET recurring_meeting_id=?, start_time=?, end_time=?, description=?, scheduler_id=?, ministry_id=? WHERE id=?")
        ) {

            stmt.setInt(1, event.getRecurringMeetingId());
            stmt.setTimestamp(2, convert(event.getStartTime()));
            stmt.setTimestamp(3, convert(event.getEndTime()));
            stmt.setString(4, event.getDescription());
            stmt.setInt(5, event.getSchedulerId());
            if(event.getMinistryId()> 0) {
                stmt.setInt(6, event.getMinistryId());
            } else {
                stmt.setNull(6, INTEGER);
            }
            stmt.setInt(7, event.getId());

            if (stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update event: " + event.getDescription());

            return event;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update event: " + event.getDescription(), e);
        }
    }

    public boolean deleteEvent(int id) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM events WHERE id=?")
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
                e.setRecurringMeetingId(rs.getInt("recurring_meeting_id"));
                e.setStartTime(convert(rs.getTimestamp("start_time")));
                e.setEndTime(convert(rs.getTimestamp("end_time")));
                e.setDescription(rs.getString("description"));
                e.setSchedulerId(rs.getInt("scheduler_id"));
                e.setMinistryId(rs.getInt("ministry_id"));
                e.setMinistryName(rs.getString("ministry_name"));
                events.add(e);
            }
            return events;
        }
    }

    private String optionalWhereClause(String search) {
        return !isEmpty(search) ? format(" description ILIKE '%%%s%%'", search.replace("'", "''")) : "";
    }
}
