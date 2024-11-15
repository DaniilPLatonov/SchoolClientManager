package ru.platonov.telegram_bot_service.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.platonov.telegram_bot_service.bot.TelegramBot;
import ru.platonov.telegram_bot_service.util.KeyboardUtil;

import java.util.List;

@Service
public class MessageService {

    private final TelegramBot telegramBot;

    public MessageService(@Lazy TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public void askForUserEmail(String chatId) {
        sendTextMessage(chatId, "Пожалуйста, отправьте свой email.");
    }

    public void askForUserPassword(String chatId) {
        sendTextMessage(chatId, "Пожалуйста, отправьте свой пароль.");
    }

    public void sendSuccessRegistration(String chatId) {
        sendTextMessage(chatId, "Вы успешно зарегистрированы!");
    }


    public void sendErrorRole(String chatId) {
        sendTextMessage(chatId, "Выберите роль для входа!");
    }

    public void sendErrorEnter(String chatId) {
        sendTextMessage(chatId, "Ошибка ввода!");
    }

    public void showMainMenu(String chatId) {
        sendTextMessageWithKeyboard(chatId, "Выберите действие:",
                KeyboardUtil.createInlineKeyboardMarkup(
                        "Записаться на урок",
                        "Просмотреть предстоящие уроки",
                        "Отменить запись на урок",
                        "Выйти из системы"
                ));
    }

    public void showLoginMenu(String chatId) {
        sendTextMessageWithKeyboard(chatId, "Выберите действие:",
                KeyboardUtil.createInlineKeyboardMarkup(
                        "Регистрация",
                        "Вход"
                ));
    }

    public void sendRoleSelectionMessage(String chatId) {
        sendTextMessageWithKeyboard(chatId, "Пожалуйста, выберите свою роль:",
                KeyboardUtil.createInlineKeyboardMarkup(
                        "Ученик",
                        "Преподаватель",
                        "Администратор"
                ));
    }

    public void sendTextMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        sendMessageToBot(message);
    }


    public void sendErrorLogin(String chatId) {
        sendTextMessage(chatId, "Ошибка при входе. Попробуйте позже.");
    }

    public void sendErrorRegistration(String chatId) {
        sendTextMessage(chatId, "Ошибка при регистрации. Попробуйте позже.");
    }

    private void sendTextMessageWithKeyboard(String chatId, String text, InlineKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);
        sendMessageToBot(message);
    }


    public void sendMessageWithInlineKeyboard(String chatId, String messageText, List<List<InlineKeyboardButton>> buttons) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(messageText);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(buttons);
        message.setReplyMarkup(keyboardMarkup);

        try {
            telegramBot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public SendMessage createSelectionMessage(String chatId, String text, String[][] keyboardButtons) {
        System.out.println("Before creating SendMessage");
        System.out.println("Before creating SendMessage");
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(KeyboardUtil.createInlineKeyboardMarkup(keyboardButtons));
        System.out.println("Message created successfully");
        return message;
    }


    public void sendErrorMessage(String chatId, String errorMessage) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(errorMessage);
        sendMessageToBot(message);
    }

    public void sendMessageToBot(SendMessage message) {
        try {
            telegramBot.execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при отправке сообщения: " + e.getMessage());
        }
    }

}
