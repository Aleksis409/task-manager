package com.javarush.taskmanager.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminApiIT extends AbstractIntegrationTest {

    @Test
    void shouldRejectWithoutToken() {
        given()
                .when()
                .put("/api/admin/users/1/role")
                .then()
                .statusCode(403);
    }

    @Test
    void shouldRejectNonAdmin() {
        String userToken = registerAndLogin("user1", "password");

        given()
                .auth().oauth2(userToken)
                .contentType(ContentType.JSON)
                .body("""
                        { "role": "ROLE_ADMIN" }
                        """)
                .when()
                .put("/api/admin/users/999/role")
                .then()
                .statusCode(403);
    }

    @Test
    void shouldAllowAdminToChangeRole() {
        String adminToken = login("admin", "password");
        Long userId = registerAndReturnId("victim2", "password");

        given()
                .auth().oauth2(adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        { "role": "ROLE_ADMIN" }
                        """)
                .when()
                .put("/api/admin/users/{id}/role", userId)
                .then()
                .statusCode(204);

        String newAdminToken = login("victim2", "password");

        given()
                .auth().oauth2(newAdminToken)
                .contentType(ContentType.JSON)
                .body("""
                        { "role": "ROLE_USER" }
                        """)
                .when()
                .put("/api/admin/users/{id}/role", userId)
                .then()
                .statusCode(204);
    }

    @Test
    void shouldRejectNullRole() {
        String adminToken = registerAndLogin("root", "password");
        Long userId = registerAndReturnId("target", "password");

        given()
                .auth().oauth2(adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        {}
                        """)
                .when()
                .put("/api/admin/users/{id}/role", userId)
                .then()
                .statusCode(400)
                .body("error", notNullValue());
    }

    protected Long registerAndReturnId(String username, String password) {

        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(username, password))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }
}