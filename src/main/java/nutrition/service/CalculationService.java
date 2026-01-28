package nutrition.service;

import nutrition.model.CalculationOption;
import nutrition.model.user.ActivityLevel;
import nutrition.model.user.Sex;
import nutrition.model.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class CalculationService {

    private final MessageService messageService;
    private final UserService userService;

    @Autowired
    public CalculationService(MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    public List<String> getOptions() {
        return Arrays.stream(CalculationOption.class.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    public SendMessage handleCalculationMenu(long chatId, String text, Locale locale) {
        User user = userService.getUser(chatId);

        if (user == null) {
            return new SendMessage(String.valueOf(chatId), messageService.get("error.user_not_found", locale));
        }

        // Определяем выбранную опцию расчета
        CalculationOption selectedOption = findCalculationOptionByText(text, locale);

        if (selectedOption != null) {
            return performCalculation(chatId, user, selectedOption, locale);
        }

        String infoMessage = findInfoMessage(text, locale);
        if (infoMessage != null) {
            return SendMessage.builder()
                    .chatId(user.chatId())
                    .parseMode(ParseMode.MARKDOWN)
                    .text(infoMessage)
                    .replyMarkup(createBackKeyboardWithOptions(locale))
                    .build();
        }

        // Если опция не найдена
        return new SendMessage(String.valueOf(chatId), messageService.get("error.invalid_option", locale));
    }

    private CalculationOption findCalculationOptionByText(String text, Locale locale) {
        for (CalculationOption option : CalculationOption.values()) {
            String optionText = messageService.get("calculation." + option.name().toLowerCase(), locale);
            if (optionText.equals(text)) {
                return option;
            }
        }
        return null;
    }

    private SendMessage performCalculation(long chatId, User user, CalculationOption option, Locale locale) {
        SendMessage message;

        try {
            switch (option) {
                case WATER:
                    message = calculateWaterIntake(user, locale);
                    break;
                case CALORIES:
                    message = calculateCalories(user, locale);
                    break;
                case MACROS:
                    message = calculateMacronutrients(user, locale);
                    break;
                case SODIUM:
                    message = getSodiumMessage(user, locale);
                    break;
                default:
                    message = new SendMessage(String.valueOf(chatId), messageService.get("error.calculation_not_implemented", locale));
            }
        } catch (Exception e) {
            message = new SendMessage(String.valueOf(chatId), messageService.get("error.calculation_failed", locale));
        }

        return message;
    }

    private SendMessage calculateWaterIntake(User user, Locale locale) {
        // Формула: вес * 0.03 (рекомендуемое количество воды в литрах)
        double waterIntake = user.weight() * 0.03;
        String formattedWater = String.format("%.2f", waterIntake);
        String message = messageService.get("calculation.water.result", locale) +
                "\n\n" + messageService.get("calculation.result.recommendation", locale) +
                " *" + formattedWater + "* " + messageService.get("metric.liters", locale);
        return SendMessage.builder()
                .chatId(user.chatId())
                .parseMode(ParseMode.MARKDOWN)
                .text(message)
                .replyMarkup(createBackKeyboardWithOptions(locale, "info.button.water"))
                .build();
    }

    private SendMessage calculateCalories(User user, Locale locale) {
        // Базовая формула Миффлина-Сан Жеора
        double bmr = calculateBMR(user);
        double calories = bmr * getActivityMultiplier(user.activityLevel());

        String formattedCalories = String.format("%.0f", calories);

        String message = messageService.get("calculation.calories.result", locale) +
                "\n\n" + messageService.get("calculation.result.daily_needs", locale) +
                " *" + formattedCalories + "* " + messageService.get("metric.kcal", locale);
        return SendMessage.builder()
                .chatId(user.chatId())
                .parseMode(ParseMode.MARKDOWN)
                .text(message)
                .replyMarkup(createBackKeyboardWithOptions(locale, "info.button.calories"))
                .build();
    }

    private SendMessage calculateMacronutrients(User user, Locale locale) {
        double bmr = calculateBMR(user);
        double calories = bmr * getActivityMultiplier(user.activityLevel());

        // Стандартное распределение БЖУ: 30% белки, 30% жиры, 40% углеводы
        double proteinCalories = calories * 0.3;
        double fatCalories = calories * 0.3;
        double carbCalories = calories * 0.4;

        // Конвертация в граммы (1г белка/углеводов = 4 ккал, 1г жиров = 9 ккал)
        double proteinGrams = proteinCalories / 4;
        double fatGrams = fatCalories / 9;
        double carbGrams = carbCalories / 4;

        String message = messageService.get("calculation.macros.result", locale) +
                "\n\n" +
                "🥩 " + messageService.get("macros.protein", locale) + ": *" + String.format("%.0f", proteinGrams) + "* " + messageService.get("metric.grams", locale) + "\n" +
                "🥑 " + messageService.get("macros.fat", locale) + ": *" + String.format("%.0f", fatGrams) + "* " + messageService.get("metric.grams", locale) + "\n" +
                "🍚 " + messageService.get("macros.carbs", locale) + ": *" + String.format("%.0f", carbGrams) + "* " + messageService.get("metric.grams", locale);
        return SendMessage.builder()
                .chatId(user.chatId())
                .parseMode(ParseMode.MARKDOWN)
                .text(message)
                .replyMarkup(createBackKeyboardWithOptions(locale, "info.button.macros"))
                .build();
    }

    private double calculateBMR(User user) {
        // Формула Миффлина-Сан Жеора
        if (user.sex() == Sex.MALE) {
            return 10 * user.weight() + 6.25 * user.height() - 5 * user.age() + 5;
        } else {
            return 10 * user.weight() + 6.25 * user.height() - 5 * user.age() - 161;
        }
    }

    private double getActivityMultiplier(ActivityLevel activity) {
        switch (activity) {
            case SEDENTARY: return 1.2;
            case LIGHT: return 1.375;
            case MODERATE: return 1.55;
            case ACTIVE: return 1.725;
            case VERY_ACTIVE: return 1.9;
            default: return 1.2;
        }
    }

    private SendMessage getSodiumMessage(User user, Locale locale) {
        String message = messageService.get("info.sodium", locale);
        return SendMessage.builder()
                .chatId(user.chatId())
                .parseMode(ParseMode.MARKDOWN)
                .text(message)
                .replyMarkup(createBackKeyboard(locale))
                .build();
    }

    private String findInfoMessage(String text, Locale locale) {
        for (CalculationOption option : CalculationOption.values()) {
            String optionText = messageService.get("info.button." + option.name().toLowerCase(), locale);
            if (optionText.equals(text)) {
                return messageService.get("info." + option.name().toLowerCase(), locale);
            }
        }
        return null;
    }

    private SendMessage showEditParamsMenu(long chatId, Locale locale) {
        // Метод для показа меню редактирования параметров
        // (реализация зависит от вашей структуры)
        String text = messageService.get("menu.edit_params.title", locale);
        SendMessage message = new SendMessage(String.valueOf(chatId), text);

        // Добавьте клавиатуру для редактирования параметров
        ReplyKeyboardMarkup keyboard = createEditParamsKeyboard(locale);
        message.setReplyMarkup(keyboard);

        return message;
    }

    private ReplyKeyboardMarkup createEditParamsKeyboard(Locale locale) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(messageService.get("param.sex.title", locale));
        row1.add(messageService.get("param.age.title", locale));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(messageService.get("param.weight.title", locale));
        row2.add(messageService.get("param.height.title", locale));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(messageService.get("param.activity.title", locale));

        KeyboardRow row4 = new KeyboardRow();
        row4.add(messageService.get("start", locale));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);
        rows.add(row4);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private ReplyKeyboardMarkup createBackKeyboardWithOptions(Locale locale, String... options) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(messageService.get("menu.calculations", locale));
        row.add(messageService.get("start", locale));
        if (options != null) {
            for (String option : options) {
                row.add(messageService.get(option, locale));
            }
        }
        rows.add(row);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private ReplyKeyboardMarkup createBackKeyboard(Locale locale) {
        return createBackKeyboardWithOptions(locale, (String[]) null);
    }
}
