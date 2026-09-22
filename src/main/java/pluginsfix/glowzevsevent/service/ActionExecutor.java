package pluginsfix.glowzevsevent.service;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import pluginsfix.glowzevsevent.hook.PlaceholderHook;
import pluginsfix.glowzevsevent.text.ColorFormatter;

import java.util.List;
import java.util.Map;

public final class ActionExecutor {

    private final PlaceholderHook placeholderHook;

    public ActionExecutor(PlaceholderHook placeholderHook) {
        this.placeholderHook = placeholderHook;
    }

    public void execute(List<String> actions, Location location, Map<String, String> placeholders) {
        if (actions == null || actions.isEmpty()) return;

        for (String rawAction : actions) {
            String action = formatPlaceholders(rawAction, placeholders);

            if (action.startsWith("[Broadcast] ")) {
                String text = action.substring(12);
                String colored = ColorFormatter.colorize(text);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(colored);
                }
            } else if (action.startsWith("[Message] ")) {
                String text = action.substring(10);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String formatted = ColorFormatter.colorize(placeholderHook.setPlaceholders(player, text));
                    player.sendMessage(formatted);
                }
            } else if (action.startsWith("[Actionbar] ")) {
                String text = action.substring(12);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String formatted = ColorFormatter.colorize(placeholderHook.setPlaceholders(player, text));
                    try {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(formatted));
                    } catch (Exception e) {
                        player.sendMessage(formatted);
                    }
                }
            } else if (action.startsWith("[Sound] ")) {
                String data = action.substring(8).trim();
                String[] parts = data.split(":");
                try {
                    Sound sound = Sound.valueOf(parts[0].toUpperCase());
                    float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                    float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), sound, volume, pitch);
                    }
                } catch (IllegalArgumentException ignored) {}
            } else if (action.startsWith("[Title] ")) {
                String data = action.substring(8).trim();
                String[] parts = data.split(";");
                String title = parts.length > 0 ? ColorFormatter.colorize(parts[0]) : "";
                String subtitle = parts.length > 1 ? ColorFormatter.colorize(parts[1]) : "";
                int inTicks = parts.length > 2 ? Integer.parseInt(parts[2]) / 50 : 10;
                int stayTicks = parts.length > 3 ? Integer.parseInt(parts[3]) / 50 : 60;
                int outTicks = parts.length > 4 ? Integer.parseInt(parts[4]) / 50 : 10;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendTitle(title, subtitle, inTicks, stayTicks, outTicks);
                }
            } else if (action.startsWith("[Effect] ")) {
                String effectName = action.substring(9).trim();
                if (location != null && location.getWorld() != null) {
                    try {
                        Effect effect = Effect.valueOf(effectName.toUpperCase());
                        location.getWorld().playEffect(location, effect, 0);
                    } catch (IllegalArgumentException ignored) {}
                }
            } else if (action.startsWith("[Particle] ")) {
                String data = action.substring(11).trim();
                String[] parts = data.split(":");
                if (location != null && location.getWorld() != null && parts.length > 0) {
                    try {
                        Particle particle = Particle.valueOf(parts[0].toUpperCase());
                        int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
                        double offX = parts.length > 2 ? Double.parseDouble(parts[2]) : 0.1;
                        double offY = parts.length > 3 ? Double.parseDouble(parts[3]) : 0.1;
                        double offZ = parts.length > 4 ? Double.parseDouble(parts[4]) : 0.1;
                        double speed = parts.length > 5 ? Double.parseDouble(parts[5]) : 0.1;
                        location.getWorld().spawnParticle(particle, location, count, offX, offY, offZ, speed);
                    } catch (IllegalArgumentException ignored) {}
                }
            } else if (action.startsWith("[Command] ")) {
                String cmd = action.substring(10).trim();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
    }

    private String formatPlaceholders(String text, Map<String, String> placeholders) {
        if (placeholders == null || text == null) return text;
        String res = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            res = res.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return res;
    }
}
