package org.servantscode.schedule.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.Search;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.schedule.Event;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.*;

import static java.sql.Types.INTEGER;
import static java.util.Collections.emptyList;

@SuppressWarnings("SqlNoDataSourceInspection")
public class EventDB extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(EventDB.class);

    private static final Map<String, String> FIELD_MAP = new HashMap<>(8);
    static {
        FIELD_MAP.put("startTime", "start_time");
        FIELD_MAP.put("endTime", "end_time");
        FIELD_MAP.put("description", "e.description");
        FIELD_MAP.put("recurringMeetingId", "recurring_meeting_id");
        FIELD_MAP.put("ministryName", "m.name");
        FIELD_MAP.put("departments", "department_names");
        FIELD_MAP.put("categories", "category_names");
        FIELD_MAP.put("privateEvent", "private_event");
    }

    private SearchParser<Event> searchParser;

    public EventDB() {
        searchParser = new SearchParser<>(Event.class,"title",FIELD_MAP);
    }

    private QueryBuilder dataQuery() {
        return select("e.*", "m.name as ministry_name, department_names, department_ids, category_names, category_ids")
                .from("events e")
                .join("LEFT JOIN (SELECT array_agg(d.id) AS department_ids, array_agg(d.name) AS department_names, event_id FROM departments d, event_departments ed WHERE d.id=ed.department_id GROUP BY event_id) depts ON depts.event_id=e.id")
                .join("LEFT JOIN (SELECT array_agg(c.id) AS category_ids, array_agg(c.name) AS category_names, event_id FROM categories c, event_categories cd WHERE c.id=cd.category_id GROUP BY event_id) cats ON cats.event_id=e.id")
                .join("LEFT JOIN ministries m ON ministry_id=m.id").inOrg("e.org_id");
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

    public List<ZonedDateTime> getFutureEvents(Event event) {
        QueryBuilder query = select("start_time").from("events")
                .where("recurring_meeting_id=?", event.getRecurringMeetingId())
                .where("start_time >= ?", event.getStartTime()).inOrg();
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
                .join("LEFT JOIN (SELECT array_agg(d.id) AS department_ids, array_agg(d.name) AS department_names, event_id FROM departments d, event_departments ed WHERE d.id=ed.department_id GROUP BY event_id) depts ON depts.event_id=e.id")
                .join("LEFT JOIN (SELECT array_agg(c.id) AS category_ids, array_agg(c.name) AS category_names, event_id FROM categories c, event_categories cd WHERE c.id=cd.category_id GROUP BY event_id) cats ON cats.event_id=e.id")
                .join("LEFT JOIN ministries m ON ministry_id=m.id")
                .search(searchParser.parse(search)).inOrg("e.org_id");
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
        QueryBuilder query = dataQuery().search(searchParser.parse(search))
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
        String sql = "INSERT INTO events(recurring_meeting_id, start_time, end_time, " +
                "title, description, private_event, " +
                "scheduler_id, contact_id, ministry_id, org_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
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
            stmt.setInt(10, OrganizationContext.orgId());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create event: " + event.getDescription());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    event.setId(rs.getInt(1));
            }

            crossReferenceDepartments(conn, event.getId(), event.getDepartmentIds());
            crossReferenceCategories(conn, event.getId(), event.getCategoryIds());

            return event;
        } catch (SQLException e) {
            throw new RuntimeException("Could not add event: " + event.getDescription(), e);
        }
    }
    public Event updateEvent(Event event) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE events SET recurring_meeting_id=?, start_time=?, end_time=?, title=?, description=?, private_event=?, scheduler_id=?, contact_id=?, ministry_id=? WHERE id=? AND org_id=?")
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
            stmt.setInt(10, event.getId());
            stmt.setInt(11, OrganizationContext.orgId());

            if (stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update event: " + event.getDescription());

            crossReferenceDepartments(conn, event.getId(), event.getDepartmentIds());
            crossReferenceCategories(conn, event.getId(), event.getCategoryIds());

            return event;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update event: " + event.getDescription(), e);
        }
    }

    public boolean deleteEvent(int id) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM events WHERE id=? AND org_id=?")
        ) {

            stmt.setInt(1, id);
            stmt.setInt(2, OrganizationContext.orgId());
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
                e.setDepartments(parseStringList(rs.getArray("department_names")));
                e.setDepartmentIds(parseIntList(rs.getArray("department_ids")));
                e.setCategories(parseStringList(rs.getArray("category_names")));
                e.setCategoryIds(parseIntList(rs.getArray("category_ids")));
                events.add(e);
            }
            return events;
        }
    }

    private List<Integer> parseIntList(Array items) throws SQLException {
        if(items == null)
            return emptyList();
        if(items.getBaseType() != Types.INTEGER)
            throw new RuntimeException("Unexpected array type encountered");
        return Arrays.asList((Integer[])items.getArray());
    }

    private List<String> parseStringList(Array items) throws SQLException {
        if(items == null)
            return emptyList();
        if(items.getBaseType() != Types.VARCHAR)
            throw new RuntimeException("Unexpected array type encountered");
        return Arrays.asList((String[])items.getArray());
    }

    /*package*/ static Search parseSearch(String search) {
        return new SearchParser<>(Event.class, "title", FIELD_MAP).parse(search);
    }

    private void crossReferenceDepartments(Connection conn, int id, List<Integer> departmentIds) throws SQLException {
        try(PreparedStatement stmt = conn.prepareStatement("DELETE FROM event_departments WHERE event_id=?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }

        if(departmentIds == null || departmentIds.isEmpty())
            return;

        try(PreparedStatement stmt = conn.prepareStatement("INSERT INTO event_departments(event_id, department_id) VALUES (?, ?)")) {
            stmt.setInt(1, id);

            for (Integer departmentId : departmentIds) {
                stmt.setInt(2, departmentId);
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    private void crossReferenceCategories(Connection conn, int id, List<Integer> categoryIds) throws SQLException {
        try(PreparedStatement stmt = conn.prepareStatement("DELETE FROM event_categories WHERE event_id=?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }

        if(categoryIds == null || categoryIds.isEmpty())
            return;

        try(PreparedStatement stmt = conn.prepareStatement("INSERT INTO event_categories(event_id, category_id) VALUES (?, ?)")) {
            stmt.setInt(1, id);

            for (Integer categoryId : categoryIds) {
                stmt.setInt(2, categoryId);
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

}
