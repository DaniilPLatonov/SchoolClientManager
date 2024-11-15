package ru.platonov.telegram_bot_service.user;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.platonov.telegram_bot_service.dto.LoginDTO;
import ru.platonov.telegram_bot_service.dto.LoginResponseDTO;
import ru.platonov.telegram_bot_service.service.MessageService;
import ru.platonov.telegram_bot_service.service.UserSessionService;
import ru.platonov.telegram_bot_service.user.action.LoginAction;
import ru.platonov.telegram_bot_service.user.state.UserStateService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoginActionTest {

    @Mock
    private MessageService messageService;
    @Mock
    private UserSessionService userSessionService;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private UserStateService userStateService;

    @InjectMocks
    private LoginAction loginAction;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void shouldStartLoginProcessWhenNewUser() {
        Update update = mockUpdateWithMessage("123", "start");

        loginAction.executeForMessage(update);

        verify(messageService).askForUserEmail("123");
    }

    @Test
    void shouldHandleInvalidEmailInput() {
        loginAction.executeForMessage(mockUpdateWithMessage("123", "start")); // Начинаем процесс логина

        Update update = mockUpdateWithMessage("123", "invalid-email");
        loginAction.executeForMessage(update);

        verify(messageService).sendTextMessage("123", "Пожалуйста, введите корректный email.");
        verify(userStateService).clearUserState("123");
        verify(messageService).showLoginMenu("123");
    }


    @Test
    void shouldHandleValidEmailAndAskForPassword() {
        Update update = mockUpdateWithMessage("123", "test@example.com");

        loginAction.executeForMessage(update);
        loginAction.executeForMessage(update);

        verify(messageService).askForUserPassword("123");
    }

    @Test
    void shouldHandleInvalidPasswordInput() {
        loginAction.executeForMessage(mockUpdateWithMessage("123", "start")); // Начинаем процесс логина
        loginAction.executeForMessage(mockUpdateWithMessage("123", "test@example.com")); // Вводим корректный email

        Update update = mockUpdateWithMessage("123", "12345");
        loginAction.executeForMessage(update);

        verify(messageService).sendTextMessage("123", "Пароль должен содержать не менее 6 символов.");
        verify(userStateService).clearUserState("123");
        verify(messageService).showLoginMenu("123");
    }

    private Update mockUpdateWithMessage(String chatId, String text) {
        Update update = new Update();
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(Long.valueOf(chatId));
        when(message.getText()).thenReturn(text);
        update.setMessage(message);
        return update;
    }

    @Test
    void shouldCompleteLoginSuccessfully() {
        LoginDTO loginDto = new LoginDTO();
        loginDto.setEmail("test@example.com");
        loginDto.setPassword("validPassword");

        LoginResponseDTO responseDto = new LoginResponseDTO();
        responseDto.setSuccess(true);
        responseDto.setUuid("user-uuid");

        String loginUrl = "http://user-service:8080/api/users/login";

        when(restTemplate.postForEntity(eq(loginUrl), any(LoginDTO.class), eq(LoginResponseDTO.class)))
                .thenReturn(new ResponseEntity<>(responseDto, HttpStatus.OK));

        Update startUpdate = mockUpdateWithMessage("123", "start");
        loginAction.executeForMessage(startUpdate);

        Update emailUpdate = mockUpdateWithMessage("123", "test@example.com");
        loginAction.executeForMessage(emailUpdate);

        Update passwordUpdate = mockUpdateWithMessage("123", "validPassword");
        loginAction.executeForMessage(passwordUpdate);

        verify(messageService).sendTextMessage("123", "Проверка данных...");
        verify(userSessionService).saveUserUUID("123", "user-uuid");
        verify(messageService).showMainMenu("123");
        verify(messageService, never()).sendErrorLogin("123");

    }


    @Test
    void shouldHandleErrorWhenLoginFails() {
        when(restTemplate.postForEntity(anyString(), any(LoginDTO.class), eq(LoginResponseDTO.class)))
                .thenThrow(new RuntimeException("Login service unavailable"));

        Update startUpdate = mockUpdateWithMessage("123", "start");
        loginAction.executeForMessage(startUpdate);

        Update emailUpdate = mockUpdateWithMessage("123", "test@example.com");
        loginAction.executeForMessage(emailUpdate);

        Update passwordUpdate = mockUpdateWithMessage("123", "validPassword");
        loginAction.executeForMessage(passwordUpdate);

        verify(messageService).sendErrorLogin("123");
    }
}


