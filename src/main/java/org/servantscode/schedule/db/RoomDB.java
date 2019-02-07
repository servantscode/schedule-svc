package org.servantscode.schedule.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.AutoCompleteComparator;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.schedule.Room;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

@SuppressWarnings("SqlNoDataSourceInspection")
public class RoomDB extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(RoomDB.class);

    public int getCount(String search) {
        String sql = format("Select count(1) from rooms%s", optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve people count '" + search + "'", e);
        }
        return 0;
    }

    public List<String> getRoomNames(String search, int count) {
        String sql = format("SELECT name FROM rooms%s", optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<String> names = new ArrayList<>();

            while (rs.next())
                names.add(rs.getString(1));

            long start = System.currentTimeMillis();
            names.sort(new AutoCompleteComparator(search));
            LOG.debug(String.format("Sorted %d names in %d ms.", names.size(), System.currentTimeMillis()-start));

            return (count < names.size())? names.subList(0, count): names;
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve names containing '" + search + "'", e);
        }
    }

    public Room getRoom(int id) {
        String sql = "SELECT * FROM rooms WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
        ) {

            stmt.setInt(1, id);

            List<Room> rooms = processResults(stmt);
            if(rooms.isEmpty())
                return null;

            return rooms.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve room: " + id, e);
        }
    }

    public List<Room> getRooms(String search, String sortField, int start, int count) {
        String sql = format("SELECT * FROM rooms%s ORDER BY %s LIMIT ? OFFSET ?", optionalWhereClause(search), sortField);
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, count);
            stmt.setInt(2, start);

            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve rooms.", e);
        }
    }

    public Room create(Room room) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO rooms(name, type, capacity) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){

            stmt.setString(1, room.getName());
            stmt.setString(2, room.getType().toString());
            stmt.setInt(3, room.getCapacity());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create room: " + room.getName());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    room.setId(rs.getInt(1));
            }
            return room;
        } catch (SQLException e) {
            throw new RuntimeException("Could not add room: " + room.getName(), e);
        }
    }

    public Room updateRoom(Room room) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE rooms SET name=?, type=?, capacity=? WHERE id=?")
        ) {

            stmt.setString(1, room.getName());
            stmt.setString(2, room.getType().toString());
            stmt.setInt(3, room.getCapacity());
            stmt.setInt(4, room.getId());

            if (stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update room: " + room.getName());

            return room;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update room: " + room.getName(), e);
        }
    }

    public boolean deleteRoom(int id) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM rooms WHERE id=?")
        ) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete room: " + id, e);
        }
    }

    // ----- Private -----
    private List<Room> processResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (rs.next()) {
                Room r = new Room();
                r.setId(rs.getInt("id"));
                r.setName(rs.getString("name"));
                r.setType(Room.RoomType.valueOf(rs.getString("type")));
                r.setCapacity(rs.getInt("capacity"));
                rooms.add(r);
            }
            return rooms;
        }
    }

    private String optionalWhereClause(String search) {
        return !isEmpty(search) ? format(" WHERE name ILIKE '%%%s%%'", search.replace("'", "''")) : "";
    }
}
