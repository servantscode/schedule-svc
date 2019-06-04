package org.servantscode.schedule.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.AutoCompleteComparator;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.schedule.Room;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

@SuppressWarnings("SqlNoDataSourceInspection")
public class RoomDB extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(RoomDB.class);

    private SearchParser<Room> searchParser;

    public RoomDB() {
        this.searchParser = new SearchParser<>(Room.class, "name");
    }

    public int getCount(String search) {
        QueryBuilder query = count().from("rooms").search(searchParser.parse(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve people count '" + search + "'", e);
        }
        return 0;
    }

    public Room getRoom(int id) {
        QueryBuilder query = selectAll().from("rooms").withId(id);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
        ) {
            List<Room> rooms = processResults(stmt);
            return firstOrNull(rooms);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve room: " + id, e);
        }
    }

    public List<Room> getRooms(String search, String sortField, int start, int count) {
        QueryBuilder query = selectAll().from("rooms").search(searchParser.parse(search))
                .sort(sortField).limit(count).offset(start);
        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn)
        ) {
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
}
