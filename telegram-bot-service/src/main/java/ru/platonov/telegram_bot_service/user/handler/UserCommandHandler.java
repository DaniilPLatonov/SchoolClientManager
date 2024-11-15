package ru.platonov.telegram_bot_service.user.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.platonov.telegram_bot_service.service.MessageService;
import ru.platonov.telegram_bot_service.service.UserSessionService;
import ru.platonov.telegram_bot_service.user.action.*;
import ru.platonov.telegram_bot_service.user.state.UserState;
import ru.platonov.telegram_bot_service.user.state.UserStateService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class UserCommandHandler {

    private final Map<UserActionType, UserAction> userActions = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(UserCommandHandler.class);
    private final UserStateService userStateService;
    private final MessageService messageService;
    private final UserSessionService userSessionService;

    @Autowired
    public UserCommandHandler(RegisterAction registerAction,
                              LoginAction loginAction,
                              BookLessonAction bookLessonAction,
                              ViewLessonsAction viewLessonsAction,
                              CancelLessonAction cancelLessonAction,
                              LogoutAction logoutAction, UserStateService userStateService, MessageService messageService, UserSessionService userSessionService) {
        this.userStateService = userStateService;
        this.messageService = messageService;
        this.userSessionService = userSessionService;
        initializeUserActions(registerAction, loginAction, bookLessonAction, viewLessonsAction, cancelLessonAction, logoutAction);
    }

    private void initializeUserActions(RegisterAction registerAction,
                                       LoginAction loginAction,
                                       BookLessonAction bookLessonAction,
                                       ViewLessonsAction viewLessonsAction,
                                       CancelLessonAction cancelLessonAction,
                                       LogoutAction logoutAction) {
        userActions.put(UserActionType.REGISTER, registerAction);
        userActions.put(UserActionType.LOGIN, loginAction);
        userActions.put(UserActionType.BOOK_LESSON, bookLessonAction);
        userActions.put(UserActionType.VIEW_LESSONS, viewLessonsAction);
        userActions.put(UserActionType.CANCEL_LESSON, cancelLessonAction);
        userActions.put(UserActionType.LOGOUT, logoutAction);
    }

    public void handleCommand(Update update) {
        String chatId = getChatIdFromUpdate(update).orElse(null);

        if (chatId == null) {
            logger.warn("No chat ID found in update");
            return;
        }

        String messageText = update.getMessage().getText();
        if (isUnauthorized(chatId, messageText)) {
            handleUnauthorizedUser(chatId);
            return;
        }
        if (userStateService.hasUserState(chatId)) {
            handleUserState(chatId, update);
        } else {
            handleInitialCommand(chatId, messageText, update);
        }
    }

    private boolean isUnauthorized(String chatId, String messageText) {
        Set<String> entryCommands = Set.of("Регистрация", "Вход");
        return !entryCommands.contains(messageText) &&
                !userSessionService.hasUserUUID(chatId) &&
                !userStateService.hasUserState(chatId);
    }

    private void handleUnauthorizedUser(String chatId) {
        messageService.sendTextMessage(chatId, "Ошибка: Вы не авторизованы.\n" +
                "Пожалуйста, зарегистрируйтесь или войдите.");
        messageService.showLoginMenu(chatId);
    }


    public void handleCallbackQuery(Update update) {
        if (isCallbackQueryValid(update)) {
            String chatId = getChatIdFromCallbackQuery(update).orElse(null);

            if (chatId == null) {
                logger.warn("No chat ID found in callback query");
                return;
            }
            String callbackData = update.getCallbackQuery().getData();
            if (!callbackData.equals("Регистрация") && !callbackData.equals("Вход") && !userSessionService.hasUserUUID(chatId) && !userStateService.hasUserState(chatId)) {
                handleUnauthorizedUser(chatId);
                return;
            }
            if (userStateService.hasUserState(chatId)) {
                handleUserState(chatId, update);
            } else {
                processCallbackAction(chatId, callbackData, update);
            }
        } else {
            logger.warn("Received invalid callback query.");
        }
    }


    private void processCallbackAction(String chatId, String callbackData, Update update) {
        UserActionType actionType = UserActionType.fromString(callbackData);
        UserAction userAction = userActions.get(actionType);

        if (userAction != null) {
            try {
                userAction.executeForCallBack(update);
                userStateService.setUserState(chatId, callbackData);
                UserState userState = userStateService.getUserState(chatId).orElse(null);
                if (userState != null && isActionComplete(userState)) {
                    userStateService.clearUserState(chatId);
                }
            } catch (Exception e) {
                logger.error("Error processing callback query for chatId: {}, error: {}", chatId, e.getMessage(), e);
            }
        } else {
            logger.warn("Unknown callback data: {} for chatId: {}", callbackData, chatId);
        }
    }


    private void handleInitialCommand(String chatId, String messageText, Update update) {
        UserActionType actionType = UserActionType.fromString(messageText);
        Optional<UserAction> userActionOpt = Optional.ofNullable(userActions.get(actionType));

        userActionOpt.ifPresentOrElse(
                userAction -> {
                    try {
                        userAction.executeForMessage(update);
                        userStateService.setUserState(chatId, messageText);
                    } catch (Exception e) {
                        logger.error("Error processing command '{}' for chatId: {}", messageText, chatId, e);
                    }
                },
                () -> logger.warn("Unknown command received from chatId: {}", chatId)
        );
    }


    private void handleUserState(String chatId, Update update) {
        UserState userState = userStateService.getUserState(chatId).orElse(null);

        if (userState == null) {
            logger.warn("No UserState found for chatId: {}", chatId);
            return;
        }
        UserActionType actionType = UserActionType.fromString(userState.getAction());
        UserAction userAction = userActions.get(actionType);

        if (userAction != null) {
            try {
                if (update.hasCallbackQuery()) {
                    userAction.executeForCallBack(update);
                } else {
                    userAction.executeForMessage(update);
                }
                userStateService.nextStep(chatId);
                if (isActionComplete(userState)) {
                    userStateService.clearUserState(chatId);
                }
            } catch (Exception e) {
                logger.error("Error processing user state action '{}' for chatId: {}", userState.getAction(), chatId, e);
            }
        } else {
            logger.warn("No matching user state action found for action '{}' and chatId: {}", userState.getAction(), chatId);
        }
    }

    private boolean isActionComplete(UserState userState) {
        int step = userState.getStep();
        UserActionType actionType = UserActionType.fromString(userState.getAction());

        return switch (actionType) {
            case REGISTER, BOOK_LESSON -> step >= 3;
            case LOGIN -> step >= 2;
            case VIEW_LESSONS, LOGOUT -> step >= 0;
            case CANCEL_LESSON -> step >= 1;
        };
    }

    private Optional<String> getChatIdFromUpdate(Update update) {
        return Optional.ofNullable(update.getMessage())
                .map(message -> message.getChatId().toString());
    }

    private Optional<String> getChatIdFromCallbackQuery(Update update) {
        return Optional.ofNullable(update.getCallbackQuery())
                .map(callback -> callback.getMessage().getChatId().toString());
    }

    private boolean isCallbackQueryValid(Update update) {
        boolean valid = update.hasCallbackQuery() && update.getCallbackQuery().getData() != null;
        logger.info("Callback query validation result: {}", valid);
        return valid;
    }
}
