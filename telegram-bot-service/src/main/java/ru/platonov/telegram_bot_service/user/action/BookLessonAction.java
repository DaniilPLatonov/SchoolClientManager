package ru.platonov.telegram_bot_service.user.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.platonov.telegram_bot_service.dto.BookingRequestDTO;
import ru.platonov.telegram_bot_service.dto.SheduleDTO;
import ru.platonov.telegram_bot_service.dto.SubjectDTO;
import ru.platonov.telegram_bot_service.dto.TutorDTO;
import ru.platonov.telegram_bot_service.service.MessageService;
import ru.platonov.telegram_bot_service.service.UserSessionService;
import ru.platonov.telegram_bot_service.user.kafka.UserKafkaProducer;
import ru.platonov.telegram_bot_service.user.state.UserStateService;

import java.util.*;
import java.util.function.Function;

@Component
public class BookLessonAction implements UserAction {

    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(BookLessonAction.class);
    private final UserSessionService userSessionService;
    private final UserKafkaProducer userKafkaProducer;
    private final MessageService messageService;
    private final UserStateService userStateService;


    private String bookingServiceScheduleUrl;

    private final String userServiceSubjectUrl;

    private String userServiceTutorUrl;


    public BookLessonAction(RestTemplate restTemplate, UserSessionService userSessionService,
                            UserKafkaProducer userKafkaProducer,
                            MessageService messageService, UserStateService userStateService,
                            @Value("${services.user-service.urlForSubject}") String userServiceSubjectUrl,
                            @Value("${services.booking-service.urlForShedule}") String bookingServiceScheduleUrl,
                            @Value("${services.user-service.urlForTutor}") String userServiceTutorUrl) {

        this.restTemplate = restTemplate;
        this.userSessionService = userSessionService;
        this.userKafkaProducer = userKafkaProducer;
        this.messageService = messageService;
        this.userStateService = userStateService;
        this.userServiceSubjectUrl = userServiceSubjectUrl;
        this.bookingServiceScheduleUrl = bookingServiceScheduleUrl;
        this.userServiceTutorUrl = userServiceTutorUrl;
    }

    public enum CallbackAction {
        BOOK_LESSON,
        SUBJECT_SELECTED,
        TUTOR_SELECTED,
        SCHEDULE_SELECTED,
        UNKNOWN
    }

    @Override
    public void executeForMessage(Update update) {

    }

    @Override
    public void executeForCallBack(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String chatId = callbackQuery.getMessage().getChatId().toString();
        String callbackData = callbackQuery.getData();
        CallbackAction action = getCallbackAction(callbackData);
        switch (action) {
            case BOOK_LESSON:
                handleSubjectSelection(chatId);
                break;
            case SUBJECT_SELECTED:
                handleTutorSelection(chatId, extractId(callbackData, "SUBJECT_"));
                break;
            case TUTOR_SELECTED:
                handleScheduleSelection(chatId, extractId(callbackData, "TUTOR_"));
                break;
            case SCHEDULE_SELECTED:
                bookLesson(chatId, extractId(callbackData, "SCHEDULE_"));
                break;
            default:
                logger.warn("Неизвестное значение callbackData: {} для chatId: {}", callbackData, chatId);
                break;
        }
    }

    private String extractId(String callbackData, String prefix) {
        return callbackData.substring(prefix.length());
    }

    private static final Map<String, CallbackAction> ACTION_MAP = Map.of(
            "Записаться на урок", CallbackAction.BOOK_LESSON,
            "SUBJECT_", CallbackAction.SUBJECT_SELECTED,
            "TUTOR_", CallbackAction.TUTOR_SELECTED,
            "SCHEDULE_", CallbackAction.SCHEDULE_SELECTED
    );

    public CallbackAction getCallbackAction(String callbackData) {
        return ACTION_MAP.entrySet()
                .stream()
                .filter(entry -> callbackData.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(CallbackAction.UNKNOWN);
    }

    public void handleSubjectSelection(String chatId) {
        List<SubjectDTO> subjects = getAllSubjectsFromUserService(chatId);
        if (subjects.isEmpty()) {
            return;
        }
        SendMessage subjectMessage = createSubjectSelectionMessage(chatId, subjects);
        messageService.sendMessageToBot(subjectMessage);
    }

    public void handleTutorSelection(String chatId, String subjectId) {
        List<TutorDTO> tutors = getTutorsBySubjectFromUserService(subjectId, chatId);
        if (tutors.isEmpty()) {
            return;
        }
        SendMessage tutorMessage = createTutorSelectionMessage(chatId, tutors);
        messageService.sendMessageToBot(tutorMessage);
    }

    public void handleScheduleSelection(String chatId, String tutorId) {
        List<SheduleDTO> schedules = getSchedulesByTutorFromBookingService(tutorId, chatId);
        if (schedules.isEmpty()) {
            return;
        }
        SendMessage scheduleMessage = createScheduleSelectionMessage(chatId, schedules);
        messageService.sendMessageToBot(scheduleMessage);
    }


    public List<SubjectDTO> getAllSubjectsFromUserService(String chatId) {
        return fetchEntitiesFromService(userServiceSubjectUrl, SubjectDTO[].class, "Не удалось получить список предметов.", chatId);
    }

    private List<TutorDTO> getTutorsBySubjectFromUserService(String subjectId, String chatId) {
        String url = userServiceTutorUrl + subjectId + "/tutors";
        return fetchEntitiesFromService(url, TutorDTO[].class, "Не удалось получить список преподавателей для предмета.", chatId);
    }

    private List<SheduleDTO> getSchedulesByTutorFromBookingService(String tutorId, String chatId) {
        String url = bookingServiceScheduleUrl + tutorId;
        return fetchEntitiesFromService(url, SheduleDTO[].class, "Не удалось получить расписание для преподавателя.", chatId);
    }

    private <T> List<T> fetchEntitiesFromService(String url, Class<T[]> responseType, String errorMessage, String chatId) {
        try {
            T[] data = restTemplate.getForObject(url, responseType);
            if (data == null) {
                logger.warn("Получен null от сервиса для URL: {}", url);
                userStateService.clearUserState(chatId);
                messageService.sendErrorMessage(chatId, errorMessage);
                messageService.showMainMenu(chatId);
                return Collections.emptyList();
            }
            return Arrays.asList(data);
        } catch (RestClientException e) {
            logger.error("Ошибка при запросе к сервису: {} - {}", url, e.getMessage(), e);
            userStateService.clearUserState(chatId);
            messageService.sendErrorMessage(chatId, errorMessage);
            messageService.showMainMenu(chatId);
            return Collections.emptyList();
        }
    }


    private <T> SendMessage createSelectionMessage(String chatId, String header, List<T> items, Function<T, String[]> buttonMapper) {
        String[][] keyboardButtons = items.stream()
                .map(buttonMapper)
                .toArray(String[][]::new);
        return messageService.createSelectionMessage(chatId, header, keyboardButtons);
    }

    private SendMessage createSubjectSelectionMessage(String chatId, List<SubjectDTO> subjects) {
        return createSelectionMessage(chatId, "Выберите предмет:", subjects,
                subject -> new String[]{subject.getName(), "SUBJECT_" + subject.getId()});
    }

    private SendMessage createTutorSelectionMessage(String chatId, List<TutorDTO> tutors) {
        return createSelectionMessage(chatId, "Выберите преподавателя:", tutors,
                tutor -> new String[]{tutor.getName(), "TUTOR_" + tutor.getId()});
    }

    private SendMessage createScheduleSelectionMessage(String chatId, List<SheduleDTO> schedules) {
        return createSelectionMessage(chatId, "Выберите время для урока:", schedules,
                schedule -> new String[]{schedule.getDate() + " " + schedule.getStartTime(), "SCHEDULE_" + schedule.getId()});
    }

    public void bookLesson(String chatId, String scheduleId) {
        String userUuid = userSessionService.getUserUUID(chatId);
        SendMessage message = new SendMessage();
        if (userUuid != null && scheduleId != null) {
            BookingRequestDTO bookingRequestDTO = new BookingRequestDTO(UUID.fromString(userUuid), UUID.fromString(scheduleId));
            try {
                userKafkaProducer.sendBookingRequest(bookingRequestDTO);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            message.setChatId(chatId);
            message.setText("Ваш урок успешно забронирован!");
            messageService.sendMessageToBot(message);
        } else {
            logger.warn("UUID пользователя не найден для chatId: {}", chatId);
            message.setChatId(chatId);
            message.setText("Произошла ошибка. Попробуйте снова позже.");
            messageService.sendMessageToBot(message);
        }
        messageService.showMainMenu(chatId);
    }
}
