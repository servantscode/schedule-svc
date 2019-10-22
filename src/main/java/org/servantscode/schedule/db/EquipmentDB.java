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
import org.servantscode.schedule.Equipment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

@SuppressWarnings("SqlNoDataSourceInspection")
public class EquipmentDB extends EasyDB<Equipment> {
    public EquipmentDB() {
        super(Equipment.class, "name");
    }

    public int getCount(String search) {
        return getCount(count().from("equipment").search(searchParser.parse(search)).inOrg());
    }

    public Equipment getEquipment(int id) {
        return getOne(selectAll().from("equipment").withId(id).inOrg());
    }

    public List<Equipment> getEquipmentList(String search, String sortField, int start, int count) {
        QueryBuilder query = selectAll().from("equipment").search(searchParser.parse(search)).inOrg()
                .page(sortField, start, count);
        return get(query);
    }

    public Equipment create(Equipment equipment) {
        InsertBuilder cmd = insertInto("equipment")
                .value("name", equipment.getName())
                .value("manufacturer", equipment.getManufacturer())
                .value("description", equipment.getDescription())
                .value("org_id", OrganizationContext.orgId());
        equipment.setId(createAndReturnKey(cmd));
        return equipment;
    }

    public Equipment updateEquipment(Equipment equipment) {
        UpdateBuilder cmd = update("equipment")
                .value("name", equipment.getName())
                .value("manufacturer", equipment.getManufacturer())
                .value("description", equipment.getDescription())
                .withId(equipment.getId()).inOrg();

        if(!update(cmd))
            throw new RuntimeException("Could not update equipment: " + equipment.getName());
        return equipment;
    }

    public boolean deleteEquipment(int id) {
        return delete(deleteFrom("equipment").withId(id).inOrg());
    }

    // ----- Private -----
    @Override
    protected Equipment processRow(ResultSet rs) throws SQLException {
        Equipment e = new Equipment();
        e.setId(rs.getInt("id"));
        e.setName(rs.getString("name"));
        e.setManufacturer(rs.getString("manufacturer"));
        e.setDescription(rs.getString("description"));
        return e;
    }
}
