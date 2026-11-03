package io.github.youssefmohamedymsh.lms.service;

import io.github.youssefmohamedymsh.lms.TestData;
import io.github.youssefmohamedymsh.lms.model.Course;
import io.github.youssefmohamedymsh.lms.model.EnrollmentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnrollmentServiceTest {

    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        service = new EnrollmentService(TestData.realStudents(), TestData.realCourses());
    }

    @Test
    @DisplayName("Regression: student ids are looked up by id, not list position")
    void looksUpByIdNotIndex() {
        // v1 used list.get(id), so asking for student 5 returned student 6.
        service.enroll(5, 1);
        assertEquals("Mark Delgado", service.student(5).name());
        assertTrue(service.student(5).isEnrolledIn(1));
        assertTrue(service.student(6).courses().isEmpty());
    }

    @Test
    void firstAndLastStudentsAreReachable() {
        assertEquals("Malcolm Barnes", service.student(1).name());
        assertEquals("Hilel Hodges", service.student(100).name());
    }

    @Test
    void unknownStudentIsRefused() {
        EnrollmentException e = assertThrows(EnrollmentException.class, () -> service.enroll(101, 1));
        assertEquals("There's no student with id 101", e.getMessage());
    }

    @Test
    void unknownCourseIsRefused() {
        EnrollmentException e = assertThrows(EnrollmentException.class, () -> service.enroll(1, 18));
        assertEquals("There's no course with id 18", e.getMessage());
    }

    @Test
    void replacingWithAnUnknownCourseChangesNothing() {
        service.enroll(1, 1);
        service.enroll(1, 2);
        assertThrows(EnrollmentException.class, () -> service.replace(1, 1, 99));
        assertEquals(List.of(1, 2), service.student(1).courses().stream().map(Course::id).toList());
    }
}
