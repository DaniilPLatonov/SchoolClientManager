package ru.platonov.telegram_bot_service.user.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.platonov.telegram_bot_service.dto.UserDTO;
import ru.platonov.telegram_bot_service.service.MessageService;
import ru.platonov.telegram_bot_service.user.kafka.UserKafkaProducer;
import ru.platonov.telegram_bot_service.user.state.UserStateService;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Component
public class RegisterAction implements UserAction {

    private final PasswordEncoder passwordEncoder;
    private final MessageService messageService;
    private final UserKafkaProducer userKafkaProducer;
    private final UserStateService userStateService;
    private final Map<String, UserDTO> userStates = new HashMap<>();

    private static final String ERROR_INVALID_NAME = "Пожалуйста, введите корректное ФИО (только имя и фамилия, не более 20 символов).";
    private static final String ERROR_INVALID_EMAIL = "Пожалуйста, введите корректный email.";
    private static final String ERROR_INVALID_PASSWORD = "Пароль должен содержать не менее 6 символов.";


    @Autowired
    public RegisterAction(PasswordEncoder passwordEncoder,
                          MessageService messageService, UserKafkaProducer userKafkaProducer, UserStateService userStateService) {
        this.passwordEncoder = passwordEncoder;
        this.messageService = messageService;
        this.userKafkaProducer = userKafkaProducer;
        this.userStateService = userStateService;
    }

    @Override
    public void executeForMessage(Update update) {
        if (update.hasMessage()) {
            String chatId = update.getMessage().getChatId().toString();
            String messageText = update.getMessage().getText();
            if (userStates.containsKey(chatId)) {
                handleRegistrationSteps(chatId, messageText);
            } else {
                startRegistration(chatId);
            }
        }
    }

    @Override
    public void executeForCallBack(Update update) {
        if (update.hasCallbackQuery()) {
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            String callbackData = update.getCallbackQuery().getData();

            if (userStates.containsKey(chatId)) {
                handleRegistrationSteps(chatId, callbackData);
            } else {
                startRegistration(chatId);
            }
        }
    }

    public void handleRegistrationSteps(String chatId, String messageText) {
        UserDTO userDto = userStates.get(chatId);
        if (userDto.getName() == null) {
            processInput(chatId, messageText, this::isValidName, userDto::setName,
                    ERROR_INVALID_NAME,
                    () -> messageService.askForUserEmail(chatId));
        } else if (userDto.getEmail() == null) {
            processInput(chatId, messageText, this::isValidEmail, userDto::setEmail,
                    ERROR_INVALID_EMAIL,
                    () -> messageService.askForUserPassword(chatId));
        } else if (userDto.getPassword() == null) {
            processInput(chatId, messageText, this::isValidPassword,
                    password -> userDto.setPassword(passwordEncoder.encode(password)),
                    ERROR_INVALID_PASSWORD,
                    () -> completeRegistration(chatId, userDto));
        }
    }


    private void processInput(String chatId, String messageText,
                              Predicate<String> validation,
                              Consumer<String> setValue,
                              String errorMessage,
                              Runnable onSuccess) {
        if (!validation.test(messageText)) {
            messageService.sendTextMessage(chatId, errorMessage);
            userStates.remove(chatId);
            userStateService.clearUserState(chatId);
            messageService.showLoginMenu(chatId);
            return;
        }

        setValue.accept(messageText);
        onSuccess.run();
    }


    public void completeRegistration(String chatId, UserDTO userDto) {
        try {
            userKafkaProducer.sendUserRegistration(userDto);
            messageService.sendSuccessRegistration(chatId);
            messageService.showLoginMenu(chatId);
        } catch (Exception e) {
            messageService.sendErrorRegistration(chatId);
            e.printStackTrace();
        } finally {
            userStates.remove(chatId);
        }
    }

    private void startRegistration(String chatId) {
        messageService.sendTextMessage(chatId, "Пожалуйста, отправьте свое Имя и Фамилию для регистрации.");
        userStates.put(chatId, new UserDTO());
    }

    private boolean isValidName(String name) {
        return name.matches("^[A-Za-zА-Яа-яёЁ\\s-]{2,20}$");
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6;
    }
}

