package org.servantscode.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Event {
    private int id;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Date startTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Date endTime;
    private String description;
    private int schedulerId;
    private String ministryName;
    private int ministryId;

    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getSchedulerId() { return schedulerId; }
    public void setSchedulerId(int schedulerId) { this.schedulerId = schedulerId; }

    public String getMinistryName() { return ministryName; }
    public void setMinistryName(String ministryName) { this.ministryName = ministryName; }

    public int getMinistryId() { return ministryId; }
    public void setMinistryId(int ministryId) { this.ministryId = ministryId; }

    public static void main(String[] args) {
        String time = "2019-01-01T06:00:00.000Z";
        DateTimeFormatter parser2 = DateTimeFormatter.ISO_DATE_TIME;
        System.out.println("Parsed" + parser2.parse(time).toString());
    }
}