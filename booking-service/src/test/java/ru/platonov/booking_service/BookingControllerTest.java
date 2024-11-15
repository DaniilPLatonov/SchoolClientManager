package ru.platonov.booking_service;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import ru.platonov.booking_service.dto.BookingInfoDTO;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = "/sql/fill_data_for_test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class BookingControllerTest {

    @LocalServerPort
    private Integer port;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    void getUserBookingsTest() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111112");

        Response response = given()
                .when()
                .get("/api/bookings/user/" + userId)
                .then()
                .extract().response();

        Assertions.assertEquals(200, response.statusCode());
        List<BookingInfoDTO> bookings = response.jsonPath().getList("", BookingInfoDTO.class);
        Assertions.assertFalse(bookings.isEmpty());


        UUID nonExistentUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        Response nonExistentResponse = given()
                .when()
                .get("/api/bookings/user/" + nonExistentUserId)
                .then()
                .extract().response();

        Assertions.assertEquals(404, nonExistentResponse.statusCode());
        Assertions.assertEquals("No bookings found for user ID: " + nonExistentUserId, nonExistentResponse.jsonPath().getString("message"));

    }

}
