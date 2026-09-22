package pluginsfix.glowzevsevent.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import pluginsfix.glowzevsevent.text.ColorFormatter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LootConfig {

    public record LootItem(ItemStack itemStack, double chance) {}

    private final Plugin plugin;
    private final File file;
    private final List<LootItem> lightningDrops = new ArrayList<>();
    private final List<LootItem> skyDrops = new ArrayList<>();
    private final List<LootItem> chestDrops = new ArrayList<>();

    public LootConfig(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "chests.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("chests.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        lightningDrops.clear();
        skyDrops.clear();
        chestDrops.clear();

        ConfigurationSection dropsSec = config.getConfigurationSection("drops");
        if (dropsSec != null) {
            loadSection(dropsSec.getConfigurationSection("lightning"), lightningDrops);
            loadSection(dropsSec.getConfigurationSection("skydrop"), skyDrops);
            loadSection(dropsSec.getConfigurationSection("chest"), chestDrops);
        }
    }

    private void loadSection(ConfigurationSection section, List<LootItem> target) {
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                ConfigurationSection itemSec = section.getConfigurationSection(key);
                if (itemSec != null) {
                    String matName = itemSec.getString("material", "DIAMOND");
                    Material material = Material.matchMaterial(matName);
                    if (material != null) {
                        int amount = itemSec.getInt("amount", 1);
                        double chance = itemSec.getDouble("chance", 50.0);
                        String name = itemSec.getString("name");
                        List<String> lore = itemSec.getStringList("lore");

                        ItemStack item = new ItemStack(material, Math.max(1, amount));
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            ColorFormatter.applyItemMeta(meta, name, lore);
                            item.setItemMeta(meta);
                        }
                        target.add(new LootItem(item, chance));
                    }
                }
            } else if (section.isList(key) || key.matches("\\d+")) {
                Object obj = section.get(key);
                if (obj instanceof ConfigurationSection subSec) {
                    String matName = subSec.getString("material", "DIAMOND");
                    Material material = Material.matchMaterial(matName);
                    if (material != null) {
                        int amount = subSec.getInt("amount", 1);
                        double chance = subSec.getDouble("chance", 50.0);
                        ItemStack item = new ItemStack(material, Math.max(1, amount));
                        target.add(new LootItem(item, chance));
                    }
                }
            }
        }
        for (var map : section.getMapList("")) {
            parseMapItem(map, target);
        }
    }

    private void parseMapItem(java.util.Map<?, ?> map, List<LootItem> target) {
        String matName = String.valueOf(map.get("material"));
        Material material = Material.matchMaterial(matName);
        if (material != null) {
            int amount = 1;
            if (map.containsKey("amount")) {
                amount = ((Number) map.get("amount")).intValue();
            }
            double chance = 50.0;
            if (map.containsKey("chance")) {
                chance = ((Number) map.get("chance")).doubleValue();
            }
            ItemStack item = new ItemStack(material, Math.max(1, amount));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String name = map.containsKey("name") ? String.valueOf(map.get("name")) : null;
                List<String> lore = null;
                if (map.containsKey("lore") && map.get("lore") instanceof List<?> loreList) {
                    lore = loreList.stream().map(Object::toString).toList();
                }
                ColorFormatter.applyItemMeta(meta, name, lore);
                item.setItemMeta(meta);
            }
            target.add(new LootItem(item, chance));
        }
    }

    public boolean addItem(String type, ItemStack item, double chance) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = "drops." + type.toLowerCase();
        List<java.util.Map<String, Object>> list = new ArrayList<>();
        List<java.util.Map<?, ?>> existing = config.getMapList(path);
        if (existing != null) {
            for (var entry : existing) {
                java.util.Map<String, Object> copy = new java.util.HashMap<>();
                entry.forEach((k, v) -> copy.put(String.valueOf(k), v));
                list.add(copy);
            }
        }

        java.util.Map<String, Object> newItem = new java.util.HashMap<>();
        newItem.put("material", item.getType().name());
        newItem.put("amount", item.getAmount());
        newItem.put("chance", chance);
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            newItem.put("name", item.getItemMeta().getDisplayName());
        }
        list.add(newItem);
        config.set(path, list);

        try {
            config.save(file);
            load();
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save chests.yml: " + e.getMessage());
            return false;
        }
    }

    public List<LootItem> getLightningDrops() {
        return Collections.unmodifiableList(lightningDrops);
    }

    public List<LootItem> getSkyDrops() {
        return Collections.unmodifiableList(skyDrops);
    }

    public List<LootItem> getChestDrops() {
        return Collections.unmodifiableList(chestDrops);
    }
}
