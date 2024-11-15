package ru.platonov.user_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import ru.platonov.user_service.dto.UserDTO;
import ru.platonov.user_service.kafka.BookingResponseListener;
import ru.platonov.user_service.service.UserService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = "/sql/fill_data_for_test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class BookingResponseListenerTest {

    @Autowired
    private BookingResponseListener bookingResponseListener;

    @MockBean
    private UserService userService;

    @SpyBean
    private ObjectMapper objectMapper;

    @Test
    public void testListenWithValidMessage() {
        Map<String, Object> validBookingMap = new HashMap<>();
        validBookingMap.put("name", "Test User");
        validBookingMap.put("email", "testuser@example.com");
        validBookingMap.put("password", "pipipi123");

        bookingResponseListener.listen(validBookingMap);

        verify(userService, times(1)).addUser(any(UserDTO.class));
    }

    @Test
    public void testListenWithEmptyMessage() {
        bookingResponseListener.listen(new HashMap<>());

        verify(userService, times(0)).addUser(any(UserDTO.class));
    }

    @Test
    public void testListenWithInvalidMessage() {
        Map<String, Object> invalidBookingMap = new HashMap<>();
        invalidBookingMap.put("invalidKey", "invalidValue");

        bookingResponseListener.listen(invalidBookingMap);

        verify(userService, times(0)).addUser(any(UserDTO.class));
    }
}
