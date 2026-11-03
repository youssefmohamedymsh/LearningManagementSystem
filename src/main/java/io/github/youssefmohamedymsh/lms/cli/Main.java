package io.github.youssefmohamedymsh.lms.cli;

import io.github.youssefmohamedymsh.lms.data.CourseCatalogue;
import io.github.youssefmohamedymsh.lms.data.Csv;
import io.github.youssefmohamedymsh.lms.data.EnrollmentJson;
import io.github.youssefmohamedymsh.lms.data.StudentRoster;
import io.github.youssefmohamedymsh.lms.model.Course;
import io.github.youssefmohamedymsh.lms.model.EnrollmentException;
import io.github.youssefmohamedymsh.lms.model.Student;
import io.github.youssefmohamedymsh.lms.service.EnrollmentService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Interactive menu. On start it converts the source data to CSV, then lets you pick a student and
 * enroll, unenroll or replace courses. Every change is saved to output/enrollments.json.
 */
public final class Main {

    private static final String RULE = "=".repeat(84);

    private final EnrollmentService service;
    private final BufferedReader in;
    private final PrintStream out;
    private final Path outputDir;

    public Main(EnrollmentService service, InputStream in, PrintStream out, Path outputDir) {
        this.service = service;
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = out;
        this.outputDir = outputDir;
    }

    public static void main(String[] args) {
        EnrollmentService service = new EnrollmentService(
                StudentRoster.parse(resource("/data/student-data.txt")),
                CourseCatalogue.fromXml(Main.class.getResourceAsStream("/data/courses.xml")));
        new Main(service, System.in, System.out, Path.of("output")).run();
    }

    public void run() {
        writeCsvExports();
        while (true) {
            showHome();
            String answer = ask("Enter a student id (q to quit):");
            if (answer == null || answer.equalsIgnoreCase("q")) {
                out.println("Goodbye.");
                return;
            }
            Integer studentId = parseId(answer);
            if (studentId == null || !exists(studentId)) {
                out.println("Please enter a valid student ID");
                continue;
            }
            if (!studentMenu(studentId)) {
                out.println("Goodbye.");
                return;
            }
        }
    }

    /** Returns false when input ends. */
    private boolean studentMenu(int studentId) {
        while (true) {
            showStudent(service.student(studentId));
            String option = ask("""
                    Please select an option
                    a - Enroll in a course
                    d - Unenroll from a course
                    r - Replace a course
                    b - Back to the student list""");
            if (option == null) {
                return false;
            }
            try {
                switch (option.trim().toLowerCase()) {
                    case "a" -> enroll(studentId);
                    case "d" -> unenroll(studentId);
                    case "r" -> replace(studentId);
                    case "b" -> {
                        return true;
                    }
                    default -> out.println("Please choose a, d, r or b");
                }
            } catch (EnrollmentException e) {
                out.println(e.getMessage());
            } catch (EndOfInput e) {
                return false;
            }
        }
    }

    private void enroll(int studentId) {
        out.println(RULE + "\nEnrollment page\n" + RULE);
        for (Course c : service.courses()) {
            out.println(c.toRow());
        }
        int courseId = askId("Enter the course id to enroll in:");
        service.enroll(studentId, courseId);
        out.println("Enrolled successfully in " + service.course(courseId).name());
        saveEnrollments();
    }

    private void unenroll(int studentId) {
        int courseId = askId("Enter the id of the course to drop:");
        service.unenroll(studentId, courseId);
        out.println("Unenrolled successfully from " + service.course(courseId).name());
        saveEnrollments();
    }

    private void replace(int studentId) {
        int current = askId("Enter the id of the course to replace:");
        int replacement = askId("Enter the id of the new course:");
        service.replace(studentId, current, replacement);
        out.println("Replaced " + service.course(current).name() + " with " + service.course(replacement).name());
        saveEnrollments();
    }

    // ---------- Screens ----------

    private void showHome() {
        out.println("Welcome to LMS\n" + RULE + "\nHome page\n" + RULE + "\nStudent list:");
        out.println(String.format("%-4s %-20s %-6s %-38s %s", "id", "name", "grade", "email", "region, country"));
        for (Student s : service.students()) {
            out.println(s.toRow());
        }
    }

    private void showStudent(Student s) {
        out.println(RULE + "\nStudent details\n" + RULE);
        out.println("Name: " + s.name() + "    Grade: " + s.grade() + "    Email: " + s.email());
        out.println("-".repeat(84) + "\nEnrolled courses:");
        if (s.courses().isEmpty()) {
            out.println("The student hasn't enrolled in any course yet.");
        } else {
            s.courses().forEach(c -> out.println(c.toRow()));
        }
    }

    // ---------- Input helpers ----------

    private String ask(String prompt) {
        out.println(prompt);
        try {
            String line = in.readLine();
            return line == null ? null : line.trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int askId(String prompt) {
        String answer = ask(prompt);
        if (answer == null) {
            throw new EndOfInput();
        }
        Integer id = parseId(answer);
        if (id == null) {
            throw new EnrollmentException("Please enter a number");
        }
        return id;
    }

    private static Integer parseId(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean exists(int studentId) {
        return service.students().stream().anyMatch(s -> s.id() == studentId);
    }

    // ---------- Files ----------

    private void writeCsvExports() {
        write("students.csv", Csv.students(service.students()));
        write("courses.csv", Csv.courses(service.courses()));
    }

    private void saveEnrollments() {
        write("enrollments.json", EnrollmentJson.of(service.students()));
    }

    private void write(String fileName, String content) {
        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputDir.resolve(fileName), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String resource(String path) {
        try (InputStream in = Main.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final class EndOfInput extends RuntimeException {
    }
}
