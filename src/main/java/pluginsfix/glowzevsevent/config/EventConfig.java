package pluginsfix.glowzevsevent.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

public final class EventConfig {

    private final String worldName;
    private final int yOffset;
    private final int searchMax;
    private final boolean timeEnabled;
    private final List<String> timeStarts;
    private final String timeZone;
    private final String timeFormat;
    private final int minPlayers;
    private final String schematicName;
    private final int schematicBatchSize;
    private final int schematicClearDelay;
    private final int distanceMin;
    private final int distanceMax;
    private final int rgSize;
    private final String hologramProvider;
    private final String bossbarColor;
    private final String bossbarStyle;
    private final Map<Integer, StageConfig> stages;
    private final Map<String, List<String>> actions;

    private EventConfig(
            String worldName,
            int yOffset,
            int searchMax,
            boolean timeEnabled,
            List<String> timeStarts,
            String timeZone,
            String timeFormat,
            int minPlayers,
            String schematicName,
            int schematicBatchSize,
            int schematicClearDelay,
            int distanceMin,
            int distanceMax,
            int rgSize,
            String hologramProvider,
            String bossbarColor,
            String bossbarStyle,
            Map<Integer, StageConfig> stages,
            Map<String, List<String>> actions
    ) {
        this.worldName = worldName;
        this.yOffset = yOffset;
        this.searchMax = searchMax;
        this.timeEnabled = timeEnabled;
        this.timeStarts = timeStarts;
        this.timeZone = timeZone;
        this.timeFormat = timeFormat;
        this.minPlayers = minPlayers;
        this.schematicName = schematicName;
        this.schematicBatchSize = schematicBatchSize;
        this.schematicClearDelay = schematicClearDelay;
        this.distanceMin = distanceMin;
        this.distanceMax = distanceMax;
        this.rgSize = rgSize;
        this.hologramProvider = hologramProvider;
        this.bossbarColor = bossbarColor;
        this.bossbarStyle = bossbarStyle;
        this.stages = stages;
        this.actions = actions;
    }

    public static EventConfig load(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveResource("config.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection settingsSec = config.getConfigurationSection("settings");
        if (settingsSec == null) {
            settingsSec = config.createSection("settings");
        }

        String worldName = settingsSec.getString("world", "world");
        int yOffset = settingsSec.getInt("yOffset", 125);
        int searchMax = settingsSec.getInt("searchMax", 50);

        ConfigurationSection timeSec = settingsSec.getConfigurationSection("time");
        boolean timeEnabled = timeSec != null && timeSec.getBoolean("enable", true);
        List<String> timeStarts = timeSec != null ? timeSec.getStringList("start") : List.of("12:00", "00:00");
        String timeZone = timeSec != null ? timeSec.getString("zone", "Europe/Moscow") : "Europe/Moscow";
        String timeFormat = timeSec != null ? timeSec.getString("format", "HH:mm") : "HH:mm";
        int minPlayers = timeSec != null ? timeSec.getInt("minPlayers", 20) : 20;

        ConfigurationSection schemSec = settingsSec.getConfigurationSection("schematic");
        String schematicName = schemSec != null ? schemSec.getString("name", "ship.schem") : "ship.schem";
        int schematicBatchSize = schemSec != null ? schemSec.getInt("batchSize", 100) : 100;
        int schematicClearDelay = schemSec != null ? schemSec.getInt("clearDelay", 5) : 5;

        ConfigurationSection distSec = settingsSec.getConfigurationSection("distance");
        int distanceMin = distSec != null ? distSec.getInt("min", 100) : 100;
        int distanceMax = distSec != null ? distSec.getInt("max", 2100) : 2100;
        int rgSize = distSec != null ? distSec.getInt("rgSize", 30) : 30;

        String hologramProvider = settingsSec.getString("hologramProvider", "DecentHolograms");

        ConfigurationSection bossbarSec = settingsSec.getConfigurationSection("bossbar");
        String bossbarColor = bossbarSec != null ? bossbarSec.getString("color", "YELLOW") : "YELLOW";
        String bossbarStyle = bossbarSec != null ? bossbarSec.getString("style", "SOLID") : "SOLID";

        Map<Integer, StageConfig> stages = new HashMap<>();
        ConfigurationSection stagesSec = settingsSec.getConfigurationSection("stages");
        if (stagesSec != null) {
            for (String key : stagesSec.getKeys(false)) {
                try {
                    int stageNum = Integer.parseInt(key);
                    ConfigurationSection sSec = stagesSec.getConfigurationSection(key);
                    if (sSec != null) {
                        String bossbar = sSec.getString("bossbar", "");
                        int duration = sSec.getInt("duration", 300);
                        int lightningInterval = sSec.getInt("lightningInterval", 0);
                        int lightningRadius = sSec.getInt("lightningRadius", 6);
                        int skyDropInterval = sSec.getInt("skyDropInterval", 0);
                        int skyDropRadius = sSec.getInt("skyDropRadius", 5);
                        Set<String> destroyBlocks = new HashSet<>(sSec.getStringList("lightningDestroyingBlocks"));

                        Map<String, String> flags = new HashMap<>();
                        ConfigurationSection flagsSec = sSec.getConfigurationSection("flags");
                        if (flagsSec != null) {
                            for (String flagKey : flagsSec.getKeys(false)) {
                                flags.put(flagKey.toUpperCase(Locale.ROOT), flagsSec.getString(flagKey, "DENY"));
                            }
                        }

                        List<String> disguiseItems = sSec.getStringList("chestDisguiseItems");
                        long chestCooldown = sSec.getLong("chestTakeCooldown", 1100);

                        Map<Integer, ChestEntryConfig> chests = new HashMap<>();
                        ConfigurationSection chestsSec = sSec.getConfigurationSection("chests");
                        if (chestsSec != null) {
                            for (String cKey : chestsSec.getKeys(false)) {
                                try {
                                    int cNum = Integer.parseInt(cKey);
                                    ConfigurationSection cSec = chestsSec.getConfigurationSection(cKey);
                                    if (cSec != null) {
                                        int offX = cSec.getInt("offSetX", 0);
                                        int offY = cSec.getInt("offSetY", 0);
                                        int offZ = cSec.getInt("offSetZ", 0);
                                        float offYaw = (float) cSec.getDouble("offSetYaw", 0.0);
                                        String cName = cSec.getString("name", "&fСундук зевса");
                                        List<String> holo = cSec.getStringList("hologram");
                                        chests.put(cNum, new ChestEntryConfig(offX, offY, offZ, offYaw, cName, holo));
                                    }
                                } catch (NumberFormatException ignored) {}
                            }
                        }

                        stages.put(stageNum, new StageConfig(
                                stageNum, bossbar, duration, lightningInterval, lightningRadius,
                                skyDropInterval, skyDropRadius, destroyBlocks, flags, disguiseItems,
                                chestCooldown, chests
                        ));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        Map<String, List<String>> actions = new HashMap<>();
        ConfigurationSection actionsSec = config.getConfigurationSection("actions");
        if (actionsSec != null) {
            for (String key : actionsSec.getKeys(false)) {
                actions.put(key, actionsSec.getStringList(key));
            }
        }

        return new EventConfig(
                worldName, yOffset, searchMax, timeEnabled, timeStarts, timeZone, timeFormat,
                minPlayers, schematicName, schematicBatchSize, schematicClearDelay, distanceMin,
                distanceMax, rgSize, hologramProvider, bossbarColor, bossbarStyle, stages, actions
        );
    }

    public String worldName() {
        return worldName;
    }

    public int yOffset() {
        return yOffset;
    }

    public int searchMax() {
        return searchMax;
    }

    public boolean timeEnabled() {
        return timeEnabled;
    }

    public List<String> timeStarts() {
        return timeStarts;
    }

    public String timeZone() {
        return timeZone;
    }

    public String timeFormat() {
        return timeFormat;
    }

    public int minPlayers() {
        return minPlayers;
    }

    public String schematicName() {
        return schematicName;
    }

    public int schematicBatchSize() {
        return schematicBatchSize;
    }

    public int schematicClearDelay() {
        return schematicClearDelay;
    }

    public int distanceMin() {
        return distanceMin;
    }

    public int distanceMax() {
        return distanceMax;
    }

    public int rgSize() {
        return rgSize;
    }

    public String hologramProvider() {
        return hologramProvider;
    }

    public String bossbarColor() {
        return bossbarColor;
    }

    public String bossbarStyle() {
        return bossbarStyle;
    }

    public Map<Integer, StageConfig> stages() {
        return stages;
    }

    public StageConfig stage(int stageNumber) {
        return stages.get(stageNumber);
    }

    public Map<String, List<String>> actions() {
        return actions;
    }

    public List<String> actionList(String actionKey) {
        return actions.getOrDefault(actionKey, Collections.emptyList());
    }
}
