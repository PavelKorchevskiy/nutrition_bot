package nutrition.bot;

import nutrition.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Locale;

@Component
public class Bot extends TelegramLongPollingBot {

    private RegistrationService registrationService;

    @Value("${bot.token}")
    private String botToken;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage());
        }
    }

    private void handleMessage(Message message) {
        long chatId = message.getChatId();
        String text = message.getText();
        Locale locale = getLocale(message.getFrom().getLanguageCode());
        executeMessage(registrationService.handleMessage(chatId, text, locale));
    }

    private Locale getLocale(String languageCode) {
        return languageCode != null ? new Locale(languageCode) : new Locale("ru");
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    public void setRegistrationService(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @Override
    public String getBotUsername() {
        return "nutrition_balance_bot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
