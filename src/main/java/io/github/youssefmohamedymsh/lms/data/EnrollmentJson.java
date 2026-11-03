package io.github.youssefmohamedymsh.lms.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.youssefmohamedymsh.lms.model.Course;
import io.github.youssefmohamedymsh.lms.model.Student;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Enrollments as JSON, keyed by the student's real id: {"1": [3, 7], "2": []}. */
public final class EnrollmentJson {

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private EnrollmentJson() {
    }

    public static String of(List<Student> students) {
        Map<String, List<Integer>> byStudent = new LinkedHashMap<>();
        for (Student s : students) {
            byStudent.put(String.valueOf(s.id()), s.courses().stream().map(Course::id).toList());
        }
        try {
            return JSON.writeValueAsString(byStudent);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
