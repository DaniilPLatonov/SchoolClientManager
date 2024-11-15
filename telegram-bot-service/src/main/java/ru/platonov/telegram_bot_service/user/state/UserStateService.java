package ru.platonov.telegram_bot_service.user.state;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class UserStateService {

    private static final Logger logger = LoggerFactory.getLogger(UserStateService.class);
    private final Map<String, UserState> userStates = new ConcurrentHashMap<>();

    public void setUserState(String chatId, String action) {
        userStates.put(chatId, new UserState(action));
        logger.info("Set user state for chatId: {}, action: {}", chatId, action);
    }

    public Optional<UserState> getUserState(String chatId) {
        return Optional.ofNullable(userStates.get(chatId));
    }

    public void clearUserState(String chatId) {
        Optional.ofNullable(userStates.remove(chatId))
                .ifPresentOrElse(
                        state -> logger.info("Cleared user state for chatId: {}", chatId),
                        () -> logger.warn("No user state found for chatId: {}", chatId)
                );
    }

    public boolean hasUserState(String chatId) {
        return userStates.containsKey(chatId);
    }

    public void nextStep(String chatId) {
        userStates.computeIfPresent(chatId, (key, state) -> {
            state.nextStep();
            return state;
        });
    }
}
