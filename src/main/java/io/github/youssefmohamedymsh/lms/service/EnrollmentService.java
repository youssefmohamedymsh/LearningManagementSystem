package io.github.youssefmohamedymsh.lms.service;

import io.github.youssefmohamedymsh.lms.model.Course;
import io.github.youssefmohamedymsh.lms.model.EnrollmentException;
import io.github.youssefmohamedymsh.lms.model.Student;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Looks students and courses up by their real ids and applies enrollment changes. */
public final class EnrollmentService {

    private final Map<Integer, Student> students;
    private final Map<Integer, Course> courses;
    private final List<Student> studentsInOrder;
    private final List<Course> coursesInOrder;

    public EnrollmentService(List<Student> students, List<Course> courses) {
        this.studentsInOrder = List.copyOf(students);
        this.coursesInOrder = List.copyOf(courses);
        this.students = students.stream().collect(Collectors.toMap(Student::id, Function.identity()));
        this.courses = courses.stream().collect(Collectors.toMap(Course::id, Function.identity()));
    }

    public List<Student> students() {
        return studentsInOrder;
    }

    public List<Course> courses() {
        return coursesInOrder;
    }

    public Student student(int id) {
        Student s = students.get(id);
        if (s == null) {
            throw new EnrollmentException("There's no student with id " + id);
        }
        return s;
    }

    public Course course(int id) {
        Course c = courses.get(id);
        if (c == null) {
            throw new EnrollmentException("There's no course with id " + id);
        }
        return c;
    }

    public void enroll(int studentId, int courseId) {
        student(studentId).enroll(course(courseId));
    }

    public void unenroll(int studentId, int courseId) {
        student(studentId).unenroll(courseId);
    }

    public void replace(int studentId, int currentCourseId, int newCourseId) {
        Student s = student(studentId);
        s.replace(currentCourseId, course(newCourseId));
    }
}
