package io.github.youssefmohamedymsh.lms.data;

import io.github.youssefmohamedymsh.lms.TestData;
import io.github.youssefmohamedymsh.lms.model.Course;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvTest {

    @Test
    @DisplayName("Regression: instructor names with commas stay in one column")
    void instructorWithCommaStaysInOneColumn() {
        // v1 wrote "1,Algorithms,Scott, Austin C.,..." and read it back with split(","), shifting every column.
        String csv = Csv.courses(TestData.realCourses());
        String algorithms = csv.lines().skip(1).findFirst().orElseThrow();
        assertEquals("1,Algorithms,\"Scott, Austin C.\",8 weeks,2H,Hall1", algorithms);

        List<String> fields = Csv.parseLine(algorithms);
        assertEquals(6, fields.size());
        assertEquals("Scott, Austin C.", fields.get(2));
        assertEquals("Hall1", fields.get(5));
    }

    @Test
    void everyCourseRowRoundTripsToSixFields() {
        List<Course> courses = TestData.realCourses();
        List<String> rows = Csv.courses(courses).lines().skip(1).toList();
        assertEquals(courses.size(), rows.size());
        for (int i = 0; i < rows.size(); i++) {
            List<String> f = Csv.parseLine(rows.get(i));
            Course c = courses.get(i);
            assertEquals(List.of(String.valueOf(c.id()), c.name(), c.instructor(), c.duration(), c.time(), c.location()), f);
        }
    }

    @Test
    void escapesQuotes() {
        assertEquals("\"Say \"\"hi\"\"\"", Csv.escape("Say \"hi\""));
        assertEquals(List.of("Say \"hi\"", "x"), Csv.parseLine("\"Say \"\"hi\"\"\",x"));
    }

    @Test
    void studentsCsvHasAHeaderAndOneRowPerStudent() {
        List<String> lines = Csv.students(TestData.realStudents()).lines().toList();
        assertEquals("id,name,grade,email,address,region,country", lines.get(0));
        assertEquals(101, lines.size());
    }
}
