package io.github.youssefmohamedymsh.lms.data;

import io.github.youssefmohamedymsh.lms.TestData;
import io.github.youssefmohamedymsh.lms.model.Course;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseCatalogueTest {

    @Test
    void loadsAllSeventeenCourses() {
        List<Course> courses = TestData.realCourses();
        assertEquals(17, courses.size());
        assertEquals(new Course(1, "Algorithms", "Scott, Austin C.", "8 weeks", "2H", "Hall1"), courses.get(0));
    }

    @Test
    void idsAreUnique() {
        List<Course> courses = TestData.realCourses();
        assertEquals(courses.size(), courses.stream().map(Course::id).distinct().count());
    }

    @Test
    void refusesARowWithAMissingField() {
        String xml = "<root><row><id>1</id><CourseName>Algorithms</CourseName></row></root>";
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> CourseCatalogue.fromXml(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
        assertTrue(e.getMessage().contains("<Instructor>"), e.getMessage());
    }

    @Test
    @DisplayName("Rejects XML with a DOCTYPE, so external entities can't be injected")
    void rejectsDoctype() {
        String xml = "<!DOCTYPE root [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><root><row><id>&x;</id></row></root>";
        assertThrows(IllegalStateException.class,
                () -> CourseCatalogue.fromXml(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
    }
}
