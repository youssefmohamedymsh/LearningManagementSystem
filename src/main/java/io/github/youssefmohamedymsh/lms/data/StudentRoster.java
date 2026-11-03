package io.github.youssefmohamedymsh.lms.data;

import io.github.youssefmohamedymsh.lms.model.Student;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the student export, a single line where '#' separates fields and '$' separates records,
 * with a header record first: id#name#Grade#email#address#region#country$1#Malcolm Barnes#9#...$
 */
public final class StudentRoster {

    private static final int FIELDS = 7;

    private StudentRoster() {
    }

    public static List<Student> parse(String raw) {
        String[] records = raw.strip().split("\\$");
        List<Student> students = new ArrayList<>();
        for (int i = 1; i < records.length; i++) { // record 0 is the header
            String record = records[i].strip();
            if (record.isEmpty()) {
                continue;
            }
            String[] f = record.split("#", -1);
            if (f.length != FIELDS) {
                throw new IllegalStateException("Student record " + i + " has " + f.length + " fields, expected " + FIELDS);
            }
            students.add(new Student(Integer.parseInt(f[0].strip()), f[1].strip(), Integer.parseInt(f[2].strip()),
                    f[3].strip(), f[4].strip(), f[5].strip(), f[6].strip()));
        }
        return students;
    }
}
