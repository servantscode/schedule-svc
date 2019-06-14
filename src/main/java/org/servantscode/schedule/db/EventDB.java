package org.servantscode.schedule.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.Search;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.schedule.Event;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.*;

import static java.sql.Types.INTEGER;

@SuppressWarnings("SqlNoDataSourceInspection")
public class EventDB extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(EventDB.class);

    private static final Map<String, String> FIELD_MAP = new HashMap<>(8);
    static {
        FIELD_MAP.put("startTime", "start_time");
        FIELD_MAP.put("endTime", "end_time");
        FIELD_MAP.put("description", "e.description");
    }

    private QueryBuilder dataQuery() {
        return select("e.*", "m.name as ministry_name")
                .from("events e")
                .join("LEFT JOIN ministries m ON ministry_id=m.id");
    }

    public Event getEvent(int id) {
        QueryBuilder query = dataQuery().where("e.id=?", id);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
        ) {
            return firstOrNull(processResults(stmt));
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve event: " + id, e);
        }
    }

    public List<ZonedDateTime> getFutureEventDates(Event event) {
        QueryBuilder query = select("start_time").from("events")
                .where("recurring_meeting_id=?", event.getRecurringMeetingId())
                .where("start_time > ?", event.getStartTime());
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            List<ZonedDateTime> futureDates = new LinkedList<>();
            while(rs.next())
                futureDates.add(convert(rs.getTimestamp("start_time")));

            return futureDates;
        } catch (SQLException e) {
            throw new RuntimeException("Could not get future recurring dates", e);
        }
    }

    public int getCount(String search) {
        QueryBuilder query = count()
                .from("events e")
                .search(parseSearch(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()
        ) {

            if (rs.next())
                return rs.getInt(1);

        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve events.", e);
        }
        return 0;
    }

    public List<Event> getEvents(String search, String sortField, int start, int count) {
        QueryBuilder query = dataQuery().search(parseSearch(search))
                .sort(sortField).limit(count).offset(start);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn)
        ) {

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve events.", e);
        }
    }

    public List<Event> getUpcomingMinistryEvents(int ministryId, int count) {
        QueryBuilder query = dataQuery().where("start_time >= now()")
                .where("ministry_id=?", ministryId)
                .sort("start_time").limit(count);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn)
        ) {

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve events.", e);
        }
    }

    public List<Event> getUpcomingRecurringEvents(int recurrenceId, ZonedDateTime start) {
        QueryBuilder query = dataQuery().where("start_time >= ?", start)
                .where("recurring_meeting_id=?", recurrenceId)
                .sort("start_time");
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn)
        ) {

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve events.", e);
        }
    }

    public Event create(Event event) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO events(recurring_meeting_id, start_time, end_time, title, description, private_event, scheduler_id, contact_id, ministry_id, departments, categories) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){

            stmt.setInt(1, event.getRecurringMeetingId());
            stmt.setTimestamp(2, convert(event.getStartTime()));
            stmt.setTimestamp(3, convert(event.getEndTime()));
            stmt.setString(4, event.getTitle());
            stmt.setString(5, event.getDescription());
            stmt.setBoolean(6, event.isPrivateEvent());
            stmt.setInt(7, event.getSchedulerId());
            if(event.getContactId()> 0) {
                stmt.setInt(8, event.getContactId());
            } else {
                stmt.setNull(8, INTEGER);
            }
            if(event.getMinistryId()> 0) {
                stmt.setInt(9, event.getMinistryId());
            } else {
                stmt.setNull(9, INTEGER);
            }
            stmt.setString(10, storeList(event.getDepartments()));
            stmt.setString(11, storeList(event.getCategories()));

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
             PreparedStatement stmt = conn.prepareStatement("UPDATE events SET recurring_meeting_id=?, start_time=?, end_time=?, title=?, description=?, private_event=?, scheduler_id=?, contact_id=?, ministry_id=?, departments=?, categories=? WHERE id=?")
        ) {

            stmt.setInt(1, event.getRecurringMeetingId());
            stmt.setTimestamp(2, convert(event.getStartTime()));
            stmt.setTimestamp(3, convert(event.getEndTime()));
            stmt.setString(4, event.getTitle());
            stmt.setString(5, event.getDescription());
            stmt.setBoolean(6, event.isPrivateEvent());
            stmt.setInt(7, event.getSchedulerId());
            if(event.getContactId()> 0) {
                stmt.setInt(8, event.getContactId());
            } else {
                stmt.setNull(8, INTEGER);
            }
            if(event.getMinistryId()> 0) {
                stmt.setInt(9, event.getMinistryId());
            } else {
                stmt.setNull(9, INTEGER);
            }
            stmt.setString(10, storeList(event.getDepartments()));
            stmt.setString(11, storeList(event.getCategories()));
            stmt.setInt(12, event.getId());

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
                e.setTitle(rs.getString("title"));
                e.setDescription(rs.getString("description"));
                e.setPrivateEvent(rs.getBoolean("private_event"));
                e.setSchedulerId(rs.getInt("scheduler_id"));
                e.setContactId(rs.getInt("contact_id"));
                e.setMinistryId(rs.getInt("ministry_id"));
                e.setMinistryName(rs.getString("ministry_name"));
                e.setDepartments(parseList(rs.getString("departments")));
                e.setCategories(parseList(rs.getString("categories")));
                events.add(e);
            }
            return events;
        }
    }

    protected static Search parseSearch(String search) {
        return new SearchParser<>(Event.class, "title", FIELD_MAP).parse(search);
    }
}
