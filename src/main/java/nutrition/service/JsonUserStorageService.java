package nutrition.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nutrition.model.user.RegistrationState;
import nutrition.model.user.User;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class JsonUserStorageService implements UserService{
    private static final String DATA_FILE = "bot-users.json";
    private final Path dataFilePath;
    private final ObjectMapper objectMapper;

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final Map<Long, RegistrationState> userStates = new ConcurrentHashMap<>();

    public JsonUserStorageService() {
        this.dataFilePath = Paths.get(DATA_FILE);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private synchronized void loadData() {
        try {
            if (Files.exists(dataFilePath)) {
                String json = Files.readString(dataFilePath);
                StorageData storageData = objectMapper.readValue(json, StorageData.class);

                users.clear();
                if (storageData.getUsers() != null) {
                    users.putAll(storageData.getUsers());
                }

                userStates.clear();
                if (storageData.getUserStates() != null) {
                    storageData.getUserStates().forEach((key, value) ->
                            userStates.put(Long.parseLong(key), RegistrationState.valueOf(value)));
                }

                log.info("Loaded {} users and {} states from {}",
                        users.size(), userStates.size(), DATA_FILE);
            } else {
                log.info("Data file {} not found, starting with empty storage", DATA_FILE);
            }
        } catch (Exception e) {
            log.error("Failed to load data from {}", DATA_FILE, e);
        }
    }

    private synchronized void saveData() {
        try {
            StorageData storageData = new StorageData();
            storageData.setUsers(new HashMap<>(users));

            Map<String, String> states = new HashMap<>();
            userStates.forEach((key, value) -> states.put(key.toString(), value.name()));
            storageData.setUserStates(states);

            String json = objectMapper.writeValueAsString(storageData);
            Files.writeString(dataFilePath, json, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            log.info("Saved {} users and {} states to {}",
                    users.size(), userStates.size(), DATA_FILE);
        } catch (Exception e) {
            log.error("Failed to save data to {}", DATA_FILE, e);
        }
    }

    @PostConstruct
    public void init() {
        loadData();
        startAutoSave();
    }

    private void startAutoSave() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                saveData();
            } catch (Exception e) {
                log.error("Auto-save failed", e);
            }
        }, 1, 1, TimeUnit.DAYS);

        // Останавливаем при завершении
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            saveData();
        }));
    }

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

    @Override
    public boolean exist(long chatId) {
        return users.get(chatId) != null;
    }
}

// Класс для хранения данных
@Data
class StorageData {
    private Map<Long, User> users = new HashMap<>();
    private Map<String, String> userStates = new HashMap<>();
}
