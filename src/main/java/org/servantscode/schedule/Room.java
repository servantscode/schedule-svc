package org.servantscode.schedule;

public class Room {
    public enum RoomType { SANCTUARY, MEETING, OFFICE, CLASS, KITCHEN, OTHER };

    private int id;
    private String name;
    private RoomType type;
    private int capacity;

    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
}
