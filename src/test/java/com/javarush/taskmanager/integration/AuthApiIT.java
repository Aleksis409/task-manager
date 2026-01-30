package com.javarush.taskmanager.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthApiIT extends AbstractIntegrationTest {

    private static final String TEST_USERNAME = "integration_test_user";
    private static final String TEST_PASSWORD = "integration_test_password_123";
    private static String refreshToken;

    @Test
    @Order(1)
    void register_ShouldCreateNewUser() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                    {
                        "username": "%s",
                        "password": "%s"
                    }
                    """, TEST_USERNAME, TEST_PASSWORD))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("username", is(TEST_USERNAME))
                .body("role", is("ROLE_USER"));
    }

    @Test
    @Order(2)
    void register_ShouldRejectDuplicateUsername() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                    {
                        "username": "%s",
                        "password": "%s"
                    }
                    """, TEST_USERNAME, "different_password"))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(400)
                .body("error", is("Username already exists"));
    }

    @Test
    @Order(3)
    void register_ShouldValidateInput() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "username": "",
                        "password": "123"
                    }
                    """)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(400)
                .body("$", hasSize(greaterThan(0)));
    }

    @Test
    @Order(4)
    void login_ShouldReturnTokensForValidCredentials() {
        var response = given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                    {
                        "username": "%s",
                        "password": "%s"
                    }
                    """, TEST_USERNAME, TEST_PASSWORD))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("refreshToken", notNullValue())
                .extract();

        refreshToken = response.path("refreshToken");
    }

    @Test
    @Order(5)
    void login_ShouldRejectInvalidCredentials() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "username": "nonexistent",
                        "password": "wrongpassword"
                    }
                    """)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(401)
                .body("error", is("Invalid username or password"));
    }

    @Test
    @Order(6)
    void refresh_ShouldReturnNewTokens() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                    {
                        "refreshToken": "%s"
                    }
                    """, refreshToken))
                .when()
                .post("/api/auth/refresh")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("refreshToken", notNullValue());
    }

    @Test
    @Order(7)
    void refresh_ShouldRejectInvalidToken() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                {
                    "refreshToken": "invalid.token.here"
                }
                """)
                .when()
                .post("/api/auth/refresh")
                .then()
                .statusCode(400)
                .body("error", is("Invalid token type"));
    }

    @Test
    @Order(8)
    void logout_ShouldInvalidateToken() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                    {
                        "refreshToken": "%s"
                    }
                    """, refreshToken))
                .when()
                .post("/api/auth/logout")
                .then()
                .statusCode(204);
    }

    @Test
    @Order(9)
    void refresh_ShouldFailAfterLogout() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                    {
                        "refreshToken": "%s"
                    }
                    """, refreshToken))
                .when()
                .post("/api/auth/refresh")
                .then()
                .statusCode(400)
                .body("error", is("Refresh token revoked"));
    }

    @Test
    void login_ShouldValidateInput() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "username": "",
                        "password": ""
                    }
                    """)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(400)
                .body("$", hasSize(greaterThan(0)));
    }

    @Test
    void register_WithVeryLongPassword_ShouldRespectValidation() {
        String longPassword = "a".repeat(256);

        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                    {
                        "username": "user_with_long_pass",
                        "password": "%s"
                    }
                    """, longPassword))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(400)
                .body("$", hasSize(greaterThan(0)))
                .body("[0].field", is("password"))
                .body("[0].message", anyOf(
                        containsString("size must be between"),
                        containsString("размер должен находиться в диапазоне")
                ));
    }
}