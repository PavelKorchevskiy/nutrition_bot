package nutrition.service;

import nutrition.model.user.RegistrationState;
import nutrition.model.user.User;

import java.util.List;
import java.util.Map;

public interface UserService {

    User getUser(long chatId);
    User getOrCreateUser(long chatId);
    void saveUser(User user);
    RegistrationState getUserState(long chatId);
    void setUserState(long chatId, RegistrationState state);
    boolean exist(long chatId);

    List<User> getAllUsers();

    User delete(Long chatId);
}
