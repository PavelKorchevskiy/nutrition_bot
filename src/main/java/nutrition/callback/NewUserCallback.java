package nutrition.callback;

@FunctionalInterface
public interface NewUserCallback {
    void onNewUser(long chatId);
}
