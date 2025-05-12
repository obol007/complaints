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

        Optional<Complaint> savedComplaint = repository.findById((long) returnedId);
        assertTrue(savedComplaint.isPresent(), "Complaint should be saved in H2");
        Complaint complaint = savedComplaint.get();
        assertEquals("Product_123", complaint.getProductId());
        assertEquals("Broken screen", complaint.getContent());
        assertEquals("alice@example.com", complaint.getReporter());
        assertEquals(1, complaint.getCounter());
        assertNotNull(complaint.getCreatedAt());
        assertNotNull(complaint.getCountry());
    }

    @Test
    void whenCreateDuplicateComplaint_thenCounterIncrements_thenContentTheSame_andDbUpdated() {
        String productId = "Product_456";
        String reporter = "bob@example.com";
        String originalContent = "Original issue";
        ComplaintRequest complaintRequest1 = new ComplaintRequest(
                productId,
                originalContent,
                reporter
        );
        ComplaintRequest complaintRequest2 = new ComplaintRequest(
                productId,
                "New issue",
                reporter
        );

        int id1 =
                given()
                        .contentType(ContentType.JSON)
                        .body(complaintRequest1)
                        .when()
                        .post()
                        .then()
                        .statusCode(200)
                        .body("counter", equalTo(1))
                        .extract().path("id");

        int id2 =
                given()
                        .contentType(ContentType.JSON)
                        .body(complaintRequest2)
                        .when()
                        .post()
                        .then()
                        .statusCode(200)
                        .body("counter", equalTo(2))
                        .extract().path("id");

        assertEquals(id1, id2, "Duplicate should return the same Id");
        Complaint complaint = repository.findById((long) id1).orElseThrow();
        assertEquals(2, complaint.getCounter(), "Database counter should be 2");
        assertEquals("Original issue", complaint.getContent(), "Content should not be changed");
    }

    @Test
    void whenUpdateContent_thenContentChanges_inDb() {
        String updatedContent = "New issue";
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

        ContentUpdateRequest contentUpdateRequest = new ContentUpdateRequest(updatedContent);

        given()
                .contentType(ContentType.JSON)
                .body(contentUpdateRequest)
                .when()
                .put("/{id}", id)
                .then()
                .statusCode(200)
                .body("content", equalTo(updatedContent));

        Complaint complaint = repository.findById((long) id).orElseThrow();
        assertEquals(updatedContent, complaint.getContent());
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
        Complaint complaint = repository
                .findByProductIdAndReporter("Product_000", "dave@example.com")
                .orElseThrow();
        assertEquals("Test get all", complaint.getContent());
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

        assertEquals(1, repository.count(), "DB has one record");
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
