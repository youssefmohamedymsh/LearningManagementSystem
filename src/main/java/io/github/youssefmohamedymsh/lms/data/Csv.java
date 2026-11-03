package io.github.youssefmohamedymsh.lms.data;

import io.github.youssefmohamedymsh.lms.model.Course;
import io.github.youssefmohamedymsh.lms.model.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** RFC 4180 CSV: fields containing commas, quotes or line breaks are quoted. */
public final class Csv {

    private Csv() {
    }

    public static String courses(List<Course> courses) {
        StringBuilder out = new StringBuilder(line("id", "name", "instructor", "duration", "time", "location"));
        for (Course c : courses) {
            out.append(line(String.valueOf(c.id()), c.name(), c.instructor(), c.duration(), c.time(), c.location()));
        }
        return out.toString();
    }

    public static String students(List<Student> students) {
        StringBuilder out = new StringBuilder(line("id", "name", "grade", "email", "address", "region", "country"));
        for (Student s : students) {
            out.append(line(String.valueOf(s.id()), s.name(), String.valueOf(s.grade()), s.email(), s.address(),
                    s.region(), s.country()));
        }
        return out.toString();
    }

    static String line(String... fields) {
        return Stream.of(fields).map(Csv::escape).collect(Collectors.joining(",")) + "\n";
    }

    static String escape(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /** Splits one CSV line, honouring quotes. */
    public static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (ch == '"') {
                    quoted = false;
                } else {
                    current.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
