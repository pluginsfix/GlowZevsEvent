package pluginsfix.glowzevsevent.text;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class Messages {

    private final Plugin plugin;
    private final Logger logger;
    private final Map<String, Object> messagesCache = new HashMap<>();

    public Messages(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        messagesCache.clear();
        for (String key : config.getKeys(true)) {
            if (!config.isConfigurationSection(key)) {
                messagesCache.put(key, config.get(key));
            }
        }
    }

    public void reload() {
        load();
    }

    public String getRaw(String key) {
        Object val = messagesCache.get(key);
        if (val instanceof String str) {
            return str;
        }
        logger.warning("Missing message key: " + key);
        return "";
    }

    @SuppressWarnings("unchecked")
    public List<String> getRawList(String key) {
        Object val = messagesCache.get(key);
        if (val instanceof List<?> list) {
            return (List<String>) list;
        }
        return Collections.emptyList();
    }

    public String get(String key, Object... replacements) {
        String raw = getRaw(key);
        if (raw.isEmpty()) {
            return "";
        }
        String prefix = getRaw("prefix");
        String message = prefix + raw;
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String target = "{" + replacements[i] + "}";
                String replacement = String.valueOf(replacements[i + 1]);
                message = message.replace(target, replacement);
            }
        }
        return ColorFormatter.colorize(message);
    }

    public void send(CommandSender sender, String key, Object... replacements) {
        String msg = get(key, replacements);
        if (!msg.isEmpty()) {
            sender.sendMessage(msg);
        }
    }

    public void sendList(CommandSender sender, String key, Object... replacements) {
        List<String> lines = getRawList(key);
        for (String line : lines) {
            String processed = line;
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    String target = "{" + replacements[i] + "}";
                    String replacement = String.valueOf(replacements[i + 1]);
                    processed = processed.replace(target, replacement);
                }
            }
            sender.sendMessage(ColorFormatter.colorize(processed));
        }
    }
}
