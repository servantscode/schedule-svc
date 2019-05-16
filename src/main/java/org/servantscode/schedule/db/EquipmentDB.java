package org.servantscode.schedule.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.AutoCompleteComparator;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.commons.search.SearchParser;
import org.servantscode.schedule.Equipment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

@SuppressWarnings("SqlNoDataSourceInspection")
public class EquipmentDB extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(EquipmentDB.class);

    private SearchParser<Equipment> searchParser;

    public EquipmentDB() {
        this.searchParser = new SearchParser<>(Equipment.class, "name");
    }

    public int getCount(String search) {
        QueryBuilder query = count().from("equipment").search(searchParser.parse(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve equipment count '" + search + "'", e);
        }
        return 0;
    }

    public Equipment getEquipment(int id) {
        QueryBuilder query = selectAll().from("equipment").where("id=?", id);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
        ) {

            List<Equipment> equipment = processResults(stmt);
            return firstOrNull(equipment);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve equipment: " + id, e);
        }
    }

    public List<Equipment> getEquipmentList(String search, String sortField, int start, int count) {
        QueryBuilder query = selectAll().from("equipment").search(searchParser.parse(search))
                .sort(sortField).limit(count).offset(start);
        try ( Connection conn = getConnection();
              PreparedStatement stmt = query.prepareStatement(conn)
        ) {
            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve equipment.", e);
        }
    }

    public Equipment create(Equipment equipment) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO equipment(name, manufacturer, description) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ){

            stmt.setString(1, equipment.getName());
            stmt.setString(2, equipment.getManufacturer());
            stmt.setString(3, equipment.getDescription());

            if(stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create equipment: " + equipment.getName());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next())
                    equipment.setId(rs.getInt(1));
            }
            return equipment;
        } catch (SQLException e) {
            throw new RuntimeException("Could not add equipment: " + equipment.getName(), e);
        }
    }

    public Equipment updateEquipment(Equipment equipment) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE equipment SET name=?, manufacturer=?, description=? WHERE id=?")
        ) {

            stmt.setString(1, equipment.getName());
            stmt.setString(2, equipment.getManufacturer());
            stmt.setString(3, equipment.getDescription());
            stmt.setInt(4, equipment.getId());

            if (stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update equipment: " + equipment.getName());

            return equipment;
        } catch (SQLException e) {
            throw new RuntimeException("Could not update equipment: " + equipment.getName(), e);
        }
    }

    public boolean deleteEquipment(int id) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM equipment WHERE id=?")
        ) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete equipment: " + id, e);
        }
    }

    // ----- Private -----
    private List<Equipment> processResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            List<Equipment> equipment = new ArrayList<>();
            while (rs.next()) {
                Equipment e = new Equipment();
                e.setId(rs.getInt("id"));
                e.setName(rs.getString("name"));
                e.setManufacturer(rs.getString("manufacturer"));
                e.setDescription(rs.getString("description"));
                equipment.add(e);
            }
            return equipment;
        }
    }

    private String optionalWhereClause(String search) {
        return !isEmpty(search) ? format(" WHERE name ILIKE '%%%s%%'", search.replace("'", "''")) : "";
    }
}
