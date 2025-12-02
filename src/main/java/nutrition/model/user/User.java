package nutrition.model.user;

public record User(long chatId, int age, int height, int weight, Sex sex, ActivityLevel activityLevel) {
    public User(long chatId) {
        this(chatId, 0, 0, 0, null, null);
    }

    public User withSex(Sex sex) {
        return new User(chatId, age, height, weight, sex, activityLevel);
    }

    public User withAge(Integer age) {
        return new User(chatId, age, height, weight, sex, activityLevel);
    }

    public User withWeight(Integer weight) {
        return new User(chatId, age, height, weight, sex, activityLevel);
    }

    public User withHeight(Integer height) {
        return new User(chatId, age, height, weight, sex, activityLevel);
    }

    public User withActivity(ActivityLevel activity) {
        return new User(chatId, age, height, weight, sex, activity);
    }
}
