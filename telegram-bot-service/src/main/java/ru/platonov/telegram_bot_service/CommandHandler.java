package ru.platonov.telegram_bot_service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.platonov.telegram_bot_service.service.MessageService;
import ru.platonov.telegram_bot_service.user.handler.UserCommandHandler;
import java.util.HashMap;
import java.util.Map;

@Component
public class CommandHandler {

    private final UserCommandHandler userCommandHandler;
    private final MessageService messageService;
    private final Map<String, UserState> userStates = new HashMap<>();

    public CommandHandler(UserCommandHandler userCommandHandler, MessageService messageService) {
        this.userCommandHandler = userCommandHandler;
        this.messageService = messageService;
    }

    private enum UserState {
        INITIAL, STUDENT, TEACHER, ADMIN
    }

    public void handleCommand(Update update) {
        String chatId = update.getMessage().getChatId().toString();
        String messageText = update.getMessage().getText();
        UserState currentState = userStates.getOrDefault(chatId, UserState.INITIAL);
        switch (currentState) {
            case INITIAL -> handleInitialCommand(chatId, messageText);
            case STUDENT -> userCommandHandler.handleCommand(update);
            case TEACHER, ADMIN -> handleAdminTeacherCommand(chatId);
            default -> handleUnknownCommand(chatId);
        }
    }

    private void handleInitialCommand(String chatId, String messageText) {
        if ("/start".equals(messageText)) {
            messageService.sendRoleSelectionMessage(chatId);
        } else {
            messageService.sendErrorRole(chatId);
            messageService.sendRoleSelectionMessage(chatId);
        }
    }

    public void handleCallbackQuery(Update update) {
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        String callbackData = update.getCallbackQuery().getData();

        UserState currentState = userStates.get(chatId);

        if (currentState == null) {
            initializeUserState(chatId, callbackData);
        } else {
            switch (currentState) {
                case STUDENT -> userCommandHandler.handleCallbackQuery(update);
                case TEACHER -> handleTeacherCallback(chatId);
                case ADMIN -> handleAdminCallback(chatId);
            }
        }
    }

    private void initializeUserState(String chatId, String role) {
        UserState state = switch (role) {
            case "Ученик" -> UserState.STUDENT;
            case "Преподаватель" -> UserState.TEACHER;
            case "Администратор" -> UserState.ADMIN;
            default -> null;
        };
        if (state != null) {
            userStates.put(chatId, state);
            messageService.showLoginMenu(chatId);
        } else {
            messageService.sendRoleSelectionMessage(chatId);
        }
    }

    private void handleAdminTeacherCommand(String chatId) {
        messageService.sendErrorEnter(chatId);
        messageService.showLoginMenu(chatId);
    }

    private void handleTeacherCallback(String chatId) {
        // TODO: Логика для обработки callback для "Преподаватель"
    }

    private void handleAdminCallback(String chatId) {
        // TODO: Логика для обработки callback для "Администратор"
    }

    private void handleUnknownCommand(String chatId) {
        messageService.sendErrorEnter(chatId);
        messageService.showLoginMenu(chatId);
    }
}
