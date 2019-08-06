package org.servantscode.schedule.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.AbstractDBUpgrade;

import java.sql.SQLException;

public class DBUpgrade extends AbstractDBUpgrade {
    private static final Logger LOG = LogManager.getLogger(DBUpgrade.class);

    @Override
    public void doUpgrade() throws SQLException {
        LOG.info("Verifying database structures.");

        if(!tableExists("events")) {
            LOG.info("-- Creating events table");
            runSql("CREATE TABLE events (id SERIAL PRIMARY KEY, " +
                                        "start_time TIMESTAMP WITH TIME ZONE, " +
                                        "recurring_meeting_id INTEGER, " +
                                        "end_time TIMESTAMP WITH TIME ZONE, " +
                                        "title TEXT, " +
                                        "description TEXT, " +
                                        "private_event BOOLEAN, " +
                                        "scheduler_id INTEGER REFERENCES people(id) ON DELETE SET NULL, " +
                                        "contact_id INTEGER REFERENCES people(id) ON DELETE SET NULL, " +
                                        "ministry_id INTEGER REFERENCES ministries(id) ON DELETE CASCADE, " +
                                        "departments TEXT, " +
                                        "categories TEXT, " +
                                        "org_id INTEGER references organizations(id) ON DELETE CASCADE)");
        }

        if(!tableExists("rooms")) {
            LOG.info("-- Creating rooms table");
            runSql("CREATE TABLE rooms (id SERIAL PRIMARY KEY, " +
                                       "name TEXT, " +
                                       "type TEXT, " +
                                       "capacity INTEGER, " +
                                       "org_id INTEGER references organizations(id) ON DELETE CASCADE)");
        }

        if(!tableExists("equipment")) {
            LOG.info("-- Creating equipment table");
            runSql("CREATE TABLE equipment (id SERIAL PRIMARY KEY, " +
                                           "name TEXT, " +
                                           "manufacturer TEXT, " +
                                           "description TEXT, " +
                                           "org_id INTEGER references organizations(id) ON DELETE CASCADE)");
        }

        if(!tableExists("reservations")) {
            LOG.info("-- Creating reservations table");
            runSql("CREATE TABLE reservations (id BIGSERIAL PRIMARY KEY, " +
                                              "resource_type TEXT, " +
                                              "resource_id INTEGER, " +
                                              "reserving_person_id INTEGER REFERENCES people(id) ON DELETE SET NULL, " +
                                              "event_id INTEGER, " +
                                              "start_time TIMESTAMP WITH TIME ZONE, " +
                                              "end_time TIMESTAMP WITH TIME ZONE)");
        }

        if(!tableExists("recurrences")) {
            LOG.info("-- Creating recurrences table");
            runSql("CREATE TABLE recurrences(id SERIAL PRIMARY KEY, " +
                                            "cycle TEXT, " +
                                            "frequency INTEGER, " +
                                            "end_date DATE, " +
                                            "weekly_days INTEGER, " +
                                            "excluded_days TEXT)");
        }
    }
}
