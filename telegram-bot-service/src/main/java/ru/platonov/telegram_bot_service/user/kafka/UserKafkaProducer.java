package ru.platonov.telegram_bot_service.user.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.platonov.telegram_bot_service.dto.BookingRequestDTO;
import ru.platonov.telegram_bot_service.dto.UserDTO;
import java.util.Map;

@Component
public class UserKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(UserKafkaProducer.class);
    private static final String BOOKING_REQUESTS_TOPIC = "booking-requests";
    private static final String BOOKING_CANCEL_TOPIC = "cancel-booking";
    private static final String USER_REGISTRATIONS_TOPIC = "user-registrations";


    public UserKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendUserRegistration(UserDTO userDto) {
        try {
            Map<String, Object> requestMap = objectMapper.convertValue(userDto, Map.class);
            logger.debug("Sending user registration to Kafka: {}", requestMap);
            kafkaTemplate.send(USER_REGISTRATIONS_TOPIC, requestMap);
        } catch (Exception e) {
            logger.error("Error while sending user registration: {}", e.getMessage(), e);
        }
    }

    public void sendBookingRequest(BookingRequestDTO bookingRequestDTO) throws JsonProcessingException {
        logger.info("Sending booking request to Kafka: {}", bookingRequestDTO);
        String bookingRequestJson = objectMapper.writeValueAsString(bookingRequestDTO);
        kafkaTemplate.send(BOOKING_REQUESTS_TOPIC, bookingRequestJson);
    }


    public void sendBookingCancellation(String bookingId) {
        try {
            kafkaTemplate.send(BOOKING_CANCEL_TOPIC, bookingId);
            logger.info("Сообщение об отмене бронирования с ID: {} отправлено в Kafka.", bookingId);
        } catch (Exception e) {
            logger.error("Ошибка при отправке сообщения в Kafka: {}", e.getMessage());
        }
    }
}
