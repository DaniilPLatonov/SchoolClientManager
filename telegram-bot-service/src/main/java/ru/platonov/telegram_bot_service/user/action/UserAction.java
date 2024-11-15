package ru.platonov.telegram_bot_service.user.action;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface UserAction {
    void executeForMessage(Update update);
    void executeForCallBack(Update update);
}
