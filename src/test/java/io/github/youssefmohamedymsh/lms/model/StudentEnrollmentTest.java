package io.github.youssefmohamedymsh.lms.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.youssefmohamedymsh.lms.TestData.course;
import static io.github.youssefmohamedymsh.lms.TestData.student;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Student enrollment rules")
class StudentEnrollmentTest {

    private Student student;

    @BeforeEach
    void setUp() {
        student = student(1);
    }

    private List<Integer> courseIds() {
        return student.courses().stream().map(Course::id).toList();
    }

    @Nested
    @DisplayName("Enrolling")
    class Enrolling {

        @Test
        void addsTheCourse() {
            student.enroll(course(3));
            assertEquals(List.of(3), courseIds());
        }

        @Test
        void refusesTheSameCourseTwice() {
            student.enroll(course(3));
            EnrollmentException e = assertThrows(EnrollmentException.class, () -> student.enroll(course(3)));
            assertEquals("Can't enroll in the same course twice", e.getMessage());
            assertEquals(List.of(3), courseIds());
        }

        @Test
        void allowsUpToSixCourses() {
            for (int id = 1; id <= Student.MAX_COURSES; id++) {
                student.enroll(course(id));
            }
            assertEquals(6, student.courses().size());
        }

        @Test
        void refusesASeventhCourse() {
            for (int id = 1; id <= 6; id++) {
                student.enroll(course(id));
            }
            EnrollmentException e = assertThrows(EnrollmentException.class, () -> student.enroll(course(7)));
            assertEquals("Can't enroll in more than six courses", e.getMessage());
            assertEquals(6, student.courses().size());
        }

        @Test
        void exposesCoursesReadOnly() {
            assertThrows(UnsupportedOperationException.class, () -> student.courses().add(course(1)));
        }
    }

    @Nested
    @DisplayName("Unenrolling")
    class Unenrolling {

        @Test
        void removesTheCourse() {
            student.enroll(course(1));
            student.enroll(course(2));
            student.unenroll(1);
            assertEquals(List.of(2), courseIds());
        }

        @Test
        void refusesToDropTheLastCourse() {
            student.enroll(course(1));
            EnrollmentException e = assertThrows(EnrollmentException.class, () -> student.unenroll(1));
            assertEquals("Student should be enrolled in at least one course", e.getMessage());
            assertEquals(List.of(1), courseIds());
        }

        @Test
        void refusesACourseTheStudentIsNotIn() {
            student.enroll(course(1));
            student.enroll(course(2));
            EnrollmentException e = assertThrows(EnrollmentException.class, () -> student.unenroll(9));
            assertEquals("The student isn't enrolled in course 9", e.getMessage());
            assertEquals(List.of(1, 2), courseIds());
        }
    }

    @Nested
    @DisplayName("Replacing")
    class Replacing {

        @BeforeEach
        void enrollInTwo() {
            student.enroll(course(1));
            student.enroll(course(2));
        }

        @Test
        void swapsInPlace() {
            student.replace(1, course(5));
            assertEquals(List.of(5, 2), courseIds());
        }

        @Test
        @DisplayName("Regression: a refused replacement keeps the original course")
        void refusedReplacementKeepsOriginal() {
            // v1 removed the old course first, then failed to add a duplicate, losing both.
            assertThrows(EnrollmentException.class, () -> student.replace(1, course(2)));
            assertEquals(List.of(1, 2), courseIds());
        }

        @Test
        void worksWhenTheStudentHasTheMaximum() {
            for (int id = 3; id <= 6; id++) {
                student.enroll(course(id));
            }
            student.replace(6, course(7));
            assertEquals(List.of(1, 2, 3, 4, 5, 7), courseIds());
        }

        @Test
        void refusesACourseTheStudentIsNotIn() {
            EnrollmentException e = assertThrows(EnrollmentException.class, () -> student.replace(9, course(5)));
            assertEquals("The student isn't enrolled in course 9", e.getMessage());
        }

        @Test
        void refusesReplacingACourseWithItself() {
            assertThrows(EnrollmentException.class, () -> student.replace(1, course(1)));
            assertEquals(List.of(1, 2), courseIds());
        }
    }
}
