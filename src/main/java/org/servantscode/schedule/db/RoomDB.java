package org.servantscode.schedule.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.AutoCompleteComparator;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.db.EasyDB;
import org.servantscode.commons.search.InsertBuilder;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.commons.search.UpdateBuilder;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.schedule.Room;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

@SuppressWarnings("SqlNoDataSourceInspection")
public class RoomDB extends EasyDB<Room> {

    public RoomDB() {
        super(Room.class, "name");
    }

    public int getCount(String search) {
        return getCount(count().from("rooms").search(searchParser.parse(search)).inOrg());
    }

    public Room getRoom(int id) {
        return getOne(selectAll().from("rooms").withId(id).inOrg());
    }

    public List<Room> getRooms(String search, String sortField, int start, int count) {
        QueryBuilder query = selectAll().from("rooms").search(searchParser.parse(search)).inOrg()
                .page(sortField, start, count);
        return get(query);
    }

    public Room create(Room room) {
        InsertBuilder cmd = insertInto("rooms")
                .value("name", room.getName())
                .value("type", room.getType())
                .value("capacity", room.getCapacity())
                .value("org_id", OrganizationContext.orgId());

        room.setId(createAndReturnKey(cmd));
        return room;
    }

    public Room updateRoom(Room room) {
        UpdateBuilder cmd = update("rooms")
                .value("name", room.getName())
                .value("type", room.getType())
                .value("capacity", room.getCapacity())
                .withId(room.getId()).inOrg();

        if(!update(cmd))
            throw new RuntimeException("Could not update room: " + room.getName());

        return room;
    }

    public boolean deleteRoom(int id) {
        return delete(deleteFrom("rooms").withId(id).inOrg());
    }

    // ----- Private -----
    @Override
    protected Room processRow(ResultSet rs) throws SQLException {
        Room r = new Room();
        r.setId(rs.getInt("id"));
        r.setName(rs.getString("name"));
        r.setType(Room.RoomType.valueOf(rs.getString("type")));
        r.setCapacity(rs.getInt("capacity"));
        return r;
    }
}
