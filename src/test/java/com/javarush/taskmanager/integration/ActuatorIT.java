package com.javarush.taskmanager.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ActuatorIT extends AbstractIntegrationTest {

    @Test
    void healthEndpoint_ShouldReturnUp() {
        given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void metricsEndpoint_ShouldBeAvailable() {
        given()
                .when()
                .get("/actuator/metrics")
                .then()
                .statusCode(200)
                .body("names", not(empty()));
    }

    @Disabled
    @Test
    void prometheusEndpoint_ShouldBeAvailable() {
        given()
                .when()
                .get("/actuator/prometheus")
                .then()
                .statusCode(200)
                .body(containsString("# TYPE"));
    }

    @Test
    void infoEndpoint_ShouldReturnApplicationInfo() {
        given()
                .when()
                .get("/actuator/info")
                .then()
                .statusCode(200);
    }

    @Test
    void listEndpoints() {
        given()
                .when()
                .get("/actuator")
                .then()
                .statusCode(200)
                .log().body();
    }
}
