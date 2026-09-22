package pluginsfix.glowzevsevent.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorFormatter {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HASH_HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");

    private ColorFormatter() {}

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(buffer, "§x§" + hexCode.charAt(0) + "§" + hexCode.charAt(1)
                    + "§" + hexCode.charAt(2) + "§" + hexCode.charAt(3)
                    + "§" + hexCode.charAt(4) + "§" + hexCode.charAt(5));
        }
        matcher.appendTail(buffer);

        String result = buffer.toString();
        matcher = HASH_HEX_PATTERN.matcher(result);
        buffer = new StringBuilder();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(buffer, "§x§" + hexCode.charAt(0) + "§" + hexCode.charAt(1)
                    + "§" + hexCode.charAt(2) + "§" + hexCode.charAt(3)
                    + "§" + hexCode.charAt(4) + "§" + hexCode.charAt(5));
        }
        matcher.appendTail(buffer);

        String colored = ChatColor.translateAlternateColorCodes('&', buffer.toString());
        return colored.replace("§o", "");
    }

    public static Component toComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        String colorized = colorize(text);
        return LegacyComponentSerializer.legacySection().deserialize(colorized)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static void applyItemMeta(ItemMeta meta, String displayName, List<String> lore) {
        if (meta == null) return;

        if (displayName != null && !displayName.isEmpty()) {
            meta.setDisplayName(colorize(displayName));
        }

        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore.stream().map(ColorFormatter::colorize).toList());
        }
    }
}
