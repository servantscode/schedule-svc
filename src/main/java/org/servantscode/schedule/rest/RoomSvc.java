package org.servantscode.schedule.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.EnumUtils;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.schedule.Room;
import org.servantscode.schedule.db.RoomDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/room")
public class RoomSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(RoomSvc.class);

    private RoomDB db;

    public RoomSvc() {
        db = new RoomDB();
    }

    @GET @Path("/autocomplete") @Produces(MediaType.APPLICATION_JSON)
    public List<String> getRoomNames(@QueryParam("start") @DefaultValue("0") int start,
                                     @QueryParam("count") @DefaultValue("100") int count,
                                     @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                     @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        verifyUserAccess("room.list");
        try {
            LOG.trace(String.format("Retrieving rooms names (%s, %s, page: %d; %d)", nameSearch, sortField, start, count));
            return db.getRoomNames(nameSearch, count);
        } catch (Throwable t) {
            LOG.error("Retrieving rooms failed:", t);
            throw t;
        }
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<Room> getRooms(@QueryParam("start") @DefaultValue("0") int start,
                                            @QueryParam("count") @DefaultValue("10") int count,
                                            @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                            @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        verifyUserAccess("room.list");
        try {
            int totalPeople = db.getCount(nameSearch);

            List<Room> results = db.getRooms(nameSearch, sortField, start, count);

            return new PaginatedResponse<>(start, results.size(), totalPeople, results);
        } catch (Throwable t) {
            LOG.error("Retrieving rooms failed:", t);
            throw t;
        }
    }

    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Room getRoom(@PathParam("id") int id) {
        verifyUserAccess("room.read");
        try {
            return db.getRoom(id);
        } catch (Throwable t) {
            LOG.error("Retrieving room failed:", t);
            throw t;
        }
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Room createRoom(Room room) {
        verifyUserAccess("room.create");
        try {
            db.create(room);
            LOG.info("Created room: " + room.getName());
            return room;
        } catch (Throwable t) {
            LOG.error("Creating room failed:", t);
            throw t;
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Room updateRoom(Room room) {
        verifyUserAccess("room.update");
        try {
            db.updateRoom(room);
            LOG.info("Edited room: " + room.getName());
            return room;
        } catch (Throwable t) {
            LOG.error("Updating room failed:", t);
            throw t;
        }
    }

    @DELETE @Path("/{id}")
    public void deleteRoom(@PathParam("id") int id) {
        verifyUserAccess("room.delete");
        if(id <= 0)
            throw new NotFoundException();
        try {
            Room room = db.getRoom(id);
            if(room == null || !db.deleteRoom(id))
                throw new NotFoundException();
            LOG.info("Deleted room: " + room.getName());
        } catch (Throwable t) {
            LOG.error("Deleting room failed:", t);
            throw t;
        }
    }

    @GET @Path("/types") @Produces(APPLICATION_JSON)
    public List<String> getRoomTypes() {
        return EnumUtils.listValues(Room.RoomType.class);
    }

}
