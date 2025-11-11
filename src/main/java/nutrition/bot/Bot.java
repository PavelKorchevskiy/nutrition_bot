package nutrition.bot;

import nutrition.model.message.KeyboardType;
import nutrition.model.message.SendMessageDto;
import nutrition.model.user.ActivityLevel;
import nutrition.model.user.RegistrationState;
import nutrition.model.user.Sex;
import nutrition.model.user.User;
import nutrition.service.MessageService;
import nutrition.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

        if ("/start".equals(text)) {
            executeMessage(registrationService.handleStart(chatId, locale));
            return;
        }

        switch (currentState) {
            case START -> handleStartMenu(chatId, text, locale);
            case ENTERING_SEX -> handleSexInput(chatId, text, currentUser, locale);
            case ENTERING_AGE -> handleAgeInput(chatId, text, currentUser, locale);
            case ENTERING_WEIGHT -> handleWeightInput(chatId, text, currentUser, locale);
            case ENTERING_HEIGHT -> handleHeightInput(chatId, text, currentUser, locale);
            case ENTERING_ACTIVITY -> handleActivityInput(chatId, text, currentUser, locale);
//            case CALCULATION_MENU -> handleCalculationMenu(chatId, text, locale);
        }
    }

    private void handleStart(long chatId, Locale locale) {
        userStates.put(chatId, RegistrationState.START);

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

        executeMessage(message);
    }

    private void handleStartMenu(long chatId, String text, Locale locale) {
        if (messageService.get("menu.enter_params", locale).equals(text)) {
            startParametrization(chatId, locale);
        } else if (messageService.get("menu.calculations", locale).equals(text)) {
            showCalculationMenu(chatId, locale);
        } else {
            sendMessage(chatId, messageService.get("error.unknown_command", locale));
        }
    }
    private void startParametrization(long chatId, Locale locale) {
        userStates.put(chatId, RegistrationState.ENTERING_SEX);
        askForSex(chatId, locale);
    }

    private void askForSex(long chatId, Locale locale) {
        String text = messageService.get("param.sex.question", locale);
        SendMessage message = new SendMessage(String.valueOf(chatId), text);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow sexRow = new KeyboardRow();
        sexRow.add(messageService.get("param.sex.male", locale));
        sexRow.add(messageService.get("param.sex.female", locale));

        KeyboardRow navRow = new KeyboardRow();
        navRow.add(messageService.get("navigation.skip", locale));
        navRow.add("/start");

        rows.add(sexRow);
        rows.add(navRow);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        executeMessage(message);
    }

    private void handleSexInput(long chatId, String text, User user, Locale locale) {
        if (messageService.get("navigation.skip", locale).equals(text)) {
            users.put(chatId, user); // Сохраняем без пола
            askForAge(chatId, locale);
            return;
        }

        if (messageService.get("param.sex.male", locale).equals(text)) {
            users.put(chatId, user.withSex(Sex.MALE));
        } else if (messageService.get("param.sex.female", locale).equals(text)) {
            users.put(chatId, user.withSex(Sex.FEMALE));
        } else {
            sendMessage(chatId, messageService.get("error.invalid_sex", locale));
            return;
        }

        askForAge(chatId, locale);
    }

    private void askForAge(long chatId, Locale locale) {
        userStates.put(chatId, RegistrationState.ENTERING_AGE);

        String text = messageService.get("param.age.question", locale);
        SendMessage message = new SendMessage(String.valueOf(chatId), text);

        setNavigationKeyboard(message, locale);
        executeMessage(message);
    }

    private void handleAgeInput(long chatId, String text, User user, Locale locale) {
        if (messageService.get("navigation.skip", locale).equals(text)) {
            users.put(chatId, user);
            askForWeight(chatId, locale);
            return;
        }

        if (messageService.get("navigation.back", locale).equals(text)) {
            askForSex(chatId, locale);
            return;
        }

        try {
            int age = Integer.parseInt(text);
            if (age < 14) {
                sendMessage(chatId, messageService.get("error.invalid_age_range.young", locale));
                return;
            }
            if (age > 100) {
                sendMessage(chatId, messageService.get("error.invalid_age_range.old", locale));
                return;
            }

            users.put(chatId, user.withAge(age));
            askForWeight(chatId, locale);

        } catch (NumberFormatException e) {
            sendMessage(chatId, messageService.get("error.invalid_number", locale));
        }
    }

    // Аналогично для веса и роста...
    private void handleWeightInput(long chatId, String text, User user, Locale locale) {
        if (messageService.get("navigation.skip", locale).equals(text)) {
            users.put(chatId, user);
            askForHeight(chatId, locale);
            return;
        }

        if (messageService.get("navigation.back", locale).equals(text)) {
            askForAge(chatId, locale);
            return;
        }

        try {
            int weight = Integer.parseInt(text);
            if (weight < 30) {
                sendMessage(chatId, messageService.get("error.invalid_weight_range.low", locale));
                return;
            }
            if (weight > 250) {
                sendMessage(chatId, messageService.get("error.invalid_weight_range.high", locale));
                return;
            }

            users.put(chatId, user.withWeight(weight));
            askForHeight(chatId, locale);

        } catch (NumberFormatException e) {
            sendMessage(chatId, messageService.get("error.invalid_number", locale));
        }
    }

    private void askForActivity(long chatId, Locale locale) {
        userStates.put(chatId, RegistrationState.ENTERING_ACTIVITY);

        String text = messageService.get("param.activity.question", locale);
        SendMessage message = new SendMessage(String.valueOf(chatId), text);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        // Уровни активности
        KeyboardRow row1 = new KeyboardRow();
        row1.add(messageService.get("param.activity.sedentary", locale));
        row1.add(messageService.get("param.activity.light", locale));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(messageService.get("param.activity.moderate", locale));
        row2.add(messageService.get("param.activity.active", locale));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(messageService.get("param.activity.very_active", locale));

        KeyboardRow navRow = new KeyboardRow();
        navRow.add(messageService.get("navigation.skip", locale));
        navRow.add(messageService.get("navigation.back", locale));
        navRow.add("/start");

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(navRow);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        executeMessage(message);
    }

    private void handleActivityInput(long chatId, String text, User user, Locale locale) {
        if (messageService.get("navigation.skip", locale).equals(text)) {
            users.put(chatId, user);
            showCalculationMenu(chatId, locale);
            return;
        }

        if (messageService.get("navigation.back", locale).equals(text)) {
            askForHeight(chatId, locale);
            return;
        }

        // Маппинг текста на enum
        ActivityLevel activity = mapTextToActivity(text, locale);
        if (activity != null) {
            users.put(chatId, user.withActivity(activity));
            showCalculationMenu(chatId, locale);
        } else {
            sendMessage(chatId, messageService.get("error.invalid_activity", locale));
        }
    }

    private ActivityLevel mapTextToActivity(String text, Locale locale) {
        if (messageService.get("param.activity.sedentary", locale).equals(text)) return ActivityLevel.SEDENTARY;
        if (messageService.get("param.activity.light", locale).equals(text)) return ActivityLevel.LIGHT;
        if (messageService.get("param.activity.moderate", locale).equals(text)) return ActivityLevel.MODERATE;
        if (messageService.get("param.activity.active", locale).equals(text)) return ActivityLevel.ACTIVE;
        if (messageService.get("param.activity.very_active", locale).equals(text)) return ActivityLevel.VERY_ACTIVE;
        return null;
    }

    private void handleHeightInput(long chatId, String text, User user, Locale locale) {
        if (messageService.get("navigation.skip", locale).equals(text)) {
            users.put(chatId, user);
            askForActivity(chatId, locale);
            return;
        }

        if (messageService.get("navigation.back", locale).equals(text)) {
            askForWeight(chatId, locale);
            return;
        }

        try {
            // Извлекаем число из текста (убираем " см")
            String heightText = text.replaceAll("[^0-9]", "");
            int height = Integer.parseInt(heightText);

            // Валидация диапазона роста
            if (height < 130 || height > 220) {
                sendMessage(chatId, messageService.get("error.invalid_height_range", locale));
                return;
            }

            users.put(chatId, user.withHeight(height));
            askForActivity(chatId, locale);

        } catch (NumberFormatException e) {
            sendMessage(chatId, messageService.get("error.invalid_number", locale));
        }
    }

    private void askForHeight(long chatId, Locale locale) {
        userStates.put(chatId, RegistrationState.ENTERING_HEIGHT);

        String text = messageService.get("param.height.question", locale);
        SendMessage message = new SendMessage(String.valueOf(chatId), text);

        // Создаем клавиатуру с популярными значениями роста
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        int[] heightRanges = {140, 145, 150, 155, 160, 165, 170, 175, 180, 185, 190, 195, 200};

        // Разбиваем на ряды по 3-4 кнопки
        KeyboardRow currentRow = new KeyboardRow();
        for (int weight : heightRanges) {
            currentRow.add(weight + " " + messageService.get("metric.sm", locale));

            if (currentRow.size() == 4) {
                rows.add(currentRow);
                currentRow = new KeyboardRow();
            }
        }

        // Добавляем последний неполный ряд
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        // Навигационная строка
        KeyboardRow navRow = new KeyboardRow();
        navRow.add(messageService.get("navigation.skip", locale));
        navRow.add(messageService.get("navigation.back", locale));
        navRow.add("/start");
        rows.add(navRow);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        executeMessage(message);
    }

    private void askForWeight(long chatId, Locale locale) {
        userStates.put(chatId, RegistrationState.ENTERING_WEIGHT);

        String text = messageService.get("param.weight.question", locale);
        SendMessage message = new SendMessage(String.valueOf(chatId), text);

        // Создаем клавиатуру с популярными значениями веса
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        // Весовые категории с шагом 5 кг
        int[] weightRanges = {40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100};

        // Разбиваем на ряды по 3-4 кнопки
        KeyboardRow currentRow = new KeyboardRow();
        for (int weight : weightRanges) {
            currentRow.add(weight + " " + messageService.get("metric.kg", locale));

            if (currentRow.size() == 4) {
                rows.add(currentRow);
                currentRow = new KeyboardRow();
            }
        }

        // Добавляем последний неполный ряд
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        // Навигационная строка
        KeyboardRow navRow = new KeyboardRow();
        navRow.add(messageService.get("navigation.skip", locale));
        navRow.add(messageService.get("navigation.back", locale));
        navRow.add("/start");

        rows.add(navRow);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        executeMessage(message);
    }

    private void showCalculationMenu(long chatId, Locale locale) {
        userStates.put(chatId, RegistrationState.CALCULATION_MENU);

        User user = users.get(chatId);
        String summary = buildUserSummary(user, locale);
        String menuText = summary + "\n\n" + messageService.get("calculation.menu.title", locale);

        SendMessage message = new SendMessage(String.valueOf(chatId), menuText);
        message.setParseMode(ParseMode.MARKDOWN);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(messageService.get("calculation.water", locale));
        row1.add(messageService.get("calculation.calories", locale));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(messageService.get("calculation.macros", locale));
        row2.add(messageService.get("menu.edit_params", locale));

        KeyboardRow row3 = new KeyboardRow();
        row3.add("/start");

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        executeMessage(message);
    }

    private String buildUserSummary(User user, Locale locale) {
        StringBuilder sb = new StringBuilder();
        sb.append(messageService.get("summary.title", locale)).append("\n");

        if (user.sex() != null) {
            sb.append("• ").append(messageService.get("param.sex.title", locale))
                    .append(": ").append(messageService.get(
                            user.sex() == Sex.MALE ? "param.sex.male" : "param.sex.female", locale))
                    .append("\n");
        }

        if (user.age() != 0) {
            sb.append("• ").append(messageService.get("param.age.title", locale))
                    .append(": ").append(user.age()).append("\n");
        }

        // Аналогично для остальных полей...

        return sb.toString();
    }

    private Locale getLocale(String languageCode) {
        return languageCode != null ? new Locale(languageCode) : new Locale("ru");
    }

    private void setNavigationKeyboard(SendMessage message, Locale locale) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow navRow = new KeyboardRow();
        navRow.add(messageService.get("navigation.skip", locale));
        navRow.add(messageService.get("navigation.back", locale));
        navRow.add("/start");

        rows.add(navRow);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        executeMessage(message);
    }

    private Locale getUserLocale(Update update) {
        if (update.hasMessage() && update.getMessage().getFrom() != null) {
            String langCode = update.getMessage().getFrom().getLanguageCode();
            if (langCode != null && !langCode.isEmpty()) {
                return new Locale(langCode);
            }
        }
        return Locale.getDefault();
    }

    private void sendMessage(SendMessageDto sendMessageDto) {
        sendMessage(sendMessageDto, Locale.getDefault());
    }

    private void sendMessage(SendMessageDto sendMessageDto, Locale locale) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(sendMessageDto.chatId()));
        message.setText(messageService.get(sendMessageDto.message(), locale));
        message.setReplyMarkup(createKeyboard(sendMessageDto.keyboardType()));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private ReplyKeyboardMarkup createKeyboard(KeyboardType type) {
        return switch (type) {
            case START -> {
                yield createStartKeyboard();
            }
            case OPTIONS -> {
                yield createOptionsKeyboard();
            }
            case BACK -> {
                yield createBackKeyboard();
            }
            default -> {
                yield null;
            }
        };
    }

    private ReplyKeyboardMarkup createStartKeyboard() {
        return null;
    }

    private ReplyKeyboardMarkup createOptionsKeyboard() {
        return null;
    }


    private ReplyKeyboardMarkup createBackKeyboard() {
        return null;
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
