package com.propertize.platform.employecraft.integration;

import com.propertize.platform.employecraft.event.EmployeeEventPublisher;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the Employee REST API using REST-assured.
 *
 * <p>
 * These tests spin up the real Spring context on a random port
 * and exercise the full HTTP stack (filters → controllers → services → DB).
 *
 * <p>
 * Requires a running database (H2 via test profile or Testcontainers).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmployeeApiIntegrationTest {

    @LocalServerPort
    private int port;

    /** Prevent real Kafka connections during integration tests. */
    @MockBean
    private EmployeeEventPublisher eventPublisher;

    private static final String ORG_ID = "00000000-0000-0000-0000-000000000001";
    private static UUID createdEmployeeId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1/employees";
    }

    @Test
    @Order(1)
    @DisplayName("POST /employees → 201 Created")
    void createEmployee_shouldReturn201() {
        Map<String, Object> request = Map.of(
                "firstName", "Jane",
                "lastName", "Doe",
                "email", "jane.doe+" + UUID.randomUUID().toString().substring(0, 8) + "@test.com",
                "employmentType", "FULL_TIME",
                "hireDate", "2026-03-01",
                "jobTitle", "Software Engineer",
                "payType", "SALARY",
                "payRate", 95000);

        String id = given()
                .contentType(ContentType.JSON)
                .header("X-Gateway-Source", "api-gateway")
                .header("X-User-Id", "1")
                .header("X-Username", "admin")
                .header("X-Email", "admin@propertize.io")
                .header("X-Roles", "ORGANIZATION_ADMIN")
                .header("X-Organization-Id", ORG_ID)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("firstName", equalTo("Jane"))
                .body("lastName", equalTo("Doe"))
                .body("status", equalTo("PENDING"))
                .extract().path("id");

        createdEmployeeId = UUID.fromString(id);
    }

    @Test
    @Order(2)
    @DisplayName("GET /employees/{id} → 200 OK")
    void getEmployee_shouldReturn200() {
        Assumptions.assumeTrue(createdEmployeeId != null, "Create test must pass first");

        given()
                .header("X-Gateway-Source", "api-gateway")
                .header("X-User-Id", "1")
                .header("X-Roles", "ORGANIZATION_ADMIN")
                .header("X-Organization-Id", ORG_ID)
                .when()
                .get("/{id}", createdEmployeeId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdEmployeeId.toString()))
                .body("firstName", equalTo("Jane"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /employees → 200 OK (paginated list)")
    void listEmployees_shouldReturnPage() {
        given()
                .header("X-Gateway-Source", "api-gateway")
                .header("X-User-Id", "1")
                .header("X-Roles", "ORGANIZATION_ADMIN")
                .header("X-Organization-Id", ORG_ID)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get()
                .then()
                .statusCode(200)
                .body("content", instanceOf(java.util.List.class));
    }

    @Test
    @Order(4)
    @DisplayName("POST /employees with missing required fields → 400")
    void createEmployee_missingFields_shouldReturn400() {
        Map<String, Object> invalid = Map.of("firstName", "");

        given()
                .contentType(ContentType.JSON)
                .header("X-Gateway-Source", "api-gateway")
                .header("X-User-Id", "1")
                .header("X-Roles", "ORGANIZATION_ADMIN")
                .header("X-Organization-Id", ORG_ID)
                .body(invalid)
                .when()
                .post()
                .then()
                .statusCode(400);
    }

    @Test
    @Order(5)
    @DisplayName("GET /employees/{nonexistent} → 404")
    void getEmployee_notFound_shouldReturn404() {
        given()
                .header("X-Gateway-Source", "api-gateway")
                .header("X-User-Id", "1")
                .header("X-Roles", "ORGANIZATION_ADMIN")
                .header("X-Organization-Id", ORG_ID)
                .when()
                .get("/{id}", UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("code", equalTo("EMPLOYEE_NOT_FOUND"));
    }
}
