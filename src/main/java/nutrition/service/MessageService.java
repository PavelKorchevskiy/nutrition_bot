package nutrition.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@Service
public class MessageService {

    private final Map<Locale, Properties> messages = new HashMap<>();

    @PostConstruct
    public void loadMessages() {
        try {
            URL resourceUrl = getClass().getClassLoader().getResource("locales");
            if (resourceUrl == null) {
                throw new RuntimeException("Папка locales не найдена в classpath");
            }

            Path dir = Paths.get(resourceUrl.toURI());

            Files.list(dir).forEach(path -> {
                String filename = path.getFileName().toString();
                if (!filename.endsWith(".properties")) return;

                String langCode = filename.replace("messages_", "").replace(".properties", "");

                Properties props = new Properties();
                try (InputStream is = Files.newInputStream(path);
                     Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    props.load(reader);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка чтения файла: " + filename, e);
                }

                Locale locale = new Locale(langCode);
                messages.put(locale, props);
            });
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки локализационных файлов", e);
        }
    }

    public String get(String key, Locale locale) {
        Properties props = messages.getOrDefault(locale, messages.get(Locale.getDefault()));
        return props.getProperty(key, "???" + key + "???");
    }
}
