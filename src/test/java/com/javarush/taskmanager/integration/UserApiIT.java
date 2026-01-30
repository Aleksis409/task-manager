package com.javarush.taskmanager.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserApiIT extends AbstractIntegrationTest {

    private static final String USERNAME = "user_it";
    private static final String PASSWORD = "password123";
    private static String accessToken;

    @Test
    @Order(1)
    void registerAndLogin() {

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(USERNAME, PASSWORD))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201);

        var response =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(USERNAME, PASSWORD))
                        .when()
                        .post("/api/auth/login")
                        .then()
                        .statusCode(200)
                        .body("token", notNullValue())
                        .extract();

        accessToken = response.path("token");
    }

    @Test
    @Order(2)
    void getProfile_ShouldReturnCurrentUser() {

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/users/me")
                .then()
                .statusCode(200)
                .body("username", is(USERNAME))
                .body("role", is("ROLE_USER"))
                .body("id", notNullValue());
    }

    @Test
    @Order(3)
    void changePassword_ShouldUpdatePassword() {

        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "newPassword": "newPassword456"
                        }
                        """)
                .when()
                .put("/api/users/me/password")
                .then()
                .statusCode(204);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "username": "%s",
                          "password": "%s"
                        }
                        """.formatted(USERNAME, PASSWORD))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(401);

        var response =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(USERNAME, "newPassword456"))
                        .when()
                        .post("/api/auth/login")
                        .then()
                        .statusCode(200)
                        .extract();

        accessToken = response.path("token");
    }

    @Test
    @Order(4)
    void deleteAccount_ShouldRemoveUser() {

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .delete("/api/users/me")
                .then()
                .statusCode(204);

        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/users/me")
                .then()
                .statusCode(anyOf(is(401), is(403)));
    }
}

