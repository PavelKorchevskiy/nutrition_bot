package nutrition.service;

import nutrition.model.user.RegistrationState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class RegistrationService {

    private MessageService messageService;
    private UserService userService;


    public SendMessage handleStart(long chatId, Locale locale) {
        userService.setUserState(chatId, RegistrationState.START);

        String welcomeText = messageService.get("welcome", locale);
        SendMessage message = new SendMessage(String.valueOf(chatId), welcomeText);
        message.setParseMode(ParseMode.MARKDOWN);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(messageService.get("menu.enter_params", locale));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(messageService.get("menu.calculations", locale));

        rows.add(row1);
        rows.add(row2);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    @Autowired
    public RegistrationService(MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }
}
