package ru.platonov.telegram_bot_service.user.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.platonov.telegram_bot_service.service.MessageService;
import ru.platonov.telegram_bot_service.service.UserSessionService;
import ru.platonov.telegram_bot_service.dto.BookingInfoDTO;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ViewLessonsAction implements UserAction {

    private final RestTemplate restTemplate;
    private final MessageService messageService;
    private final UserSessionService userSessionService;
    private static final Logger logger = LoggerFactory.getLogger(ViewLessonsAction.class);

    private final String bookingServiceUrl;

    public ViewLessonsAction(RestTemplate restTemplate,
                             MessageService messageService,
                             UserSessionService userSessionService,
                             @Value("${services.booking-service.url}") String bookingServiceUrl) {
        this.restTemplate = restTemplate;
        this.messageService = messageService;
        this.userSessionService = userSessionService;
        this.bookingServiceUrl = bookingServiceUrl;
    }

    @Override
    public void executeForMessage(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        viewLessons(chatId);
    }

    @Override
    public void executeForCallBack(Update update) {
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        viewLessons(chatId);
    }

    public void viewLessons(String chatId) {
        String userUuid = userSessionService.getUserUUID(chatId);
        List<BookingInfoDTO> bookings = fetchUserBookings(userUuid);
        processAndSendBookings(chatId, userUuid, bookings);
    }


    private List<BookingInfoDTO> fetchUserBookings(String userUuid) {
        String url = bookingServiceUrl + "/user/" + userUuid;

        try {
            ResponseEntity<List<BookingInfoDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody() != null ? response.getBody() : Collections.emptyList();
            } else {
                logger.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("Error fetching bookings for user UUID {}: {}", userUuid, e.getMessage());
            return Collections.emptyList();
        }
    }





    private void processAndSendBookings(String chatId, String userUuid, List<BookingInfoDTO> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            messageService.sendTextMessage(chatId, "У вас нет предстоящих бронирований.");
            messageService.showMainMenu(chatId);
            return;
        }
        messageService.sendTextMessage(chatId, formatBookings(bookings));
        messageService.showMainMenu(chatId);
    }

    private String formatBookings(List<BookingInfoDTO> bookings) {
        return IntStream.range(0, bookings.size())
                .mapToObj(i -> (i + 1) + ". " + formatBooking(bookings.get(i)))
                .collect(Collectors.joining("\n\n"));
    }


    private String formatBooking(BookingInfoDTO booking) {
        return String.format("Предмет: %s\nПреподаватель: %s\nДата: %s\nНачало: %s\nКонец: %s",
                booking.getSubjectName(),
                booking.getTutorName(),
                booking.getDate(),
                booking.getStartTime(),
                booking.getEndTime());
    }
}
