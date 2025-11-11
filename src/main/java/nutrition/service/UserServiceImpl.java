package nutrition.service;

import nutrition.model.user.RegistrationState;
import nutrition.model.user.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserServiceImpl implements UserService{

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final Map<Long, RegistrationState> userStates = new ConcurrentHashMap<>();

    @Override
    public User getUser(long chatId) {
        return users.get(chatId);
    }

    @Override
    public User getOrCreateUser(long chatId) {
        return users.getOrDefault(chatId, new User(chatId));
    }

    @Override
    public void saveUser(long chatId, User user) {
        users.put(chatId, user);
    }

    @Override
    public RegistrationState getUserState(long chatId) {
        return userStates.get(chatId);
    }

    @Override
    public void setUserState(long chatId, RegistrationState state) {
        userStates.put(chatId, state);
    }
}
