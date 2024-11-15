package ru.platonov.telegram_bot_service.user.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.platonov.telegram_bot_service.dto.LoginDTO;
import ru.platonov.telegram_bot_service.dto.LoginResponseDTO;
import ru.platonov.telegram_bot_service.service.MessageService;
import ru.platonov.telegram_bot_service.service.UserSessionService;
import ru.platonov.telegram_bot_service.user.state.UserStateService;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Component
public class LoginAction implements UserAction {

    private final MessageService messageService;
    private final UserSessionService userSessionService;
    private final RestTemplate restTemplate;
    private final UserStateService userStateService;
    private final Map<String, LoginDTO> loginStates = new HashMap<>();
    private static final String ERROR_INVALID_EMAIL = "Пожалуйста, введите корректный email.";
    private static final String ERROR_INVALID_PASSWORD = "Пароль должен содержать не менее 6 символов.";



    @Autowired
    public LoginAction(MessageService messageService, UserSessionService userSessionService, RestTemplate restTemplate, UserStateService userStateService) {
        this.messageService = messageService;
        this.userSessionService = userSessionService;
        this.restTemplate = restTemplate;
        this.userStateService = userStateService;
    }

    @Override
    public void executeForMessage(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        String messageText = update.getMessage().getText();
        processLogin(chatId, messageText);
    }

    @Override
    public void executeForCallBack(Update update) {
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        String callbackData = update.getCallbackQuery().getData();
        processLogin(chatId, callbackData);
    }

    public void processLogin(String chatId, String callbackData) {
        if (loginStates.containsKey(chatId)) {
            handleLoginSteps(chatId, callbackData);
        } else {
            startLogin(chatId);
        }
    }

    private void startLogin(String chatId) {
        messageService.askForUserEmail(chatId);
        loginStates.put(chatId, new LoginDTO());
    }

    private void handleLoginSteps(String chatId, String input) {
        LoginDTO loginDto = loginStates.get(chatId);

        if (loginDto.getEmail() == null) {
            processAndValidateInput(chatId, input, this::isValidEmail, loginDto::setEmail, ERROR_INVALID_EMAIL,
                    () -> messageService.askForUserPassword(chatId));
        } else if (loginDto.getPassword() == null) {
            processAndValidateInput(chatId, input, this::isValidPassword, loginDto::setPassword, ERROR_INVALID_PASSWORD,
                    () -> completeLogin(chatId, loginDto));
        }
    }

    private void processAndValidateInput(String chatId, String input,
                                         Predicate<String> validation,
                                         Consumer<String> setValue,
                                         String errorMessage,
                                         Runnable onSuccess) {
        if (!validation.test(input)) {
            messageService.sendTextMessage(chatId, errorMessage);
            loginStates.remove(chatId);
            userStateService.clearUserState(chatId);
            messageService.showLoginMenu(chatId);
            return;
        }

        setValue.accept(input);
        onSuccess.run();
    }


    private boolean isValidEmail(String email) {
        return email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6;
    }

    private void completeLogin(String chatId, LoginDTO loginDto) {
        try {
            messageService.sendTextMessage(chatId, "Проверка данных...");
            String url = "http://user-service:8080/api/users/login";

            ResponseEntity<LoginResponseDTO> response = restTemplate.postForEntity(url, loginDto, LoginResponseDTO.class);


            if (response.getBody() == null) {
                messageService.sendErrorLogin(chatId);
                return;
            }

            LoginResponseDTO responseBody = response.getBody();

            if (responseBody == null) {
                messageService.sendErrorLogin(chatId);
                return;
            }

            if (responseBody.isSuccess()) {
                userSessionService.saveUserUUID(chatId, responseBody.getUuid());
                messageService.showMainMenu(chatId);
            }
        } catch (Exception e) {
            messageService.sendErrorLogin(chatId);
            messageService.showLoginMenu(chatId);
            e.printStackTrace();
        } finally {
            loginStates.remove(chatId);
        }
    }
}
