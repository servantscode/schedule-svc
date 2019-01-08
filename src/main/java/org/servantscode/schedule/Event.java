package org.servantscode.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class Event {
    private int id;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date eventDate;
    private String description;
    private int schedulerId;
    private int ministryId;

    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Date getEventDate() { return eventDate; }
    public void setEventDate(Date eventDate) { this.eventDate = eventDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getSchedulerId() { return schedulerId; }
    public void setSchedulerId(int schedulerId) { this.schedulerId = schedulerId; }

    public int getMinistryId() { return ministryId; }
    public void setMinistryId(int ministryId) { this.ministryId = ministryId; }
}