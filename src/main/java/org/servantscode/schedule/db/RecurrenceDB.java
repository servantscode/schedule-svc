package org.servantscode.schedule.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.db.EasyDB;
import org.servantscode.commons.search.InsertBuilder;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.UpdateBuilder;
import org.servantscode.schedule.Recurrence;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.servantscode.commons.StringUtils.isEmpty;

public class RecurrenceDB extends EasyDB<Recurrence> {
    private static final Logger LOG = LogManager.getLogger(RecurrenceDB.class);

    public RecurrenceDB() {
        super(Recurrence.class, "id");
    }

    public Recurrence getRecurrence(int id) {
        return getOne(selectAll().from("recurrences").withId(id));
    }

    public List<Recurrence> getEventRecurrences(String search) {
        QueryBuilder query = selectAll().from("recurrences")
                .whereIdIn("id", select("DISTINCT e.recurring_meeting_id").from("events e")
                        .join("LEFT JOIN (SELECT array_agg(d.id) AS department_ids, array_agg(d.name) AS department_names, event_id FROM departments d, event_departments ed WHERE d.id=ed.department_id GROUP BY event_id) depts ON depts.event_id=e.id")
                        .join("LEFT JOIN (SELECT array_agg(c.id) AS category_ids, array_agg(c.name) AS category_names, event_id FROM categories c, event_categories cd WHERE c.id=cd.category_id GROUP BY event_id) cats ON cats.event_id=e.id")
                        .join("LEFT JOIN ministries m ON ministry_id=m.id").inOrg("e.org_id").search(EventDB.parseSearch(search)));
        return get(query);
    }


    public Recurrence create(Recurrence recurrence) {
        InsertBuilder cmd = insertInto("recurrences")
                .value("cycle", recurrence.getCycle())
                .value("frequency", recurrence.getFrequency())
                .value("end_date", convert(recurrence.getEndDate()))
                .value("weekly_days", encodeDays(recurrence.getWeeklyDays()))
                .value("excluded_days", encodeDateList(recurrence.getExceptionDates()));
        recurrence.setId(createAndReturnKey(cmd));
        return recurrence;
    }

    public Recurrence update(Recurrence recurrence) {
        UpdateBuilder cmd = update("recurrences")
                .value("cycle", recurrence.getCycle())
                .value("frequency", recurrence.getFrequency())
                .value("end_date", convert(recurrence.getEndDate()))
                .value("weekly_days", encodeDays(recurrence.getWeeklyDays()))
                .value("excluded_days", encodeDateList(recurrence.getExceptionDates()))
                .withId(recurrence.getId());
        if (!update(cmd))
            throw new RuntimeException("Could not update " + recurrence.getCycle() + " recurrence.");
        return recurrence;
    }

    public boolean delete(int id) {
        return delete(deleteFrom("recurrences").withId(id));
    }

    public boolean trimEndDate(Recurrence r) {
        QueryBuilder query = select("max(start_time)").from("events").with("recurring_meeting_id", r.getId());
        try (Connection conn = getConnection();
            PreparedStatement stmt = query.prepareStatement(conn);
            ResultSet rs = stmt.executeQuery()
        ) {

            LocalDate ts = rs.next()? convert(rs.getDate(1)): null;

            if(ts != null) {
                r.setEndDate(ts);
                update(r);
                return false;
            } else {
                delete(r.getId());
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not trim recurrence: " + r.getId(), e);
        }
    }

    // ----- Private -----
    @Override
    protected Recurrence processRow(ResultSet rs) throws SQLException {
        Recurrence r = new Recurrence();
        r.setId(rs.getInt("id"));
        r.setCycle(Recurrence.RecurrenceCycle.valueOf(rs.getString("cycle")));
        r.setFrequency(rs.getInt("frequency"));
        r.setEndDate(convert(rs.getDate("end_date")));
        r.setWeeklyDays(decodeDays(rs.getInt("weekly_days")));
        r.setExceptionDates(decodeDateList(rs.getString("excluded_days")));
        return r;
    }

    private static int encodeDays(List<DayOfWeek> days) {
        int result = 0;
        if(days == null || days.isEmpty())
            return 0;

        for(DayOfWeek day: days)
            result += 1 << day.getValue()-1;

        return result;
    }

    private static List<DayOfWeek> decodeDays(int days) {
        if(days == 0)
            return emptyList();

        LinkedList<DayOfWeek> result = new LinkedList<>();
        for(int i=0; i<7; i++){
            if((days & 1 << i) != 0)
                result.add(DayOfWeek.of(i+1));
        }
        return result;
    }

    private static String encodeDateList(List<LocalDate> dates) {
        if(dates == null || dates.isEmpty())
            return "";

        return dates.stream().map(date->date.format(DateTimeFormatter.ISO_DATE)).collect(Collectors.joining("|"));
    }

    private static List<LocalDate> decodeDateList(String dateString) {
        if(isEmpty(dateString))
            return emptyList();

        String[] dates = dateString.split("\\|");
        return Arrays.stream(dates).map(LocalDate::parse).collect(Collectors.toList());
    }
}
