package nutrition.controller;

import nutrition.model.user.User;
import nutrition.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class UserController {

    private final String ADMIN_USERNAME;
    private final String ADMIN_PASSWORD;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    
    @GetMapping("/users")
    public List<User> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        checkAuth(authHeader);
        
        return userService.getAllUsers();
    }

    @PostMapping("/users")
    public String addUsers(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody List<User> newUsers) {

        checkAuth(authHeader);

        if (newUsers == null || newUsers.isEmpty()) {
            return "No users to add";
        }

        int added = 0;
        for (User user : newUsers) {
            if (user != null) {
                userService.saveUser(user);
                added++;
            }
        }
        return String.format("Added %d users. Total: %d",
                added, userService.getAllUsers().size());
    }

    // 3. Удалить пользователей
    @DeleteMapping("/users")
    public String deleteUsers(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) List<Long> chatIds) {

        checkAuth(authHeader);

        if (chatIds == null || chatIds.isEmpty()) {
            // Удалить всех
            int count = userService.getAllUsers().size();
            userService.getAllUsers().clear();
            return String.format("Deleted all %d users", count);
        } else {
            // Удалить конкретных
            int deleted = 0;
            for (Long chatId : chatIds) {
                if (userService.delete(chatId) != null) {
                    deleted++;
                }
            }
            return String.format("Deleted %d users", deleted);
        }
    }

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
        log.info("Controller created");
        this.ADMIN_USERNAME = System.getenv()
                .getOrDefault("ADMIN_USERNAME", "admin");
        this.ADMIN_PASSWORD = System.getenv()
                .getOrDefault("ADMIN_PASSWORD", "admin123");
    }

    private void checkAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid auth header");
        }

        String credentials = new String(Base64.getDecoder().decode(authHeader.substring(6)));
        String[] parts = credentials.split(":", 2);

        if (parts.length != 2 ||
                !ADMIN_USERNAME.equals(parts[0]) ||
                !ADMIN_PASSWORD.equals(parts[1])) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }
}