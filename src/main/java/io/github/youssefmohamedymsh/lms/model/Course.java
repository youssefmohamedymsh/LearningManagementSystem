package io.github.youssefmohamedymsh.lms.model;

/** A course students can enroll in. Immutable. */
public record Course(int id, String name, String instructor, String duration, String time, String location) {

    /** One line for the course catalogue table. */
    public String toRow() {
        return String.format("%-3d %-32s %-24s %-9s %-5s %s", id, name, instructor, duration, time, location);
    }
}
