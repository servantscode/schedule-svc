package org.servantscode.schedule.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.schedule.Equipment;
import org.servantscode.schedule.db.EquipmentDB;
import org.servantscode.schedule.db.EquipmentDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/equipment")
public class EquipmentSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(EquipmentSvc.class);

    private EquipmentDB db;

    public EquipmentSvc() {
        db = new EquipmentDB();
    }

    @GET @Path("/autocomplete") @Produces(MediaType.APPLICATION_JSON)
    public List<String> getEquipmentNames(@QueryParam("start") @DefaultValue("0") int start,
                                     @QueryParam("count") @DefaultValue("100") int count,
                                     @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                     @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        verifyUserAccess("equipment.list");
        try {
            LOG.trace(String.format("Retrieving equipments names (%s, %s, page: %d; %d)", nameSearch, sortField, start, count));
            return db.getEquipmentNames(nameSearch, count);
        } catch (Throwable t) {
            LOG.error("Retrieving equipments failed:", t);
            throw t;
        }
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<Equipment> getEquipment(@QueryParam("start") @DefaultValue("0") int start,
                                                     @QueryParam("count") @DefaultValue("10") int count,
                                                     @QueryParam("sort_field") @DefaultValue("id") String sortField,
                                                     @QueryParam("partial_name") @DefaultValue("") String nameSearch) {

        verifyUserAccess("equipment.list");
        try {
            int totalPeople = db.getCount(nameSearch);

            List<Equipment> results = db.getEquipmentList(nameSearch, sortField, start, count);

            return new PaginatedResponse<>(start, results.size(), totalPeople, results);
        } catch (Throwable t) {
            LOG.error("Retrieving equipment failed:", t);
            throw t;
        }
    }

    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Equipment getEquipment(@PathParam("id") int id) {
        verifyUserAccess("equipment.read");
        try {
            return db.getEquipment(id);
        } catch (Throwable t) {
            LOG.error("Retrieving equipment failed:", t);
            throw t;
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Equipment createEquipment(Equipment equipment) {
        verifyUserAccess("equipment.create");
        try {
            db.create(equipment);
            LOG.info("Created equipment: " + equipment.getName());
            return equipment;
        } catch (Throwable t) {
            LOG.error("Creating equipment failed:", t);
            throw t;
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Equipment updateEquipment(Equipment equipment) {
        verifyUserAccess("equipment.update");
        try {
            db.updateEquipment(equipment);
            LOG.info("Edited equipment: " + equipment.getName());
            return equipment;
        } catch (Throwable t) {
            LOG.error("Updating equipment failed:", t);
            throw t;
        }
    }

    @DELETE @Path("/{id}")
    public void deleteEquipment(@PathParam("id") int id) {
        verifyUserAccess("equipment.delete");
        if(id <= 0)
            throw new NotFoundException();
        try {
            Equipment equipment = db.getEquipment(id);
            if(equipment == null || db.deleteEquipment(id))
                throw new NotFoundException();
            LOG.info("Deleted equipment: " + equipment.getName());
        } catch (Throwable t) {
            LOG.error("Deleting equipment failed:", t);
            throw t;
        }
    }
}
