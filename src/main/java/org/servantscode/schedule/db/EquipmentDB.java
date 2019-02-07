package org.servantscode.schedule.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.AutoCompleteComparator;
import org.servantscode.commons.db.DBAccess;
import org.servantscode.schedule.Equipment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.servantscode.commons.StringUtils.isEmpty;

@SuppressWarnings("SqlNoDataSourceInspection")
public class EquipmentDB extends DBAccess {
    private static final Logger LOG = LogManager.getLogger(EquipmentDB.class);

    public int getCount(String search) {
        String sql = format("Select count(1) from equipment%s", optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve equipment count '" + search + "'", e);
        }
        return 0;
    }

    public List<String> getEquipmentNames(String search, int count) {
        String sql = format("SELECT name FROM equipment%s", optionalWhereClause(search));
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<String> names = new ArrayList<>();

            while (rs.next())
                names.add(rs.getString(1));

            names.sort(new AutoCompleteComparator(search));

            return (count < names.size())? names.subList(0, count): names;
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve names containing '" + search + "'", e);
        }
    }

    public Equipment getEquipment(int id) {
        String sql = "SELECT * FROM equipment WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
        ) {

            stmt.setInt(1, id);

            List<Equipment> equipment = processResults(stmt);
            if(equipment.isEmpty())
                return null;

            return equipment.get(0);
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve equipment: " + id, e);
        }
    }

    public List<Equipment> getEquipmentList(String search, String sortField, int start, int count) {
        String sql = format("SELECT * FROM equipment%s ORDER BY %s LIMIT ? OFFSET ?", optionalWhereClause(search), sortField);
        try ( Connection conn = getConnection();
              PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, count);
            stmt.setInt(2, start);

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
