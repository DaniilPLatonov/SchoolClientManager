package ru.platonov.telegram_bot_service.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserSessionService {

    private final Map<String, String> userUUIDs = new ConcurrentHashMap<>();

    public String getUserUUID(String chatId) {
        return userUUIDs.get(chatId);
    }

    public void saveUserUUID(String chatId, String uuid) {
        userUUIDs.put(chatId, uuid);
    }

    public void removeUserUUID(String chatId) {
        userUUIDs.remove(chatId);
    }

    public boolean hasUserUUID(String chatId) {
        return userUUIDs.containsKey(chatId);
    }
}
