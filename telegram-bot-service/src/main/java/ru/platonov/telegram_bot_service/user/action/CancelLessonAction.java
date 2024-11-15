package ru.platonov.telegram_bot_service.user.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.platonov.telegram_bot_service.dto.BookingInfoDTO;
import ru.platonov.telegram_bot_service.service.MessageService;
import ru.platonov.telegram_bot_service.service.UserSessionService;
import ru.platonov.telegram_bot_service.user.kafka.UserKafkaProducer;

import java.util.ArrayList;
import java.util.List;

@Component
public class CancelLessonAction implements UserAction {

    private final RestTemplate restTemplate;
    private final MessageService messageService;
    private final UserKafkaProducer userKafkaProducer;
    private final UserSessionService userSessionService;
    private static final Logger logger = LoggerFactory.getLogger(CancelLessonAction.class);

    public CancelLessonAction(RestTemplate restTemplate, MessageService messageService,
                              UserKafkaProducer userKafkaProducer, UserSessionService userSessionService) {
        this.restTemplate = restTemplate;
        this.messageService = messageService;
        this.userKafkaProducer = userKafkaProducer;
        this.userSessionService = userSessionService;
    }

    @Override
    public void executeForMessage(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        showUserBookingsForCancellation(chatId);
    }

    @Override
    public void executeForCallBack(Update update) {
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        String callbackData = update.getCallbackQuery().getData();
        if ("Отменить запись на урок".equals(callbackData)) {
            showUserBookingsForCancellation(chatId);
        } else if (callbackData.startsWith("cancel_")) {
            String bookingId = callbackData.split("_")[1];
            cancelBooking(chatId, bookingId);
        } else {
            logger.warn("Неизвестный callback: {}", callbackData);
            messageService.sendTextMessage(chatId, "Неизвестный callback: " + callbackData);

        }
    }

    public void showUserBookingsForCancellation(String chatId) {
        String userUuid = userSessionService.getUserUUID(chatId);
        ResponseEntity<List<BookingInfoDTO>> response = fetchUserBookings(userUuid);

        if (response != null && response.getBody() != null && !response.getBody().isEmpty()) {
            sendBookingCancellationOptions(chatId, response.getBody());
        } else {
            messageService.sendTextMessage(chatId, "У вас нет предстоящих бронирований.");
            messageService.showMainMenu(chatId);
        }
    }

    private ResponseEntity<List<BookingInfoDTO>> fetchUserBookings(String userUuid) {
        try {
            String bookingServiceUrl = "http://booking-service:8080/api/bookings/user/" + userUuid;
            return restTemplate.exchange(
                    bookingServiceUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException e) {
            logger.error("Ошибка при запросе к booking-service: {}", e.getMessage());
            messageService.sendTextMessage(userUuid, "Произошла ошибка при получении списка бронирований.");
            return null;
        }
    }

    private void sendBookingCancellationOptions(String chatId, List<BookingInfoDTO> bookings) {
        StringBuilder messageBuilder = new StringBuilder("Выберите урок для отмены:\n\n");
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        for (int index = 0; index < bookings.size(); index++) {
            BookingInfoDTO booking = bookings.get(index);
            messageBuilder.append(String.format(
                    "%d. Предмет: %s\nПреподаватель: %s\nДата: %s\nНачало: %s\nКонец: %s\n\n",
                    index + 1,
                    booking.getSubjectName(),
                    booking.getTutorName(),
                    booking.getDate(),
                    booking.getStartTime(),
                    booking.getEndTime()));

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Отменить " + (index + 1));
            button.setCallbackData("cancel_" + booking.getBookingId());
            buttons.add(List.of(button));
        }
        messageService.sendMessageWithInlineKeyboard(chatId, messageBuilder.toString(), buttons);
    }

    public void cancelBooking(String chatId, String bookingId) {
        try {
            userKafkaProducer.sendBookingCancellation(bookingId);
            messageService.sendTextMessage(chatId, "Ваше бронирование успешно отменено.");
        } catch (Exception e) {
            logger.error("Ошибка при отправке сообщения в Kafka: {}", e.getMessage());
            messageService.sendTextMessage(chatId, "Произошла ошибка при отмене бронирования. Попробуйте позже.");
        }
        messageService.showMainMenu(chatId);
    }
}
