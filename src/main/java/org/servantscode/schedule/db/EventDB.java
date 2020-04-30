package org.servantscode.schedule.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.EasyDB;
import org.servantscode.commons.db.ReportStreamingOutput;
import org.servantscode.commons.search.*;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.schedule.Event;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Collections.emptyList;

@SuppressWarnings("SqlNoDataSourceInspection")
public class EventDB extends EasyDB<Event> {
    private static final Logger LOG = LogManager.getLogger(EventDB.class);

    private static final Map<String, String> FIELD_MAP = new HashMap<>(8);
    static {
        FIELD_MAP.put("startTime", "start_time");
        FIELD_MAP.put("endTime", "end_time");
        FIELD_MAP.put("description", "e.description");
        FIELD_MAP.put("recurringMeetingId", "recurring_meeting_id");
        FIELD_MAP.put("ministryName", "m.name");
        FIELD_MAP.put("ministryId", "m.id");
        FIELD_MAP.put("departments", "department_names");
        FIELD_MAP.put("categories", "category_names");
        FIELD_MAP.put("privateEvent", "private_event");
        FIELD_MAP.put("contactId", "e.contact_id");
        FIELD_MAP.put("schedulerId", "e.scheduler_id");
    }

    public EventDB() {
        super(Event.class,"title",FIELD_MAP);
    }

    private QueryBuilder query(QueryBuilder data) {
        return data.from("events e")
                .join("LEFT JOIN (SELECT array_agg(d.id) AS department_ids, array_agg(d.name) AS department_names, event_id FROM departments d, event_departments ed WHERE d.id=ed.department_id GROUP BY event_id) depts ON depts.event_id=e.id")
                .join("LEFT JOIN (SELECT array_agg(c.id) AS category_ids, array_agg(c.name) AS category_names, event_id FROM categories c, event_categories cd WHERE c.id=cd.category_id GROUP BY event_id) cats ON cats.event_id=e.id")
                .join("LEFT JOIN people contact ON contact_id=contact.id")
                .join("LEFT JOIN ministries m ON ministry_id=m.id").inOrg("e.org_id");
    }

    private QueryBuilder allData() {
        return select("e.*", "m.name as ministry_name, department_names, department_ids, category_names, category_ids, contact.name AS contact_name, contact.email AS contact_email");
    }

    public Event getEvent(int id) {
        return getOne(query(allData()).with("e.id", id));
    }

    public int getCount(String search) {
        return getCount(query(count()).search(searchParser.parse(search)));
    }

    public List<Event> getEvents(String search, String sortField, int start, int count) {
        QueryBuilder query = query(allData()).search(searchParser.parse(search))
                .page(sortField, start, count);
        return get(query);
    }

    public List<Event> getEventsById(List<Integer> ids) {
        QueryBuilder query = query(allData()).withAny("e.id", ids);
        return get(query);
    }

    public List<Event> getUpcomingMinistryEvents(int ministryId, int count) {
        QueryBuilder query = query(allData()).where("start_time >= now()")
                .with("ministry_id", ministryId)
                .page("start_time", 0, count);
        return get(query);
    }

    public List<Event> getUpcomingRecurringEvents(int recurrenceId, ZonedDateTime start) {
        QueryBuilder query = query(allData()).where("start_time >= ?", start)
                .with("recurring_meeting_id", recurrenceId)
                .sort("start_time");
        return get(query);
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

    public StreamingOutput getReportReader(String search, final List<String> fields) {
        final QueryBuilder query = query(allData()).search(searchParser.parse(search));

        return new ReportStreamingOutput(fields) {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try ( Connection conn = getConnection();
                      PreparedStatement stmt = query.prepareStatement(conn);
                      ResultSet rs = stmt.executeQuery()) {

                    writeCsv(output, rs);
                } catch (SQLException | IOException e) {
                    throw new RuntimeException("Could not retrieve events containing '" + search + "'", e);
                }
            }
        };
    }

    public Event create(Event event) {
        ZonedDateTime now = ZonedDateTime.now();
        event.setCreatedTime(now);
        event.setModifiedTime(now);

        InsertBuilder cmd = insertInto("events")
                .value("recurring_meeting_id", event.getRecurringMeetingId())
                .value("start_time", convert(event.getStartTime()))
                .value("end_time", convert(event.getEndTime()))
                .value("title", event.getTitle())
                .value("description", event.getDescription())
                .value("private_event", event.isPrivateEvent())
                .value("scheduler_id ", event.getSchedulerId())
                .value("contact_id", event.getContactId() > 0 ? event.getContactId() : null)
                .value("ministry_id", event.getMinistryId() > 0 ? event.getMinistryId() : null)
                .value("attendees", event.getAttendees())
                .value("created_time", event.getCreatedTime())
                .value("modified_time", event.getModifiedTime())
                .value("org_id", OrganizationContext.orgId());
        event.setId(createAndReturnKey(cmd));

        processEventCrosslinks(event);
        return event;
    }

    public Event updateEvent(Event event) {
        event.setModifiedTime(ZonedDateTime.now());
        event.incrementSequenceNumber();
        UpdateBuilder cmd = update("events")
                .value("recurring_meeting_id", event.getRecurringMeetingId())
                .value("start_time", convert(event.getStartTime()))
                .value("end_time", convert(event.getEndTime()))
                .value("title", event.getTitle())
                .value("description", event.getDescription())
                .value("private_event", event.isPrivateEvent())
                .value("scheduler_id ", event.getSchedulerId())
                .value("contact_id", event.getContactId() > 0 ? event.getContactId() : null)
                .value("ministry_id", event.getMinistryId() > 0 ? event.getMinistryId() : null)
                .value("attendees", event.getAttendees())
                .value("modified_time", event.getModifiedTime())
                .value("sequence_number", event.getSequenceNumber())
                .withId(event.getId()).inOrg();

        if (!update(cmd))
            throw new RuntimeException("Could not update event: " + event.getDescription());

        processEventCrosslinks(event);

        return event;
    }

    public boolean deleteEvent(int id) {
        return delete(deleteFrom("events").withId(id).inOrg());
    }

    // ----- Private -----
    @Override
    protected Event processRow(ResultSet rs) throws SQLException {
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
        e.setContactName(rs.getString("contact_name"));
        e.setContactEmail(rs.getString("contact_email"));
        e.setMinistryId(rs.getInt("ministry_id"));
        e.setMinistryName(rs.getString("ministry_name"));
        e.setAttendees(rs.getInt("attendees"));
        e.setCreatedTime(convert(rs.getTimestamp("created_time")));
        e.setModifiedTime(convert(rs.getTimestamp("modified_time")));
        e.setSequenceNumber(rs.getInt("sequence_number"));
        e.setDepartments(parseStringList(rs.getArray("department_names")));
        e.setDepartmentIds(parseIntList(rs.getArray("department_ids")));
        e.setCategories(parseStringList(rs.getArray("category_names")));
        e.setCategoryIds(parseIntList(rs.getArray("category_ids")));
        return e;
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

    private void processEventCrosslinks(Event event) {
        try (Connection conn = getConnection()) {
            crossReferenceDepartments(conn, event.getId(), event.getDepartmentIds());
            crossReferenceCategories(conn, event.getId(), event.getCategoryIds());
        } catch (SQLException e) {
            throw new RuntimeException("Could not crosslink event: " + event.getDescription(), e);
        }
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
