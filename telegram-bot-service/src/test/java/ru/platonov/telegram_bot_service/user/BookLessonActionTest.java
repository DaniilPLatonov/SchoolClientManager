package ru.platonov.telegram_bot_service.user;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.platonov.telegram_bot_service.bot.TelegramBot;
import ru.platonov.telegram_bot_service.dto.BookingRequestDTO;
import ru.platonov.telegram_bot_service.dto.SheduleDTO;
import ru.platonov.telegram_bot_service.dto.SubjectDTO;
import ru.platonov.telegram_bot_service.dto.TutorDTO;
import ru.platonov.telegram_bot_service.service.MessageService;
import ru.platonov.telegram_bot_service.service.UserSessionService;
import ru.platonov.telegram_bot_service.user.action.BookLessonAction;
import ru.platonov.telegram_bot_service.user.kafka.UserKafkaProducer;
import ru.platonov.telegram_bot_service.user.state.UserStateService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@SpringBootTest
public class BookLessonActionTest {

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private TelegramBotsApi telegramBotsApi;


    @Mock
    private UserSessionService userSessionService;

    @MockBean
    private TelegramBot telegramBot;

    @Mock
    private UserKafkaProducer userKafkaProducer;

    @Value("${services.user-service.urlForSubject}")
    private String userServiceSubjectUrl;

    @Value("${services.booking-service.urlForShedule}")
    String bookingServiceScheduleUrl;

    @Value("${services.user-service.urlForTutor}")
    String userServiceTutorUrl;

    @Mock
    private MessageService messageService;

    @Mock
    private UserStateService userStateService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private BookLessonAction bookLessonAction;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        bookLessonAction = new BookLessonAction(restTemplate, userSessionService, userKafkaProducer,
                messageService, userStateService, userServiceSubjectUrl, bookingServiceScheduleUrl, userServiceTutorUrl);
    }

    @Test
    public void testHandleSubjectSelection() {
        String chatId = "12345";
        List<SubjectDTO> subjects = Arrays.asList(new SubjectDTO(UUID.randomUUID(), "Math"), new SubjectDTO(UUID.randomUUID(), "Physics"));

        when(restTemplate.getForObject(userServiceSubjectUrl, SubjectDTO[].class))
                .thenReturn(subjects.toArray(new SubjectDTO[0]));

        when(messageService.createSelectionMessage(anyString(), anyString(), any())).thenReturn(new SendMessage());

        bookLessonAction.handleSubjectSelection(chatId);

        verify(messageService, times(1)).sendMessageToBot(any(SendMessage.class));

        verify(restTemplate, times(1)).getForObject(eq(userServiceSubjectUrl), eq(SubjectDTO[].class));
    }


    @Test
    public void testHandleTutorSelection() {
        String chatId = "12345";
        String subjectId = "subject-id";
        List<TutorDTO> tutors = Arrays.asList(new TutorDTO(UUID.randomUUID(), "John Doe"), new TutorDTO(UUID.randomUUID(), "Jane Doe"));

        when(restTemplate.getForObject(anyString(), eq(TutorDTO[].class)))
                .thenReturn(tutors.toArray(new TutorDTO[0]));
        when(messageService.createSelectionMessage(anyString(), anyString(), any())).thenReturn(new SendMessage());

        bookLessonAction.handleTutorSelection(chatId, subjectId);

        verify(messageService, times(1)).sendMessageToBot(any(SendMessage.class));
    }

    @Test
    public void testHandleScheduleSelection() {
        String chatId = "12345";
        String tutorId = "tutor-id";
        List<SheduleDTO> schedules = Arrays.asList(new SheduleDTO(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(11, 0), false));

        when(restTemplate.getForObject(anyString(), eq(SheduleDTO[].class)))
                .thenReturn(schedules.toArray(new SheduleDTO[0]));
        when(messageService.createSelectionMessage(anyString(), anyString(), any())).thenReturn(new SendMessage());
        bookLessonAction.handleScheduleSelection(chatId, tutorId);
        verify(messageService, times(1)).sendMessageToBot(any(SendMessage.class));
    }

    @Test
    public void testBookLesson() throws Exception {
        String chatId = "12345";
        String scheduleId = UUID.randomUUID().toString();
        String userUuid = UUID.randomUUID().toString();

        when(userSessionService.getUserUUID(chatId)).thenReturn(userUuid);

        BookingRequestDTO bookingRequestDTO = new BookingRequestDTO(UUID.fromString(userUuid), UUID.fromString(scheduleId));

        bookLessonAction.bookLesson(chatId, scheduleId);

        verify(userKafkaProducer, times(1)).sendBookingRequest(any(BookingRequestDTO.class));
        verify(messageService, times(1)).sendMessageToBot(any(SendMessage.class));
    }

    @Test
    public void testFetchEntitiesFromServiceHandlesErrorsGracefully() {
        String chatId = "12345";
        String url = "http://dummy-url";

        when(restTemplate.getForObject(eq(url), eq(SubjectDTO[].class)))
                .thenThrow(new RestClientException("Service is down"));

        List<SubjectDTO> result = bookLessonAction.getAllSubjectsFromUserService(chatId);

        assertTrue(result.isEmpty());
        verify(messageService, times(1)).sendErrorMessage(eq(chatId), eq("Не удалось получить список предметов."));
    }
}
