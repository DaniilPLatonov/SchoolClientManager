package ru.platonov.telegram_bot_service.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.platonov.telegram_bot_service.CommandHandler;


@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${bot.name}")
    private String botUsername;

    @Value("${bot.token}")
    private String botToken;

    private final CommandHandler commandHandler;


    @Autowired
    public TelegramBot(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @Override
    public void onUpdateReceived(Update update) {


        if (update.hasMessage() && update.getMessage().hasText()) {
            commandHandler.handleCommand(update);
        } else if (update.hasCallbackQuery()) {
            commandHandler.handleCallbackQuery(update);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
