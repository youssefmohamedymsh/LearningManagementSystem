package io.github.youssefmohamedymsh.lms.cli;

import io.github.youssefmohamedymsh.lms.TestData;
import io.github.youssefmohamedymsh.lms.service.EnrollmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Drives the menu with scripted keyboard input, the way a user would. */
class MainTest {

    @TempDir
    Path outputDir;

    private String run(String... lines) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        EnrollmentService service = new EnrollmentService(TestData.realStudents(), TestData.realCourses());
        new Main(service, new ByteArrayInputStream((String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8)),
                new PrintStream(bytes, true, StandardCharsets.UTF_8), outputDir).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Regression: any valid student id can be selected, not only the first")
    void anyValidStudentCanBeSelected() {
        // v1 threw "Please enter a Valid student ID" for every student except #1.
        String out = run("42", "b", "q");
        assertTrue(out.contains("Name: Deborah Weber"), out);
        assertFalse(out.contains("Please enter a valid student ID"), out);
    }

    @Test
    void invalidStudentIdsAreRefused() {
        String out = run("abc", "0", "101", "q");
        assertTrue(out.split("Please enter a valid student ID", -1).length - 1 == 3, out);
    }

    @Test
    void enrollReplaceAndDropAreSavedToJson() throws Exception {
        String out = run("2", "a", "1", "a", "2", "r", "1", "5", "d", "2", "b", "q");
        assertTrue(out.contains("Enrolled successfully in Algorithms"), out);
        assertTrue(out.contains("Replaced Algorithms with Computer Networking."), out);
        assertTrue(out.contains("Unenrolled successfully from Number Theory."), out);

        String json = Files.readString(outputDir.resolve("enrollments.json"));
        assertTrue(json.replaceAll("\\s", "").contains("\"2\":[5]"), json);
    }

    @Test
    void ruleViolationsAreShownAndTheMenuContinues() {
        String out = run("3", "a", "1", "a", "1", "d", "1", "b", "q");
        assertTrue(out.contains("Can't enroll in the same course twice"), out);
        assertTrue(out.contains("Student should be enrolled in at least one course"), out);
        assertTrue(out.trim().endsWith("Goodbye."), out);
    }

    @Test
    void writesCsvExportsOnStart() throws Exception {
        run("q");
        assertTrue(Files.readString(outputDir.resolve("courses.csv")).contains("\"Scott, Austin C.\""));
        assertTrue(Files.exists(outputDir.resolve("students.csv")));
    }

    @Test
    void endOfInputExitsCleanly() {
        String out = run("1", "a");
        assertTrue(out.trim().endsWith("Goodbye."), out);
    }
}
