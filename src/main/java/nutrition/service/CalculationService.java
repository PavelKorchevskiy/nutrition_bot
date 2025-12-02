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

import static nutrition.model.user.ActivityLevel.*;

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
        String result;

        try {
            switch (option) {
                case WATER:
                    result = calculateWaterIntake(user, locale);
                    break;
                case CALORIES:
                    result = calculateCalories(user, locale);
                    break;
                case MACROS:
                    result = calculateMacronutrients(user, locale);
                    break;
                default:
                    result = messageService.get("error.calculation_not_implemented", locale);
            }
        } catch (Exception e) {
            result = messageService.get("error.calculation_failed", locale);
        }

        SendMessage message = new SendMessage(String.valueOf(chatId), result);
        message.setParseMode(ParseMode.MARKDOWN);

        // Добавляем клавиатуру для возврата в меню
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(messageService.get("menu.calculations", locale));
        row.add(messageService.get("start", locale));
        rows.add(row);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        return message;
    }

    private String calculateWaterIntake(User user, Locale locale) {
        // Формула: вес * 0.03 (рекомендуемое количество воды в литрах)
        double waterIntake = user.weight() * 0.03;
        String formattedWater = String.format("%.2f", waterIntake);

        return messageService.get("calculation.water.result", locale) +
                "\n\n" + messageService.get("calculation.result.recommendation", locale) +
                " *" + formattedWater + "* " + messageService.get("metric.liters", locale);
    }

    private String calculateCalories(User user, Locale locale) {
        // Базовая формула Миффлина-Сан Жеора
        double bmr = calculateBMR(user);
        double calories = bmr * getActivityMultiplier(user.activityLevel());

        String formattedCalories = String.format("%.0f", calories);

        return messageService.get("calculation.calories.result", locale) +
                "\n\n" + messageService.get("calculation.result.daily_needs", locale) +
                " *" + formattedCalories + "* " + messageService.get("metric.kcal", locale);
    }

    private String calculateMacronutrients(User user, Locale locale) {
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

        return messageService.get("calculation.macros.result", locale) +
                "\n\n" +
                "🥩 " + messageService.get("macros.protein", locale) + ": *" + String.format("%.0f", proteinGrams) + "* " + messageService.get("metric.grams", locale) + "\n" +
                "🥑 " + messageService.get("macros.fat", locale) + ": *" + String.format("%.0f", fatGrams) + "* " + messageService.get("metric.grams", locale) + "\n" +
                "🍚 " + messageService.get("macros.carbs", locale) + ": *" + String.format("%.0f", carbGrams) + "* " + messageService.get("metric.grams", locale);
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
}
