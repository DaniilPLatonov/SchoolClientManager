package ru.platonov.telegram_bot_service.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.platonov.telegram_bot_service.dto.UserDTO;
import ru.platonov.telegram_bot_service.service.MessageService;
import ru.platonov.telegram_bot_service.user.action.RegisterAction;
import ru.platonov.telegram_bot_service.user.kafka.UserKafkaProducer;
import ru.platonov.telegram_bot_service.user.state.UserStateService;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegisterActionTest {

    @Mock
    private MessageService messageService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserKafkaProducer userKafkaProducer;
    @Mock
    private UserStateService userStateService;

    @InjectMocks
    private RegisterAction registerAction;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldStartRegistrationProcessWhenNewUser() {
        Update update = mockUpdateWithMessage("123", "start");

        registerAction.executeForMessage(update);

        verify(messageService).sendTextMessage("123", "Пожалуйста, отправьте свое Имя и Фамилию для регистрации.");
    }

    @Test
    void shouldHandleInvalidNameInput() {
        registerAction.executeForMessage(mockUpdateWithMessage("123", "start")); // Начинаем процесс регистрации

        Update update = mockUpdateWithMessage("123", "invalid name that is too long for validation");
        registerAction.executeForMessage(update);

        verify(messageService).sendTextMessage("123", "Пожалуйста, введите корректное ФИО (только имя и фамилия, не более 20 символов).");
        verify(userStateService).clearUserState("123");
        verify(messageService).showLoginMenu("123");
    }

    @Test
    void shouldHandleValidNameAndAskForEmail() {
        Update update = mockUpdateWithMessage("123", "John Doe");

        registerAction.executeForMessage(update);
        registerAction.executeForMessage(update);  // Recheck to process the name input

        verify(messageService).askForUserEmail("123");
    }

    @Test
    void shouldHandleInvalidEmailInput() {
        registerAction.executeForMessage(mockUpdateWithMessage("123", "start"));
        registerAction.executeForMessage(mockUpdateWithMessage("123", "John Doe")); // Начинаем процесс регистрации
        registerAction.executeForMessage(mockUpdateWithMessage("123", "dsgthr")); // Вводим имя

        verify(messageService).sendTextMessage("123", "Пожалуйста, введите корректный email.");
        verify(userStateService).clearUserState("123");
        verify(messageService).showLoginMenu("123");
    }

    @Test
    void shouldHandleValidEmailAndAskForPassword() {
        registerAction.executeForMessage(mockUpdateWithMessage("123", "start"));
        registerAction.executeForMessage(mockUpdateWithMessage("123", "John Doe"));  // Имя
        registerAction.executeForMessage(mockUpdateWithMessage("123", "test@domain.com"));  // Email

        Update update = mockUpdateWithMessage("123", "validpassword");
        registerAction.executeForMessage(update);

        verify(messageService).askForUserPassword("123");
    }

    @Test
    void shouldHandleInvalidPasswordInput() {
        registerAction.executeForMessage(mockUpdateWithMessage("123", "start"));
        registerAction.executeForMessage(mockUpdateWithMessage("123", "John Doe"));
        registerAction.executeForMessage(mockUpdateWithMessage("123", "test@domain.com"));

        Update update = mockUpdateWithMessage("123", "12345"); // Invalid password
        registerAction.executeForMessage(update);

        verify(messageService).sendTextMessage("123", "Пароль должен содержать не менее 6 символов.");
        verify(userStateService).clearUserState("123");
        verify(messageService).showLoginMenu("123");
    }


    @Test
    void shouldCompleteRegistrationSuccessfully() {
        UserDTO userDto = new UserDTO();
        userDto.setName("John Doe");
        userDto.setEmail("john.doe@example.com");
        userDto.setPassword(passwordEncoder.encode("validpassword"));

        Update startUpdate = mockUpdateWithMessage("123", "start");
        registerAction.executeForMessage(startUpdate);

        Update nameUpdate = mockUpdateWithMessage("123", "John Doe");
        registerAction.executeForMessage(nameUpdate);

        Update emailUpdate = mockUpdateWithMessage("123", "john.doe@example.com");
        registerAction.executeForMessage(emailUpdate);

        Update passwordUpdate = mockUpdateWithMessage("123", "validpassword");
        registerAction.executeForMessage(passwordUpdate);

        // Используем refEq для сравнения объектов по значению полей
        verify(userKafkaProducer).sendUserRegistration(refEq(userDto));

        verify(messageService).sendSuccessRegistration("123");
        verify(messageService).showLoginMenu("123");
    }

    @Test
    void shouldHandleErrorWhenRegistrationFails() {
        doThrow(new RuntimeException("User registration failed")).when(userKafkaProducer).sendUserRegistration(any(UserDTO.class));

        registerAction.executeForMessage(mockUpdateWithMessage("123", "start"));
        registerAction.executeForMessage(mockUpdateWithMessage("123", "John Doe"));
        registerAction.executeForMessage(mockUpdateWithMessage("123", "john.doe@example.com"));
        registerAction.executeForMessage(mockUpdateWithMessage("123", "validpassword"));

        verify(messageService).sendErrorRegistration("123");
    }

    private Update mockUpdateWithMessage(String chatId, String text) {
        Update update = new Update();
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(Long.valueOf(chatId));
        when(message.getText()).thenReturn(text);
        update.setMessage(message);
        return update;
    }
}


