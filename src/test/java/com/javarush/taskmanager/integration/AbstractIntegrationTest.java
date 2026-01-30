package com.javarush.taskmanager.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Slf4j
public abstract class AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ApplicationContext applicationContext;

    private static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
                .withDatabaseName("taskmanager_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true)
                .withStartupTimeout(Duration.ofMinutes(3));
        postgres.start();
        log.info("Test PostgreSQL container started on port: {}", postgres.getFirstMappedPort());
    }

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 5);
        registry.add("spring.datasource.hikari.minimum-idle", () -> 1);
        registry.add("spring.datasource.hikari.connection-timeout", () -> 60000);
        registry.add("spring.datasource.hikari.idle-timeout", () -> 300000);
        registry.add("spring.datasource.hikari.max-lifetime", () -> 120000);
        registry.add("spring.datasource.hikari.connection-test-query", () -> "SELECT 1");
    }

    @PostConstruct
    public void init() {
        log.info("Initializing test class: {}", this.getClass().getSimpleName());
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    protected String login(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                            "username": "%s",
                            "password": "%s"
                        }
                        """, username, password))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    protected String registerAndLogin(String username, String password) {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                            "username": "%s",
                            "password": "%s"
                        }
                        """, username, password))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201);

        return login(username, password);
    }
}

