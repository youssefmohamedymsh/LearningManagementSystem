package io.github.youssefmohamedymsh.lms.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.youssefmohamedymsh.lms.data.EnrollmentJson;
import io.github.youssefmohamedymsh.lms.model.EnrollmentException;
import io.github.youssefmohamedymsh.lms.model.Student;
import io.github.youssefmohamedymsh.lms.model.UnknownIdException;
import io.github.youssefmohamedymsh.lms.service.EnrollmentService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Web front end for the LMS: a JSON API over EnrollmentService plus the single-page UI in /web.
 *
 * <pre>
 * GET    /api/students                          summary list
 * GET    /api/students/{id}                     one student with their courses
 * GET    /api/courses                           the catalogue
 * POST   /api/students/{id}/courses             {"courseId": 3}           enroll
 * DELETE /api/students/{id}/courses/{courseId}                            drop
 * PUT    /api/students/{id}/courses/{courseId}  {"newCourseId": 5}        replace
 * GET    /api/enrollments                       all enrollments, as saved to enrollments.json
 * </pre>
 * Rule violations return 409, unknown ids 404, malformed requests 400, each with {"message": "..."}.
 */
public final class WebServer {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8", "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8", "svg", "image/svg+xml");

    private final EnrollmentService service;
    private final Path outputDir;
    private final HttpServer server;

    private WebServer(EnrollmentService service, Path outputDir, int port) throws IOException {
        this.service = service;
        this.outputDir = outputDir;
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/", this::api);
        server.createContext("/", this::staticFile);
        server.setExecutor(Executors.newFixedThreadPool(4));
    }

    public static WebServer start(EnrollmentService service, Path outputDir, int port) throws IOException {
        WebServer web = new WebServer(service, outputDir, port);
        web.server.start();
        return web;
    }

    public String url() {
        return "http://localhost:" + server.getAddress().getPort() + "/";
    }

    public void stop() {
        server.stop(0);
    }

    // ---------- API ----------

    private void api(HttpExchange ex) throws IOException {
        try {
            send(ex, 200, route(ex));
        } catch (UnknownIdException e) {
            send(ex, 404, Map.of("message", e.getMessage()));
        } catch (EnrollmentException e) {
            send(ex, 409, Map.of("message", e.getMessage()));
        } catch (NotFound e) {
            send(ex, 404, Map.of("message", "Not found"));
        } catch (Exception e) {
            send(ex, 400, Map.of("message", "Malformed request"));
        }
    }

    private Object route(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String[] p = ex.getRequestURI().getPath().replaceAll("/+$", "").split("/"); // ["", "api", ...]

        if (p.length == 3 && p[2].equals("students") && method.equals("GET")) {
            return service.students().stream().map(WebServer::summary).toList();
        }
        if (p.length == 3 && p[2].equals("courses") && method.equals("GET")) {
            return service.courses();
        }
        if (p.length == 3 && p[2].equals("enrollments") && method.equals("GET")) {
            return JSON.readTree(EnrollmentJson.of(service.students()));
        }
        if (p.length >= 4 && p[2].equals("students")) {
            int studentId = Integer.parseInt(p[3]);
            if (p.length == 4 && method.equals("GET")) {
                return detail(service.student(studentId));
            }
            if (p.length == 5 && p[4].equals("courses") && method.equals("POST")) {
                service.enroll(studentId, intField(body(ex), "courseId"));
                return changed(studentId);
            }
            if (p.length == 6 && p[4].equals("courses")) {
                int courseId = Integer.parseInt(p[5]);
                if (method.equals("DELETE")) {
                    service.unenroll(studentId, courseId);
                    return changed(studentId);
                }
                if (method.equals("PUT")) {
                    service.replace(studentId, courseId, intField(body(ex), "newCourseId"));
                    return changed(studentId);
                }
            }
        }
        throw new NotFound();
    }

    private Map<String, Object> changed(int studentId) {
        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputDir.resolve("enrollments.json"), EnrollmentJson.of(service.students()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return detail(service.student(studentId));
    }

    private static Map<String, Object> summary(Student s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.id());
        m.put("name", s.name());
        m.put("grade", s.grade());
        m.put("country", s.country());
        m.put("courseCount", s.courses().size());
        return m;
    }

    private static Map<String, Object> detail(Student s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.id());
        m.put("name", s.name());
        m.put("grade", s.grade());
        m.put("email", s.email());
        m.put("address", s.address());
        m.put("region", s.region());
        m.put("country", s.country());
        m.put("courses", s.courses());
        m.put("maxCourses", Student.MAX_COURSES);
        return m;
    }

    private static Map<String, Object> body(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return JSON.readValue(in.readAllBytes(), new TypeReference<>() {
            });
        }
    }

    private static int intField(Map<String, Object> body, String field) {
        Object v = body.get(field);
        if (v == null) {
            throw new IllegalArgumentException("Missing " + field);
        }
        return Integer.parseInt(v.toString());
    }

    private static void send(HttpExchange ex, int status, Object payload) throws IOException {
        byte[] bytes = JSON.writeValueAsBytes(payload);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    // ---------- Static UI ----------

    private void staticFile(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        InputStream in = path.contains("..") ? null : WebServer.class.getResourceAsStream("/web" + path);
        if (in == null) {
            byte[] msg = "Not found".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, msg.length);
            ex.getResponseBody().write(msg);
            ex.close();
            return;
        }
        try (in) {
            byte[] bytes = in.readAllBytes();
            String ext = path.substring(path.lastIndexOf('.') + 1);
            ex.getResponseHeaders().set("Content-Type", TYPES.getOrDefault(ext, "application/octet-stream"));
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = ex.getResponseBody()) {
                out.write(bytes);
            }
        }
    }

    private static final class NotFound extends RuntimeException {
    }
}
