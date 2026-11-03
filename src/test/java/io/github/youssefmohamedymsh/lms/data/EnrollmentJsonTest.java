package io.github.youssefmohamedymsh.lms.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.youssefmohamedymsh.lms.model.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.github.youssefmohamedymsh.lms.TestData.course;
import static io.github.youssefmohamedymsh.lms.TestData.student;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EnrollmentJsonTest {

    @Test
    @DisplayName("Regression: enrollments are keyed by the real student id, not id - 1")
    void keyedByRealStudentId() throws Exception {
        Student first = student(1);
        Student second = student(2);
        second.enroll(course(3));
        second.enroll(course(7));

        Map<String, List<Integer>> json = new ObjectMapper().readValue(
                EnrollmentJson.of(List.of(first, second)), new TypeReference<>() {
                });

        assertEquals(Map.of("1", List.of(), "2", List.of(3, 7)), json);
    }
}
