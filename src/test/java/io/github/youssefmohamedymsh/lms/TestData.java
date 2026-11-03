package io.github.youssefmohamedymsh.lms;

import io.github.youssefmohamedymsh.lms.data.CourseCatalogue;
import io.github.youssefmohamedymsh.lms.data.StudentRoster;
import io.github.youssefmohamedymsh.lms.model.Course;
import io.github.youssefmohamedymsh.lms.model.Student;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

public final class TestData {

    private TestData() {
    }

    public static List<Course> realCourses() {
        return CourseCatalogue.fromXml(TestData.class.getResourceAsStream("/data/courses.xml"));
    }

    public static List<Student> realStudents() {
        try (InputStream in = TestData.class.getResourceAsStream("/data/student-data.txt")) {
            return StudentRoster.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Course course(int id) {
        return new Course(id, "Course " + id, "Instructor, A.", "8 weeks", "2H", "Hall1");
    }

    /** Courses with ids 1..n. */
    public static List<Course> courses(int n) {
        return IntStream.rangeClosed(1, n).mapToObj(TestData::course).toList();
    }

    public static Student student(int id) {
        return new Student(id, "Student " + id, 5, "s" + id + "@example.com", "1 Main St", "Region", "Country");
    }
}
