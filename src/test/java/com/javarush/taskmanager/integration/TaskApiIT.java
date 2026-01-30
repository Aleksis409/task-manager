package com.javarush.taskmanager.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TaskApiIT extends AbstractIntegrationTest {

    @Test
    void shouldRejectUnauthorized() {
        given()
                .when()
                .get("/api/tasks")
                .then()
                .statusCode(anyOf(is(401), is(403)));
    }

    @Test
    void shouldCreateAndGetTasks() {
        String token = registerAndLogin("user1", "password");

        Long taskId =
                given()
                        .auth().oauth2(token)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "title": "Test task",
                                  "description": "desc",
                                  "status": "PENDING",
                                  "deadline": "%s"
                                }
                                """.formatted(LocalDate.now().plusDays(2)))
                        .when()
                        .post("/api/tasks")
                        .then()
                        .statusCode(201)
                        .body("title", equalTo("Test task"))
                        .extract()
                        .jsonPath()
                        .getLong("id");

        given()
                .auth().oauth2(token)
                .when()
                .get("/api/tasks")
                .then()
                .statusCode(200)
                .body("$.size()", is(1));

        given()
                .auth().oauth2(token)
                .when()
                .get("/api/tasks/{id}", taskId)
                .then()
                .statusCode(200)
                .body("id", equalTo(taskId.intValue()));
    }

    @Test
    void shouldNotSeeForeignTask() {
        String token1 = registerAndLogin("userA", "password");
        String token2 = registerAndLogin("userB", "password");

        Long id =
                given()
                        .auth().oauth2(token1)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "title":"Private",
                                  "status":"PENDING"
                                }
                                """)
                        .post("/api/tasks")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        given()
                .auth().oauth2(token2)
                .get("/api/tasks/{id}", id)
                .then()
                .statusCode(400)
                .body("error", containsString("Task not found"));
    }

    @Test
    void shouldUpdateTask() {
        String token = registerAndLogin("upd", "password");

        Long id =
                given()
                        .auth().oauth2(token)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "title":"Old",
                                  "status":"PENDING"
                                }
                                """)
                        .post("/api/tasks")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title":"New title",
                          "status":"COMPLETED"
                        }
                        """)
                .put("/api/tasks/{id}", id)
                .then()
                .statusCode(200)
                .body("title", equalTo("New title"))
                .body("status", equalTo("COMPLETED"));
    }

    @Test
    void shouldDeleteTask() {
        String token = registerAndLogin("del", "password");

        Long id =
                given()
                        .auth().oauth2(token)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "title":"To delete",
                                  "status":"PENDING"
                                }
                                """)
                        .post("/api/tasks")
                        .then()
                        .statusCode(201)
                        .extract()
                        .jsonPath()
                        .getLong("id");

        given()
                .auth().oauth2(token)
                .delete("/api/tasks/{id}", id)
                .then()
                .statusCode(204);

        given()
                .auth().oauth2(token)
                .get("/api/tasks/{id}", id)
                .then()
                .statusCode(400);
    }

    @Test
    void shouldFilterTasks() {
        String token = registerAndLogin("filter", "password");

        createTask(token, "t1", "PENDING");
        createTask(token, "t2", "COMPLETED");

        given()
                .auth().oauth2(token)
                .queryParam("status", "COMPLETED")
                .get("/api/tasks/filter")
                .then()
                .statusCode(200)
                .body("$.size()", is(1))
                .body("[0].status", equalTo("COMPLETED"));
    }

    @Test
    void shouldReturnStats() {
        String token = registerAndLogin("stats", "password");

        createTask(token, "t1", "PENDING");
        createTask(token, "t2", "COMPLETED");

        given()
                .auth().oauth2(token)
                .get("/api/tasks/stats")
                .then()
                .statusCode(200)
                .body("totalTasks", equalTo(2))
                .body("completedTasks", equalTo(1));
    }

    @Test
    void shouldValidateRequest() {
        String token = registerAndLogin("val", "password");

        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "description":"no title"
                        }
                        """)
                .post("/api/tasks")
                .then()
                .statusCode(400)
                .body("field", hasItems("title", "status"));
    }

    @Test
    void shouldFilterByDeadlineRange() {

        String token = registerAndLogin("datefilter", "password");

        createTaskWithDeadline(token, "early", LocalDate.now().plusDays(1));
        createTaskWithDeadline(token, "late", LocalDate.now().plusDays(10));

        given()
                .auth().oauth2(token)
                .queryParam("fromDeadline", LocalDate.now().plusDays(5).toString())
                .get("/api/tasks/filter")
                .then()
                .statusCode(200)
                .body("$.size()", is(1))
                .body("[0].title", equalTo("late"));
    }

    private void createTask(String token, String title, String status) {
        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title":"%s",
                          "status":"%s"
                        }
                        """.formatted(title, status))
                .post("/api/tasks")
                .then()
                .statusCode(201);
    }

    private void createTaskWithDeadline(String token, String title, LocalDate deadline) {
        given()
                .auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body("""
            {
              "title":"%s",
              "status":"PENDING",
              "deadline":"%s"
            }
            """.formatted(title, deadline))
                .post("/api/tasks")
                .then()
                .statusCode(201);
    }
}
