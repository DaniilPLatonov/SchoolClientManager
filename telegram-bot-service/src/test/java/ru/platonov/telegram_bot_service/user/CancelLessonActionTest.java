package ru.platonov.telegram_bot_service.user;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.platonov.telegram_bot_service.bot.TelegramBot;
import ru.platonov.telegram_bot_service.service.MessageService;
import ru.platonov.telegram_bot_service.service.UserSessionService;
import ru.platonov.telegram_bot_service.user.action.CancelLessonAction;
import ru.platonov.telegram_bot_service.user.kafka.UserKafkaProducer;
import ru.platonov.telegram_bot_service.dto.BookingInfoDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

class CancelLessonActionTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MessageService messageService;

    @Mock
    private UserKafkaProducer userKafkaProducer;

    @Mock
    private UserSessionService userSessionService;

    @InjectMocks
    private CancelLessonAction cancelLessonAction;

    @MockBean
    private TelegramBotsApi telegramBotsApi;

    @MockBean
    private TelegramBot telegramBot;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void shouldShowBookingsForCancellationWhenBookingsExist() {
        String chatId = "12345";
        String userUuid = "user-uuid";
        List<BookingInfoDTO> bookings = createMockBookings();

        when(userSessionService.getUserUUID(chatId)).thenReturn(userUuid);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(new ParameterizedTypeReference<List<BookingInfoDTO>>() {})))
                .thenReturn(ResponseEntity.ok(bookings));


        cancelLessonAction.showUserBookingsForCancellation(chatId);

        verify(messageService, times(1)).sendMessageWithInlineKeyboard(eq(chatId), anyString(), anyList());
    }

    @Test
    public void shouldSendNoBookingsMessageWhenNoBookingsExist() {
        String chatId = "12345";
        String userUuid = "user-uuid";

        when(userSessionService.getUserUUID(chatId)).thenReturn(userUuid);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(new ParameterizedTypeReference<List<BookingInfoDTO>>() {})))
                .thenReturn(ResponseEntity.ok(Collections.emptyList()));

        cancelLessonAction.showUserBookingsForCancellation(chatId);

        verify(messageService, times(1)).sendTextMessage(chatId, "У вас нет предстоящих бронирований.");
    }

    @Test
    public void shouldHandleErrorWhenFetchingBookingsFails() {
        String chatId = "12345";
        String userUuid = "user-uuid";

        when(userSessionService.getUserUUID(chatId)).thenReturn(userUuid);
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                eq(new ParameterizedTypeReference<List<BookingInfoDTO>>() {})))
                .thenThrow(new RestClientException("Error"));

        cancelLessonAction.showUserBookingsForCancellation(chatId);

        // Проверяем, что sendTextMessage был вызван с userUuid, так как это используется в методе fetchUserBookings
        verify(messageService, times(1)).sendTextMessage(userUuid, "Произошла ошибка при получении списка бронирований.");
    }


    @Test
    public void shouldCancelBookingWhenValidCallbackIsReceived() {
        String chatId = "12345";
        String bookingId = "booking-123";

        doNothing().when(userKafkaProducer).sendBookingCancellation(bookingId);

        cancelLessonAction.cancelBooking(chatId, bookingId);

        verify(userKafkaProducer, times(1)).sendBookingCancellation(bookingId);
        verify(messageService, times(1)).sendTextMessage(chatId, "Ваше бронирование успешно отменено.");
    }

    @Test
    public void shouldHandleErrorWhenCancelBookingFails() {
        String chatId = "12345";
        String bookingId = "booking-123";

        doThrow(new RuntimeException("Kafka error")).when(userKafkaProducer).sendBookingCancellation(bookingId);

        cancelLessonAction.cancelBooking(chatId, bookingId);

        verify(messageService, times(1)).sendTextMessage(chatId, "Произошла ошибка при отмене бронирования. Попробуйте позже.");
    }

    @Test
    public void shouldProcessCallbackDataCorrectly() {
        String chatId = "12345";
        String callbackData = "cancel_booking-123";

        // Создаем моки для всех объектов, которые будем использовать
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class); // Мок для CallbackQuery
        Message message = mock(Message.class); // Мок для Message

        // Настроим поведение mock-объектов
        when(update.getCallbackQuery()).thenReturn(callbackQuery); // Возвращаем callbackQuery из update
        when(callbackQuery.getData()).thenReturn(callbackData); // Возвращаем callbackData из callbackQuery
        when(callbackQuery.getMessage()).thenReturn(message); // Возвращаем message из callbackQuery
        when(message.getChatId()).thenReturn(Long.valueOf(chatId)); // Возвращаем chatId из message

        // Выполним действие
        cancelLessonAction.executeForCallBack(update);

        // Проверяем, что метод отправки сообщения был вызван один раз с ожидаемым сообщением
        verify(messageService, times(1)).sendTextMessage(chatId, "Ваше бронирование успешно отменено.");
    }


    @Test
    public void shouldHandleUnknownCallbackDataGracefully() {
        String chatId = "12345";
        String callbackData = "unknown_callback";

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);

        when(callbackQuery.getData()).thenReturn(callbackData);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(Long.valueOf(chatId));


        Update update = mock(Update.class);
        when(update.getCallbackQuery()).thenReturn(callbackQuery);

        // Выполним действие
        cancelLessonAction.executeForCallBack(update);

        // Проверяем, что нужные методы были вызваны
        verify(messageService, times(0)).sendTextMessage(chatId, "Ваше бронирование успешно отменено.");
        verify(messageService, times(1)).sendTextMessage(chatId, "Неизвестный callback: unknown_callback");
    }


    private List<BookingInfoDTO> createMockBookings() {
        BookingInfoDTO booking1 = BookingInfoDTO.builder()
                .bookingId("booking-123")
                .subjectName("Math")
                .tutorName("John Doe")
                .date(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build();

        BookingInfoDTO booking2 = BookingInfoDTO.builder()
                .bookingId("booking-456")
                .subjectName("Physics")
                .tutorName("Jane Doe")
                .date(LocalDate.now())
                .startTime(LocalTime.of(12, 0))
                .endTime(LocalTime.of(13, 0))
                .build();

        return Arrays.asList(booking1, booking2);
    }
}
