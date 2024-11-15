package ru.platonov.telegram_bot_service.user.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.platonov.telegram_bot_service.service.MessageService;
import ru.platonov.telegram_bot_service.service.UserSessionService;
import ru.platonov.telegram_bot_service.user.handler.UserCommandHandler;
import ru.platonov.telegram_bot_service.bot.TelegramBot;

@Component
public class LogoutAction implements UserAction {

    private final MessageService messageService;
    private final UserSessionService userSessionService;

    public LogoutAction(MessageService messageService, UserSessionService userSessionService) {
        this.messageService = messageService;
        this.userSessionService = userSessionService;
    }

    @Override
    public void executeForMessage(Update update) {

    }

    @Override
    public void executeForCallBack(Update update) {
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        if (userSessionService.hasUserUUID(chatId)) {
            userSessionService.removeUserUUID(chatId);
            messageService.sendTextMessage(chatId, "Вы успешно вышли из системы.");
        } else {
            messageService.sendTextMessage(chatId, "Вы уже не авторизованы.");
        }
        messageService.showLoginMenu(chatId);
    }
}
