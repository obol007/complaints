package com.example.demo.controller;

import com.example.demo.dto.ComplaintRequest;
import com.example.demo.dto.ContentUpdateRequest;
import com.example.demo.model.Complaint;
import com.example.demo.repository.ComplaintRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ComplaintControllerIT {

    @LocalServerPort
    int port;

    @Autowired
    ComplaintRepository repository;

    @BeforeEach
    void setUp() {
        baseURI = "http://localhost";
        basePath = "/api/v1/complaints";
        RestAssured.port = port;
        repository.deleteAll();
    }

    @Test
    void whenCreateNewComplaint_thenCounterIsOne_andSavedInDb() {
        ComplaintRequest complaintRequest = new ComplaintRequest(
                "Product_123",
                "Broken screen",
                "alice@example.com"
        );

        int returnedId =
                given()
                        .contentType(ContentType.JSON)
                        .body(complaintRequest)
                        .when()
                        .post()
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("productId", equalTo("Product_123"))
                        .body("content", equalTo("Broken screen"))
                        .body("reporter", equalTo("alice@example.com"))
                        .body("counter", equalTo(1))
                        .extract().path("id");

        Optional<Complaint> saved = repository.findById((long) returnedId);
        assertTrue(saved.isPresent(), "Complaint should be saved in H2");
        Complaint c = saved.get();
        assertEquals("Product_123", c.getProductId());
        assertEquals("Broken screen", c.getContent());
        assertEquals("alice@example.com", c.getReporter());
        assertEquals(1, c.getCounter());
        assertNotNull(c.getCreatedAt());
        assertNotNull(c.getCountry());
    }

    @Test
    void whenCreateDuplicateComplaint_thenCounterIncrements_andDbUpdated() {
        ComplaintRequest complaintRequest = new ComplaintRequest(
                "Product_456",
                "Battery issue",
                "bob@example.com"
        );

        int id1 =
                given()
                        .contentType(ContentType.JSON)
                        .body(complaintRequest)
                        .when()
                        .post()
                        .then()
                        .statusCode(200)
                        .body("counter", equalTo(1))
                        .extract().path("id");

        int id2 =
                given()
                        .contentType(ContentType.JSON)
                        .body(complaintRequest)
                        .when()
                        .post()
                        .then()
                        .statusCode(200)
                        .body("counter", equalTo(2))
                        .extract().path("id");

        assertEquals(id1, id2, "Duplicate should return same ID");

        Complaint c = repository.findById((long) id1).orElseThrow();
        assertEquals(2, c.getCounter(), "Database counter should be 2");
    }

    @Test
    void whenUpdateContent_thenContentChanges_inDb() {
        ComplaintRequest complaintRequest = new ComplaintRequest(
                "Product_789",
                "Old content",
                "carol@example.com"
        );

        int id =
                given()
                        .contentType(ContentType.JSON)
                        .body(complaintRequest)
                        .when()
                        .post()
                        .then()
                        .statusCode(200)
                        .extract().path("id");

        ContentUpdateRequest contentUpdateRequest = new ContentUpdateRequest("New better content");

        given()
                .contentType(ContentType.JSON)
                .body(contentUpdateRequest)
                .when()
                .put("/{id}", id)
                .then()
                .statusCode(200)
                .body("content", equalTo("New better content"));

        Complaint c = repository.findById((long) id).orElseThrow();
        assertEquals("New better content", c.getContent());
    }

    @Test
    void whenGetAll_thenListContainsCreated() {
        assertEquals(0, repository.count(), "DB should be empty at start");

        ComplaintRequest complaintRequest = new ComplaintRequest(
                "Product_000",
                "Test get all",
                "dave@example.com"
        );

        given()
                .contentType(ContentType.JSON)
                .body(complaintRequest)
                .when()
                .post()
                .then()
                .statusCode(200);

        when()
                .get()
                .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("productId", hasItem("Product_000"));

        assertEquals(1, repository.count(), "One record in DB");
        Complaint c = repository
                .findByProductIdAndReporter("Product_000", "dave@example.com")
                .orElseThrow();
        assertEquals("Test get all", c.getContent());
    }

    @Test
    void whenGetOne_thenReturnThatComplaint() {
        ComplaintRequest complaintRequest = new ComplaintRequest(
                "Product_111",
                "Single fetch",
                "eve@example.com"
        );

        int id =
                given()
                        .contentType(ContentType.JSON)
                        .body(complaintRequest)
                        .when()
                        .post()
                        .then()
                        .statusCode(200)
                        .extract().path("id");

        when()
                .get("/{id}", id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("productId", equalTo("Product_111"));

        assertEquals(1, repository.count(), "DB still has one record");
    }

    @Test
    void whenGetNonExisting_then404AndErrorResponse() {
        int nonExistingId = 999;
        when()
                .get("/{id}", nonExistingId)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", equalTo("Complaint not found with id: " + nonExistingId))
                .body("path", equalTo("/api/v1/complaints/" + nonExistingId));
    }

    @Test
    void whenUpdateNonExisting_then404AndErrorResponse() {
        int id = 888;
        ContentUpdateRequest contentUpdateRequest = new ContentUpdateRequest("New content");

        given()
                .contentType(ContentType.JSON)
                .body(contentUpdateRequest)
                .when()
                .put("/{id}", id)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", equalTo("Complaint not found with id: " + id))
                .body("path", equalTo("/api/v1/complaints/" + id));
    }

    @Test
    void whenCreateInvalid_then400AndErrorResponse_andDbUnchanged() {
        Map<String, String> invalid = Map.of(
                "productId", "",
                "content", "",
                "reporter", ""
        );

        given()
                .contentType(ContentType.JSON)
                .body(invalid)
                .when()
                .post()
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("message", allOf(
                        containsString("productId: must not be blank"),
                        containsString("content: must not be blank"),
                        containsString("reporter: must not be blank")
                ))
                .body("path", equalTo("/api/v1/complaints"));

        assertEquals(0, repository.count(), "DB should remain empty after validation failure");
    }
}
