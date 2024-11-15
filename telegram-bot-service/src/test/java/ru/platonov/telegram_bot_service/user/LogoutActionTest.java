package ru.platonov.telegram_bot_service.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.platonov.telegram_bot_service.service.MessageService;
import ru.platonov.telegram_bot_service.service.UserSessionService;
import ru.platonov.telegram_bot_service.user.action.LogoutAction;

import static org.mockito.Mockito.*;

class LogoutActionTest {

    @Mock
    private MessageService messageService;

    @Mock
    private UserSessionService userSessionService;

    @InjectMocks
    private LogoutAction logoutAction;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testExecuteForCallback_UserIsAuthorized() {
        String chatId = "12345";
        Update update = mockUpdateWithChatId(chatId);
        when(userSessionService.hasUserUUID(chatId)).thenReturn(true);
        logoutAction.executeForCallBack(update);
        verify(userSessionService).removeUserUUID(chatId);
        verify(messageService).sendTextMessage(chatId, "Вы успешно вышли из системы.");
        verify(messageService).showLoginMenu(chatId);
    }

    @Test
    void testExecuteForCallback_UserIsNotAuthorized() {
        String chatId = "12345";
        Update update = mockUpdateWithChatId(chatId);

        when(userSessionService.hasUserUUID(chatId)).thenReturn(false);

        logoutAction.executeForCallBack(update);

        verify(userSessionService, never()).removeUserUUID(chatId);
        verify(messageService).sendTextMessage(chatId, "Вы уже не авторизованы.");
        verify(messageService).showLoginMenu(chatId);
    }

    private Update mockUpdateWithChatId(String chatId) {
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);

        when(update.getCallbackQuery()).thenReturn(callbackQuery);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(Long.valueOf(chatId));

        return update;
    }
}
