package nutrition.service;

import nutrition.model.user.RegistrationState;
import nutrition.model.user.User;

public interface UserService {

    User getUser(long chatId);
    User getOrCreateUser(long chatId);
    void saveUser(long chatId, User user);
    RegistrationState getUserState(long chatId);
    void setUserState(long chatId, RegistrationState state);
}
