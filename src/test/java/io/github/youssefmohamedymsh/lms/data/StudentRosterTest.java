package io.github.youssefmohamedymsh.lms.data;

import io.github.youssefmohamedymsh.lms.TestData;
import io.github.youssefmohamedymsh.lms.model.Student;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StudentRosterTest {

    @Test
    void loadsAllHundredStudentsWithoutTheHeader() {
        List<Student> students = TestData.realStudents();
        assertEquals(100, students.size());
        assertEquals(1, students.get(0).id());
        assertEquals(100, students.get(99).id());
    }

    @Test
    void readsEveryField() {
        Student first = TestData.realStudents().get(0);
        assertEquals("Malcolm Barnes", first.name());
        assertEquals(9, first.grade());
        assertEquals("velit.aliquam@outlook.couk", first.email());
        assertEquals("375-3721 Elementum_ Road", first.address());
        assertEquals("Adana", first.region());
        assertEquals("United Kingdom", first.country());
    }

    @Test
    void keepsNonAsciiCharacters() {
        Student s = TestData.realStudents().get(3);
        assertEquals("Hà Giang", s.region());
    }

    @Test
    void refusesARecordWithTheWrongFieldCount() {
        String raw = "id#name#Grade#email#address#region#country$1#Only#Three$";
        assertThrows(IllegalStateException.class, () -> StudentRoster.parse(raw));
    }
}
