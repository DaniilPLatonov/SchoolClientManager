package ru.platonov.booking_service;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import ru.platonov.booking_service.dto.SheduleDTO;
import ru.platonov.booking_service.service.SheduleService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = "/sql/fill_data_for_test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class SheduleControllerTest {

    @LocalServerPort
    private Integer port;

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    void getAllShedulesTest() {
        Response response = given()
                .when()
                .get("/shedules")
                .then()
                .extract().response();

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("").size() > 0);
    }

    @Test
    void getShedulesByTutorIdTest() {
        UUID tutorId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        Response response = given()
                .when()
                .get("/shedules/tutor/" + tutorId)
                .then()
                .extract().response();

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(!response.jsonPath().getList("").isEmpty());

        UUID nonExistentTutorId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        Response negativeResponse = given()
                .when()
                .get("/shedules/tutor/" + nonExistentTutorId)
                .then()
                .extract().response();

        Assertions.assertEquals(404, negativeResponse.statusCode());
        Assertions.assertEquals("No available schedules found for tutor with id: " + nonExistentTutorId, negativeResponse.jsonPath().getString("message"));
    }

    @Test
    void createDuplicateSheduleTest() {
        SheduleDTO newShedule = new SheduleDTO();
        newShedule.setTutorId(UUID.fromString("77777777-7777-7777-7777-777777777777"));
        newShedule.setSubjectId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        newShedule.setDate(LocalDate.now().plusDays(1));
        newShedule.setStartTime(LocalTime.of(10, 0));
        newShedule.setEndTime(LocalTime.of(11, 0));
        newShedule.setBooked(false);

        // Создаем новое расписание
        Response response = given()
                .header("Content-Type", "application/json")
                .body(newShedule)
                .when()
                .post("/shedules")
                .then()
                .extract().response();

        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertNotNull(response.jsonPath().getString("id"), "Schedule ID should not be null");
        Assertions.assertEquals(newShedule.getTutorId().toString(), response.jsonPath().getString("tutorId"));

        // Попытка создать дубликат
        Response duplicateResponse = given()
                .header("Content-Type", "application/json")
                .body(newShedule)
                .when()
                .post("/shedules")
                .then()
                .extract().response();

        Assertions.assertEquals(400, duplicateResponse.statusCode());
        Assertions.assertTrue(duplicateResponse.jsonPath().getString("message").contains("Расписание с такими параметрами уже существует"));
    }






}
