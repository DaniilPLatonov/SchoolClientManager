package ru.platonov.user_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.util.Map;
import ru.platonov.user_service.dto.UserDTO;
import ru.platonov.user_service.service.UserService;


@Service
public class BookingResponseListener {

    private final ObjectMapper objectMapper;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(BookingResponseListener.class);

    public BookingResponseListener(ObjectMapper objectMapper, UserService userService) {
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    @KafkaListener(topics = "user-registrations", groupId = "user-service")
    public void listen(Map<String, Object> bookingMap) {
        if (bookingMap == null || bookingMap.isEmpty()) {
            logger.error("Received null or empty user request message. Skipping processing.");
            return;
        }
        try {
            UserDTO userDTO = objectMapper.convertValue(bookingMap, UserDTO.class);
            userService.addUser(userDTO);
        } catch (Exception e) {
            logger.error("Failed to register user: {}", e);
        }
    }
}








