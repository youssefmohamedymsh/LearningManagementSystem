package io.github.youssefmohamedymsh.lms.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A student and their enrollments. The enrollment rules live here:
 * <ul>
 *   <li>no course twice</li>
 *   <li>at most {@value #MAX_COURSES} courses</li>
 *   <li>at least one course once enrolled (you can't drop your last course)</li>
 *   <li>replacing a course is all-or-nothing</li>
 * </ul>
 */
public final class Student {

    public static final int MAX_COURSES = 6;

    private final int id;
    private final String name;
    private final int grade;
    private final String email;
    private final String address;
    private final String region;
    private final String country;
    private final List<Course> courses = new ArrayList<>();

    public Student(int id, String name, int grade, String email, String address, String region, String country) {
        this.id = id;
        this.name = name;
        this.grade = grade;
        this.email = email;
        this.address = address;
        this.region = region;
        this.country = country;
    }

    public int id() { return id; }
    public String name() { return name; }
    public int grade() { return grade; }
    public String email() { return email; }
    public String address() { return address; }
    public String region() { return region; }
    public String country() { return country; }

    public List<Course> courses() {
        return Collections.unmodifiableList(courses);
    }

    public boolean isEnrolledIn(int courseId) {
        return courses.stream().anyMatch(c -> c.id() == courseId);
    }

    public void enroll(Course course) {
        checkCanAdd(course);
        courses.add(course);
    }

    public void unenroll(int courseId) {
        if (!isEnrolledIn(courseId)) {
            throw new EnrollmentException("The student isn't enrolled in course " + courseId);
        }
        if (courses.size() == 1) {
            throw new EnrollmentException("Student should be enrolled in at least one course");
        }
        courses.removeIf(c -> c.id() == courseId);
    }

    /**
     * Swaps one course for another. Every rule is checked before anything changes, so a refused
     * replacement leaves the student's courses exactly as they were.
     */
    public void replace(int currentCourseId, Course replacement) {
        if (!isEnrolledIn(currentCourseId)) {
            throw new EnrollmentException("The student isn't enrolled in course " + currentCourseId);
        }
        if (replacement.id() == currentCourseId) {
            throw new EnrollmentException("Choose a different course to replace it with");
        }
        if (isEnrolledIn(replacement.id())) {
            throw new EnrollmentException("Can't enroll in the same course twice");
        }
        int index = indexOf(currentCourseId);
        courses.set(index, replacement); // same count, so the max-courses rule still holds
    }

    private void checkCanAdd(Course course) {
        if (isEnrolledIn(course.id())) {
            throw new EnrollmentException("Can't enroll in the same course twice");
        }
        if (courses.size() >= MAX_COURSES) {
            throw new EnrollmentException("Can't enroll in more than six courses");
        }
    }

    private int indexOf(int courseId) {
        for (int i = 0; i < courses.size(); i++) {
            if (courses.get(i).id() == courseId) {
                return i;
            }
        }
        return -1;
    }

    public String toRow() {
        return String.format("%-4d %-20s %-6d %-38s %s, %s", id, name, grade, email, region, country);
    }
}
