# Learning Management System

[![CI](https://github.com/youssefmohamedymsh/LearningManagementSystem/actions/workflows/ci.yml/badge.svg)](https://github.com/youssefmohamedymsh/LearningManagementSystem/actions/workflows/ci.yml)
![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![JUnit 5](https://img.shields.io/badge/JUnit-5-25A162?logo=junit5&logoColor=white)

A course enrollment system with a **web UI** and a **command-line** interface. It loads 100 students from a delimited text export and 17 courses from XML, converts both to CSV, and lets you enroll students in courses, drop them and swap them. Enrollments are saved as JSON after every change.

![Enrollment desk web UI](docs/web-ui.png)

## The story: re-testing my own code

I first wrote this in January 2023, while learning Java during Udacity's testing nanodegree. Coming back to it as a QA engineer, I tested it the way I'd test anyone else's code and found six defects. Version 2 fixes them, and **every bug has a regression test** so it can't come back.

| # | Defect in v1 | Impact | Fix | Regression test |
|---|---|---|---|---|
| 1 | Course CSV written and read with a plain `split(",")`, but instructor names contain commas ("Scott, Austin C.") | Every course's instructor, duration, time and location shifted one column | RFC 4180 quoting on write, quote-aware parsing on read | `CsvTest.instructorWithCommaStaysInOneColumn` |
| 2 | Student-ID check threw as soon as *any* student didn't match | Only student #1 could ever be selected | Validate against the set of known IDs | `MainTest.anyValidStudentCanBeSelected` |
| 3 | Students looked up with `list.get(id)` | Selecting student 5 showed and changed student 6 | Look up by ID in a map | `EnrollmentServiceTest.looksUpByIdNotIndex` |
| 4 | Replace removed the old course *before* checking the new one | A refused replacement (duplicate or limit) lost the original course | Check every rule first, then swap in place | `StudentEnrollmentTest.refusedReplacementKeepsOriginal` |
| 5 | JSON keyed by `id - 1` | Student 2's courses were saved under "1" | Key by the real ID | `EnrollmentJsonTest.keyedByRealStudentId` |
| 6 | Hard-coded `C:\Work Files\...` paths | Ran on one Windows machine only | Data on the classpath; output in `./output` | Every test runs in CI on Linux |

I also hardened the XML loader against XXE (external-entity) attacks, made each course row parse independently so one malformed row can't shift the rest, and replaced the menu's mutual recursion with a loop.

## Business rules (all covered by tests)

- A student can't enroll in the same course twice.
- A student can take at most **6** courses.
- A student can't drop their **last** course.
- Replacing a course is all-or-nothing: if the new course isn't allowed, nothing changes.
- Unknown student or course IDs are refused with a clear message.

## Web UI

An enrollment desk in the browser: search the 100 students, pick one, and enroll, drop or swap courses. The rules are visible before you hit them. A meter shows the 6 course slots, **Enroll** is disabled (with the reason on hover) for courses the student already has or once they're full, and **Drop** is locked on their last course. Every change shows a confirmation or the rule that blocked it.

It runs on the JDK's built-in HTTP server, with no web framework, over the same `EnrollmentService` the CLI uses. Every element has a `data-testid`, ready for UI automation.

| Method | Path | Does |
|---|---|---|
| `GET` | `/api/students` | Student list with course counts |
| `GET` | `/api/students/{id}` | One student with their courses |
| `GET` | `/api/courses` | Course catalogue |
| `POST` | `/api/students/{id}/courses` | Enroll: `{"courseId": 3}` |
| `DELETE` | `/api/students/{id}/courses/{courseId}` | Drop |
| `PUT` | `/api/students/{id}/courses/{courseId}` | Replace: `{"newCourseId": 5}` |
| `GET` | `/api/enrollments` | All enrollments |

Rule violations return **409** with the rule's message, unknown IDs **404**, and malformed requests **400**.

## Tests

**45 JUnit 5 tests** in five layers:

| Layer | What's tested |
|---|---|
| `model` | Every enrollment rule, including the edge cases (exactly 6 courses, the last course, a replacement at the limit) |
| `data` | XML and text parsing against the real data files, CSV quoting round-trips, JSON shape, malformed input, XXE rejection |
| `service` | ID lookup, unknown IDs, atomic replacement through the service |
| `cli` | The menu driven with scripted keyboard input, including invalid input and end-of-input |
| `web` | Every API endpoint over real HTTP: happy paths, 409 rule violations, 404 unknown IDs, 400 malformed requests |

```bash
mvn test
```

## Run it

```bash
mvn compile exec:java -Dexec.args="--web"   # web UI at http://localhost:8080
mvn compile exec:java                        # command-line menu
```

In the CLI, pick a student by ID, then `a` to enroll, `d` to drop, `r` to replace, `b` to go back, and `q` on the home screen to quit. `output/` gets `students.csv`, `courses.csv` and `enrollments.json`.

## Project structure

```
src/main/java/.../lms/
  model/     Course, Student (enrollment rules), EnrollmentException
  data/      CourseCatalogue (XML), StudentRoster (text), Csv, EnrollmentJson
  service/   EnrollmentService: lookups by ID, applies changes
  cli/       Main: the interactive menu (and --web to start the web UI)
  web/       WebServer: JSON API and static files
src/main/resources/web/           the web UI (HTML, CSS, JS)
src/main/resources/data/
  courses.xml, student-data.txt   (synthetic sample data)
src/test/java/...                 tests mirroring the main packages
```
