package org.servantscode.schedule.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.schedule.Recurrence;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.emptyList;

public class RecurrenceDB extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(RecurrenceDB.class);

    public Recurrence getRecurrence(int id) {
        QueryBuilder query = selectAll().from("recurrences").withId(id);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
        ) {

            List<Recurrence> recurrences = processResults(stmt);
            if(recurrences.isEmpty())
                return null;

            return recurrences.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve recurrence: " + id, e);
        }
    }

    public List<Recurrence> getEventRecurrences(String search) {
        QueryBuilder query = selectAll().from("recurrences")
                .whereIdIn("id", select("DISTINCT e.recurring_meeting_id").from("events e").search(EventDB.parseSearch(search)));
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn)
        ) {

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve event recurrences.", e);
        }
    }


    public Recurrence create(Recurrence recurrence) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO recurrences(cycle, frequency, end_date, weekly_days) values (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){

            stmt.setString(1, recurrence.getCycle().toString());
            stmt.setInt(2, recurrence.getFrequency());
            stmt.setTimestamp(3, convert(recurrence.getEndDate()));
            stmt.setInt(4, encodeDays(recurrence.getWeeklyDays()));

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create " + recurrence.getCycle() + " recurrence.");
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    recurrence.setId(rs.getInt(1));
            }
            return recurrence;
        } catch (SQLException e) {
            throw new RuntimeException("Could not create " + recurrence.getCycle() + " recurrence.", e);
        }
    }

    public Recurrence update(Recurrence recurrence) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE recurrences SET cycle=?, frequency=?, end_date=?, weekly_days=? WHERE id=?")
        ) {

            stmt.setString(1, recurrence.getCycle().toString());
            stmt.setInt(2, recurrence.getFrequency());
            stmt.setTimestamp(3, convert(recurrence.getEndDate()));
            stmt.setInt(4, encodeDays(recurrence.getWeeklyDays()));
            stmt.setInt(5, recurrence.getId());

            if (stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update " + recurrence.getCycle() + " recurrence.");

            return recurrence;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update " + recurrence.getCycle() + " recurrence.", e);
        }
    }

    public boolean delete(int id) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM recurrences WHERE id=?")
        ) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete recurrence: " + id, e);
        }
    }

    public boolean trimEndDate(Recurrence r) {
        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement("select max(start_time) from events where recurring_meeting_id=?")
        ) {

            stmt.setInt(1, r.getId());

            try (ResultSet rs = stmt.executeQuery()) {
                ZonedDateTime ts = rs.next()? convert(rs.getTimestamp(1)): null;

                if(ts != null) {
                    r.setEndDate(ts);
                    update(r);
                    return false;
                } else {
                    delete(r.getId());
                    return true;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not trim recurrence: " + r.getId(), e);
        }
    }

    // ----- Private -----
    private List<Recurrence> processResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            List<Recurrence> recurrences = new ArrayList<>();
            while (rs.next()) {
                Recurrence r = new Recurrence();
                r.setId(rs.getInt("id"));
                r.setCycle(Recurrence.RecurrenceCycle.valueOf(rs.getString("cycle")));
                r.setFrequency(rs.getInt("frequency"));
                r.setEndDate(convert(rs.getTimestamp("end_date")));
                r.setWeeklyDays(decodeDays(rs.getInt("weekly_days")));
                recurrences.add(r);
            }
            return recurrences;
        }
    }

    private static int encodeDays(List<DayOfWeek> days) {
        int result = 0;
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
}
