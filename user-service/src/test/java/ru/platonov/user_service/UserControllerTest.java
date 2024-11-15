package ru.platonov.user_service;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import ru.platonov.user_service.dto.LoginDTO;
import ru.platonov.user_service.dto.UserDTO;
import ru.platonov.user_service.service.UserService;

import java.util.UUID;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = "/sql/fill_data_for_test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UserControllerTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    void getAllSubjectsTest() {
        Response response = given()
                .when()
                .get("/api/users/subjects")
                .then()
                .extract().response();

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("").size() > 0);
    }

    @Test
    void getTutorsBySubjectTest() {
        UUID subjectId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        Response response = given()
                .when()
                .get("/api/users/" + subjectId + "/tutors")
                .then()
                .extract().response();

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(!response.jsonPath().getList("").isEmpty());

        UUID nonExistentSubjectId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        Response negativeResponse = given()
                .when()
                .get("/api/users/" + nonExistentSubjectId + "/tutors")
                .then()
                .extract().response();

        Assertions.assertEquals(404, negativeResponse.statusCode());
        Assertions.assertEquals("No tutors found for subject: " + nonExistentSubjectId, negativeResponse.jsonPath().getString("message"));

    }


    @Test
    void getSubjectByIdTest() {
        UUID subjectId = UUID.fromString("55555555-5555-5555-5555-555555555555"); // Physics

        Response response = given()
                .when()
                .get("/api/users/subjects/" + subjectId)
                .then()
                .extract().response();

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertNotNull(response.jsonPath().getString("id"));


        UUID nonExistentSubjectId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        Response negativeResponse = given()
                .when()
                .get("/api/users/subjects/" + nonExistentSubjectId)
                .then()
                .extract().response();

        Assertions.assertEquals(404, negativeResponse.statusCode());
        Assertions.assertEquals("Subject not found for id: " + nonExistentSubjectId, negativeResponse.jsonPath().getString("message"));

    }

    @Test
    void getTutorByIdTest() {
        UUID tutorId = UUID.fromString("88888888-8888-8888-8888-888888888888"); // Dr. Marie Curie

        Response response = given()
                .when()
                .get("/api/users/tutor/" + tutorId)
                .then()
                .extract().response();

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertNotNull(response.jsonPath().getString("id"));

        UUID nonExistentTutorId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        Response negativeResponse = given()
                .when()
                .get("/api/users/tutor/" + nonExistentTutorId)
                .then()
                .extract().response();

        Assertions.assertEquals(404, negativeResponse.statusCode());
        Assertions.assertEquals("Tutor not found for id: " + nonExistentTutorId, negativeResponse.jsonPath().getString("message"));
    }

    @Test
    void loginTest() {
        UserDTO userDTO = new UserDTO();
        userDTO.setName("John2 Doe");
        userDTO.setEmail("john2.doe@example.com");
        userDTO.setPassword(passwordEncoder.encode("password1234"));

        try {
            userService.addUser(userDTO);
            System.out.println("User добавлен в базу данных.");
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Ошибка при добавлении пользователя в базу данных: " + e.getMessage());
        }

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("john2.doe@example.com");
        loginDTO.setPassword("password1234");

        Response response = given()
                .header("Content-type", "application/json")
                .and()
                .body(loginDTO)
                .when()
                .post("/api/users/login")
                .then()
                .extract().response();

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getBoolean("success"));


        LoginDTO wrongLoginDTO = new LoginDTO();
        wrongLoginDTO.setEmail("john2.doe@example.com");
        wrongLoginDTO.setPassword("wrongpassword");

        Response negativeResponse = given()
                .header("Content-type", "application/json")
                .and()
                .body(wrongLoginDTO)
                .when()
                .post("/api/users/login")
                .then()
                .extract().response();

        Assertions.assertEquals(401, negativeResponse.statusCode());
        Assertions.assertEquals("Invalid email or password", negativeResponse.jsonPath().getString("message"));
    }

}
