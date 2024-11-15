package ru.platonov.telegram_bot_service.user.handler;

import lombok.Getter;

@Getter
public enum UserActionType {
    REGISTER("Регистрация"),
    LOGIN("Вход"),
    BOOK_LESSON("Записаться на урок"),
    VIEW_LESSONS("Просмотреть предстоящие уроки"),
    CANCEL_LESSON("Отменить запись на урок"),
    LOGOUT("Выйти из системы");

    private final String value;

    UserActionType(String value) {
        this.value = value;
    }

    public static UserActionType fromString(String text) {
        for (UserActionType actionType : UserActionType.values()) {
            if (actionType.value.equals(text)) {
                return actionType;
            }
        }
        throw new IllegalArgumentException("No enum constant for value: " + text);
    }
}

