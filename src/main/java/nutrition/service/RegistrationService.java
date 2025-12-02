package nutrition.service;

import nutrition.model.user.ActivityLevel;
import nutrition.model.user.RegistrationState;
import nutrition.model.user.Sex;
import nutrition.model.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class RegistrationService {

    private final MessageService messageService;
    private final UserService userService;
    private final CalculationService calculationService;

    public SendMessage handleMessage(long chatId, String text, Locale locale) {
        if ("/start".equals(text) || messageService.get("start", locale).equals(text)) {
            return handleStart(chatId, locale);
        }
        if (messageService.get("menu.calculations", locale).equals(text)) {
            return showCalculationMenu(chatId, locale);
        }
        RegistrationState userState = userService.getUserState(chatId);
        User user = userService.getOrCreateUser(chatId);
        return switch (userState) {
            case START -> handleStartMenu(chatId, text, locale);
            case ENTERING_SEX -> handleSexInput(chatId, text, user, locale);
            case ENTERING_AGE -> handleAgeInput(chatId, text, user, locale);
            case ENTERING_WEIGHT -> handleWeightInput(chatId, text, user, locale);
            case ENTERING_HEIGHT -> handleHeightInput(chatId, text, user, locale);
            case ENTERING_ACTIVITY -> handleActivityInput(chatId, text, user, locale);
            case CALCULATION_MENU -> calculationService.handleCalculationMenu(chatId, text, locale);
            default -> new SendMessage(String.valueOf(chatId), messageService.get("error.unknown_command", locale));
        };
    }

    private SendMessage handleStart(long chatId, Locale locale) {
        userService.setUserState(chatId, RegistrationState.START);
        if (!userService.exist(chatId)) {
            userService.saveUser(chatId, new User(chatId));
        }
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

    private SendMessage handleStartMenu(long chatId, String text, Locale locale) {
        if (messageService.get("menu.enter_params", locale).equals(text)) {
            return askForSex(chatId, locale);
        }
        if (messageService.get("menu.calculations", locale).equals(text)) {
            return showCalculationMenu(chatId, locale);
        }
        return new SendMessage(String.valueOf(chatId), messageService.get("error.unknown_command", locale));
    }

    private SendMessage askForSex(long chatId, Locale locale) {
        userService.setUserState(chatId, RegistrationState.ENTERING_SEX);
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
        navRow.add(messageService.get("start", locale));

        rows.add(sexRow);
        rows.add(navRow);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    private SendMessage handleSexInput(long chatId, String text, User user, Locale locale) {
        if (messageService.get("navigation.skip", locale).equals(text)) {
            userService.saveUser(chatId, user);
            return askForAge(chatId, locale);
        }
        if (messageService.get("param.sex.male", locale).equals(text)) {
            userService.saveUser(chatId, user.withSex(Sex.MALE));
        } else if (messageService.get("param.sex.female", locale).equals(text)) {
            userService.saveUser(chatId, user.withSex(Sex.FEMALE));
        } else {
            return new SendMessage(String.valueOf(chatId), messageService.get("error.invalid_sex", locale));
        }

        return askForAge(chatId, locale);
    }

    private SendMessage askForAge(long chatId, Locale locale) {
        userService.setUserState(chatId, RegistrationState.ENTERING_AGE);

        String text = messageService.get("param.age.question", locale);
        SendMessage message = new SendMessage(String.valueOf(chatId), text);

        setNavigationKeyboard(message, locale);
        return message;
    }

    private SendMessage handleAgeInput(long chatId, String text, User user, Locale locale) {
        if (messageService.get("navigation.skip", locale).equals(text)) {
            userService.saveUser(chatId, user);
            return askForWeight(chatId, locale);
        }

        if (messageService.get("navigation.back", locale).equals(text)) {
            return askForSex(chatId, locale);
        }

        try {
            int age = Integer.parseInt(text);
            if (age < 14) {
                return new SendMessage(String.valueOf(chatId), messageService.get("error.invalid_age_range.young", locale));
            }
            if (age > 100) {
                return new SendMessage(String.valueOf(chatId), messageService.get("error.invalid_age_range.old", locale));
            }

            userService.saveUser(chatId, user.withAge(age));
            return askForWeight(chatId, locale);

        } catch (NumberFormatException e) {
            return new SendMessage(String.valueOf(chatId), messageService.get("error.invalid_number", locale));
        }
    }

    private SendMessage askForWeight(long chatId, Locale locale) {
        userService.setUserState(chatId, RegistrationState.ENTERING_WEIGHT);

        String text = messageService.get("param.weight.question", locale);
        SendMessage message = new SendMessage(String.valueOf(chatId), text);

        // Создаем клавиатуру с популярными значениями веса
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        int[] weightRanges = {40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100};

        KeyboardRow currentRow = new KeyboardRow();
        for (int weight : weightRanges) {
            currentRow.add(weight + " " + messageService.get("metric.kg", locale));

            if (currentRow.size() == 4) {
                rows.add(currentRow);
                currentRow = new KeyboardRow();
            }
        }

        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        KeyboardRow navRow = new KeyboardRow();
        navRow.add(messageService.get("navigation.skip", locale));
        navRow.add(messageService.get("navigation.back", locale));
        navRow.add(messageService.get("start", locale));
        rows.add(navRow);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    private SendMessage handleWeightInput(long chatId, String text, User user, Locale locale) {
        if (messageService.get("navigation.skip", locale).equals(text)) {
            userService.saveUser(chatId, user);
            return askForHeight(chatId, locale);
        }

        if (messageService.get("navigation.back", locale).equals(text)) {
            return askForAge(chatId, locale);
        }

        try {
            String weightText = text.replaceAll("[^0-9]", "");
            int weight = Integer.parseInt(weightText);
            if (weight < 30) {
                return new SendMessage(String.valueOf(chatId), messageService.get("error.invalid_weight_range.low", locale));
            }
            if (weight > 250) {
                return new SendMessage(String.valueOf(chatId), messageService.get("error.invalid_weight_range.high", locale));
            }

            userService.saveUser(chatId, user.withWeight(weight));
            return askForHeight(chatId, locale);

        } catch (NumberFormatException e) {
            return new SendMessage(String.valueOf(chatId), messageService.get("error.invalid_number", locale));
        }
    }

    private SendMessage askForHeight(long chatId, Locale locale) {
        userService.setUserState(chatId, RegistrationState.ENTERING_HEIGHT);

        String text = messageService.get("param.height.question", locale);
        SendMessage message = new SendMessage(String.valueOf(chatId), text);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        int[] heightRanges = {140, 145, 150, 155, 160, 165, 170, 175, 180, 185, 190, 195, 200};

        KeyboardRow currentRow = new KeyboardRow();
        for (int height : heightRanges) {
            currentRow.add(height + " " + messageService.get("metric.sm", locale));

            if (currentRow.size() == 4) {
                rows.add(currentRow);
                currentRow = new KeyboardRow();
            }
        }

        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        KeyboardRow navRow = new KeyboardRow();
        navRow.add(messageService.get("navigation.skip", locale));
        navRow.add(messageService.get("navigation.back", locale));
        navRow.add(messageService.get("start", locale));
        rows.add(navRow);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    private SendMessage handleHeightInput(long chatId, String text, User user, Locale locale) {
        if (messageService.get("navigation.skip", locale).equals(text)) {
            userService.saveUser(chatId, user);
            return askForActivity(chatId, locale);
        }

        if (messageService.get("navigation.back", locale).equals(text)) {
            return askForWeight(chatId, locale);
        }

        try {
            String heightText = text.replaceAll("[^0-9]", "");
            int height = Integer.parseInt(heightText);

            if (height < 130 || height > 220) {
                return new SendMessage(String.valueOf(chatId), messageService.get("error.invalid_height_range", locale));
            }

            userService.saveUser(chatId, user.withHeight(height));
            return askForActivity(chatId, locale);

        } catch (NumberFormatException e) {
            return new SendMessage(String.valueOf(chatId), messageService.get("error.invalid_number", locale));
        }
    }

    private SendMessage askForActivity(long chatId, Locale locale) {
        userService.setUserState(chatId, RegistrationState.ENTERING_ACTIVITY);

        String text = messageService.get("param.activity.question", locale);
        SendMessage message = new SendMessage(String.valueOf(chatId), text);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

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
        navRow.add(messageService.get("start", locale));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(navRow);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    private SendMessage handleActivityInput(long chatId, String text, User user, Locale locale) {
        if (messageService.get("navigation.skip", locale).equals(text)) {
            userService.saveUser(chatId, user);
            return showCalculationMenu(chatId, locale);
        }

        if (messageService.get("navigation.back", locale).equals(text)) {
            return askForHeight(chatId, locale);
        }

        ActivityLevel activity = mapTextToActivity(text, locale);
        if (activity != null) {
            userService.saveUser(chatId, user.withActivity(activity));
            return showCalculationMenu(chatId, locale);
        } else {
            return new SendMessage(String.valueOf(chatId), messageService.get("error.invalid_activity", locale));
        }
    }

    private SendMessage showCalculationMenu(long chatId, Locale locale) {
        userService.setUserState(chatId, RegistrationState.CALCULATION_MENU);

        User user = userService.getUser(chatId);
        String summary = buildUserSummary(user, locale);
        String menuText = summary + "\n\n" + messageService.get("calculation.menu.title", locale);

        SendMessage message = new SendMessage(String.valueOf(chatId), menuText);
        message.setParseMode(ParseMode.MARKDOWN);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        // Получаем опции вычислений
        List<String> calculationOptions = calculationService.getOptions().stream()
                .map(option -> messageService.get("calculation." + option.toLowerCase(), locale))
                .toList();

        // Распределяем опции по рядам (по 2 кнопки в ряду)
        KeyboardRow currentRow = new KeyboardRow();
        for (String option : calculationOptions) {
            currentRow.add(option);

            if (currentRow.size() == 2) {
                rows.add(currentRow);
                currentRow = new KeyboardRow();
            }
        }

        // Добавляем последний неполный ряд с опциями
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        // Ряд с кнопкой редактирования параметров
        KeyboardRow editRow = new KeyboardRow();
        editRow.add(messageService.get("menu.edit_params", locale));
        rows.add(editRow);

        // Навигационный ряд
        KeyboardRow navRow = new KeyboardRow();
        navRow.add(messageService.get("start", locale));
        rows.add(navRow);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    // Вспомогательный метод остается без изменений
    private ActivityLevel mapTextToActivity(String text, Locale locale) {
        if (messageService.get("param.activity.sedentary", locale).equals(text)) return ActivityLevel.SEDENTARY;
        if (messageService.get("param.activity.light", locale).equals(text)) return ActivityLevel.LIGHT;
        if (messageService.get("param.activity.moderate", locale).equals(text)) return ActivityLevel.MODERATE;
        if (messageService.get("param.activity.active", locale).equals(text)) return ActivityLevel.ACTIVE;
        if (messageService.get("param.activity.very_active", locale).equals(text)) return ActivityLevel.VERY_ACTIVE;
        return null;
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

        if (user.weight() != 0) {
            sb.append("• ").append(messageService.get("param.weight.title", locale))
                    .append(": ").append(user.weight()).append(" ").append(messageService.get("metric.kg", locale)).append("\n");
        }

        if (user.height() != 0) {
            sb.append("• ").append(messageService.get("param.height.title", locale))
                    .append(": ").append(user.height()).append(" ").append(messageService.get("metric.sm", locale)).append("\n");
        }

        if (user.activityLevel() != null) {
            sb.append("• ").append(messageService.get("param.activity.title", locale))
                    .append(": ").append(messageService.get("param.activity." + user.activityLevel().name().toLowerCase(), locale))
                    .append("\n");
        }

        return sb.toString();
    }

    private void setNavigationKeyboard(SendMessage message, Locale locale) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow navRow = new KeyboardRow();
        navRow.add(messageService.get("navigation.skip", locale));
        navRow.add(messageService.get("navigation.back", locale));
        navRow.add(messageService.get("start", locale));

        rows.add(navRow);
        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
    }

    @Autowired
    public RegistrationService(MessageService messageService, UserService userService, CalculationService calculationService) {
        this.messageService = messageService;
        this.userService = userService;
        this.calculationService = calculationService;
    }
}
