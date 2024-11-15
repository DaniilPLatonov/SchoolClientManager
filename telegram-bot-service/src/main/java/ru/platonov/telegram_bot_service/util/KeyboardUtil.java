package ru.platonov.telegram_bot_service.util;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.util.ArrayList;
import java.util.List;

public class KeyboardUtil {

    public static InlineKeyboardMarkup createInlineKeyboardMarkup(String... buttonNames) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (String name : buttonNames) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(name);
            button.setCallbackData(name);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            rows.add(row);
        }
        markup.setKeyboard(rows);
        return markup;
    }

    public static InlineKeyboardMarkup createInlineKeyboardMarkup(String[]... buttonData) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (String[] data : buttonData) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(data[0]);
            button.setCallbackData(data[1]);
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);
            rows.add(row);
        }
        markup.setKeyboard(rows);
        return markup;
    }
}

