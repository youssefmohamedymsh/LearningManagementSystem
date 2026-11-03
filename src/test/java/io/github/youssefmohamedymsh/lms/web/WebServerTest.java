package io.github.youssefmohamedymsh.lms.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.youssefmohamedymsh.lms.TestData;
import io.github.youssefmohamedymsh.lms.service.EnrollmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Web API")
class WebServerTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    @TempDir
    Path outputDir;

    private WebServer server;

    @BeforeEach
    void start() throws Exception {
        server = WebServer.start(new EnrollmentService(TestData.realStudents(), TestData.realCourses()), outputDir, 0);
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    private record Reply(int status, JsonNode body) {
    }

    private Reply call(String method, String path, String json) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(server.url() + path.substring(1)));
        req.method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        if (json != null) {
            req.header("Content-Type", "application/json");
        }
        HttpResponse<String> res = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(res.statusCode(), JSON.readTree(res.body()));
    }

    @Test
    void listsAllStudentsWithCourseCounts() throws Exception {
        Reply r = call("GET", "/api/students", null);
        assertEquals(200, r.status());
        assertEquals(100, r.body().size());
        assertEquals("Malcolm Barnes", r.body().get(0).get("name").asText());
        assertEquals(0, r.body().get(0).get("courseCount").asInt());
    }

    @Test
    void listsTheCatalogue() throws Exception {
        Reply r = call("GET", "/api/courses", null);
        assertEquals(17, r.body().size());
        assertEquals("Scott, Austin C.", r.body().get(0).get("instructor").asText());
    }

    @Test
    void enrollReplaceAndDrop() throws Exception {
        assertEquals(200, call("POST", "/api/students/7/courses", "{\"courseId\": 1}").status());
        assertEquals(200, call("POST", "/api/students/7/courses", "{\"courseId\": 2}").status());

        Reply replaced = call("PUT", "/api/students/7/courses/1", "{\"newCourseId\": 5}");
        assertEquals(200, replaced.status());
        assertEquals(5, replaced.body().get("courses").get(0).get("id").asInt());

        Reply dropped = call("DELETE", "/api/students/7/courses/2", null);
        assertEquals(1, dropped.body().get("courses").size());

        String saved = Files.readString(outputDir.resolve("enrollments.json")).replaceAll("\\s", "");
        assertTrue(saved.contains("\"7\":[5]"), saved);
    }

    @Test
    @DisplayName("Rule violations return 409 with the rule's message")
    void ruleViolationIs409() throws Exception {
        call("POST", "/api/students/3/courses", "{\"courseId\": 4}");
        Reply duplicate = call("POST", "/api/students/3/courses", "{\"courseId\": 4}");
        assertEquals(409, duplicate.status());
        assertEquals("Can't enroll in the same course twice", duplicate.body().get("message").asText());

        Reply lastCourse = call("DELETE", "/api/students/3/courses/4", null);
        assertEquals(409, lastCourse.status());
        assertEquals("Student should be enrolled in at least one course", lastCourse.body().get("message").asText());
    }

    @Test
    void seventhCourseIs409() throws Exception {
        for (int c = 1; c <= 6; c++) {
            assertEquals(200, call("POST", "/api/students/9/courses", "{\"courseId\": " + c + "}").status());
        }
        Reply seventh = call("POST", "/api/students/9/courses", "{\"courseId\": 7}");
        assertEquals(409, seventh.status());
        assertEquals("Can't enroll in more than six courses", seventh.body().get("message").asText());
    }

    @Test
    void unknownIdsAre404() throws Exception {
        assertEquals(404, call("GET", "/api/students/101", null).status());
        Reply course = call("POST", "/api/students/1/courses", "{\"courseId\": 99}");
        assertEquals(404, course.status());
        assertEquals("There's no course with id 99", course.body().get("message").asText());
    }

    @Test
    void malformedRequestsAre400() throws Exception {
        assertEquals(400, call("POST", "/api/students/1/courses", "{\"wrong\": 1}").status());
        assertEquals(400, call("GET", "/api/students/abc", null).status());
    }

    @Test
    void servesTheUi() throws Exception {
        HttpResponse<String> page = http.send(HttpRequest.newBuilder(URI.create(server.url())).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("Enrollment Desk"));
    }
}
