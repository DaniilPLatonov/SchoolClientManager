package ru.platonov.telegram_bot_service.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.platonov.telegram_bot_service.service.MessageService;
import ru.platonov.telegram_bot_service.service.UserSessionService;
import ru.platonov.telegram_bot_service.user.action.ViewLessonsAction;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.mockito.Mockito.when;

@SpringBootTest
public class ViewLessonsActionTest {

    @Mock
    private MessageService messageService;

    @Mock
    private UserSessionService userSessionService;

    private RestTemplate restTemplate;


    @InjectMocks
    private ViewLessonsAction viewLessonsAction;

    private MockRestServiceServer mockServer;

    @Value("${services.booking-service.url}")
    private String bookingServiceUrl;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        restTemplate = new RestTemplateBuilder().build();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        viewLessonsAction = new ViewLessonsAction(restTemplate, messageService, userSessionService, bookingServiceUrl);
    }


    @Test
    public void testViewLessonWithBookings() {
        String chatId = "12345";
        String userUuid = "test-uuid";

        when(userSessionService.getUserUUID(chatId)).thenReturn(userUuid);

        String url = bookingServiceUrl + "/user/" + userUuid;
        mockServer.expect(once(), requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK).body("[{\"subjectName\":\"Math\",\"tutorName\":\"John Doe\",\"date\":\"2024-11-10\",\"startTime\":\"10:00:00\",\"endTime\":\"11:00:00\"}]")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON));

        viewLessonsAction.viewLessons(chatId);

        verify(messageService).sendTextMessage(chatId, "1. Предмет: Math\nПреподаватель: John Doe\nДата: 2024-11-10\nНачало: 10:00\nКонец: 11:00");
        verify(messageService).showMainMenu(chatId);

        mockServer.verify();
    }


    @Test
    public void testViewLessonsWithNoBookings() {
        String chatId = "12345";
        String userUuid = "test-uuid";

        when(userSessionService.getUserUUID(chatId)).thenReturn(userUuid);

        String url = bookingServiceUrl + "/user/" + userUuid;
        mockServer.expect(once(), requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK).body("[]")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON));

        viewLessonsAction.viewLessons(chatId);

        verify(messageService).sendTextMessage(chatId, "У вас нет предстоящих бронирований.");
        verify(messageService).showMainMenu(chatId);

        mockServer.verify();
    }

    @Test
    public void testViewLessonsWithError() {
        String chatId = "12345";
        String userUuid = "test-uuid";

        when(userSessionService.getUserUUID(chatId)).thenReturn(userUuid);

        String url = bookingServiceUrl + "/user/" + userUuid;
        mockServer.expect(once(), requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        viewLessonsAction.viewLessons(chatId);
        verify(messageService).sendTextMessage(chatId, "У вас нет предстоящих бронирований.");

        mockServer.verify();
    }
}


