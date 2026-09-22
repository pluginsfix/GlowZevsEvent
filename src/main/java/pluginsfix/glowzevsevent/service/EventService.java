package pluginsfix.glowzevsevent.service;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pluginsfix.glowzevsevent.config.ChestEntryConfig;
import pluginsfix.glowzevsevent.config.EventConfig;
import pluginsfix.glowzevsevent.config.StageConfig;
import pluginsfix.glowzevsevent.domain.DisguisedChest;
import pluginsfix.glowzevsevent.domain.EventState;
import pluginsfix.glowzevsevent.hook.HologramProvider;
import pluginsfix.glowzevsevent.hook.WorldGuardHook;
import pluginsfix.glowzevsevent.text.ColorFormatter;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class EventService {

    private final Plugin plugin;
    private final EventConfig config;
    private final ActionExecutor actionExecutor;
    private final SchematicService schematicService;
    private final LootService lootService;
    private final WorldGuardHook worldGuardHook;
    private final HologramProvider hologramProvider;

    private EventState state = EventState.IDLE;
    private Location eventCenter;
    private int currentStageNumber = 0;
    private int stageSecondsRemaining = 0;
    private int totalStageDuration = 0;

    private BossBar activeBossBar;
    private BukkitTask mainTask;
    private BukkitTask lightningTask;
    private BukkitTask skyDropTask;

    private final Map<Location, DisguisedChest> activeChests = new HashMap<>();
    private final Map<UUID, Long> chestTakeCooldowns = new HashMap<>();

    public EventService(
            Plugin plugin,
            EventConfig config,
            ActionExecutor actionExecutor,
            SchematicService schematicService,
            LootService lootService,
            WorldGuardHook worldGuardHook,
            HologramProvider hologramProvider
    ) {
        this.plugin = plugin;
        this.config = config;
        this.actionExecutor = actionExecutor;
        this.schematicService = schematicService;
        this.lootService = lootService;
        this.worldGuardHook = worldGuardHook;
        this.hologramProvider = hologramProvider;
    }

    public boolean isRunning() {
        return state != EventState.IDLE && state != EventState.STOPPED;
    }

    public EventState getState() {
        return state;
    }

    public Location getEventCenter() {
        return eventCenter;
    }

    public boolean startEvent() {
        if (isRunning()) return false;

        World world = Bukkit.getWorld(config.worldName());
        if (world == null) {
            plugin.getLogger().warning("World not found: " + config.worldName());
            return false;
        }

        Location loc = findRandomLocation(world);
        if (loc == null) {
            plugin.getLogger().warning("Could not find safe location for event");
            return false;
        }

        this.eventCenter = loc;
        this.state = EventState.STAGE_1;
        this.currentStageNumber = 1;

        worldGuardHook.createRegion(eventCenter, config.rgSize());

        schematicService.pasteSchematic(config.schematicName(), eventCenter);

        initBossBar();

        Map<String, String> placeholders = buildPlaceholders();
        actionExecutor.execute(config.actionList("start"), eventCenter, placeholders);

        enterStage(1);
        startMainLoop();

        return true;
    }

    public void stopEvent() {
        if (!isRunning()) return;

        this.state = EventState.STOPPED;
        cancelTasks();

        Map<String, String> placeholders = buildPlaceholders();
        actionExecutor.execute(config.actionList("stop"), eventCenter, placeholders);

        removeBossBar();
        removeChests();
        hologramProvider.removeAll();

        if (eventCenter != null && eventCenter.getWorld() != null) {
            worldGuardHook.removeRegion(eventCenter.getWorld());
            schematicService.clearSchematic(config.schematicBatchSize(), config.schematicClearDelay(), () -> {
                this.state = EventState.IDLE;
                this.eventCenter = null;
            });
        } else {
            this.state = EventState.IDLE;
            this.eventCenter = null;
        }
    }

    private void enterStage(int stageNum) {
        this.currentStageNumber = stageNum;
        StageConfig stage = config.stage(stageNum);
        if (stage == null) {
            stopEvent();
            return;
        }

        this.state = switch (stageNum) {
            case 1 -> EventState.STAGE_1;
            case 2 -> EventState.STAGE_2;
            case 3 -> EventState.STAGE_3;
            default -> EventState.STOPPED;
        };

        this.totalStageDuration = stage.durationSeconds();
        this.stageSecondsRemaining = stage.durationSeconds();

        if (eventCenter != null && eventCenter.getWorld() != null) {
            worldGuardHook.applyFlags(eventCenter.getWorld(), stage.flags());
        }

        if (stageNum == 2) {
            actionExecutor.execute(config.actionList("stage2"), eventCenter, buildPlaceholders());
        } else if (stageNum == 3) {
            actionExecutor.execute(config.actionList("stage3"), eventCenter, buildPlaceholders());
            spawnZeusChests(stage);
        }

        restartStageTasks(stage);
    }

    private void restartStageTasks(StageConfig stage) {
        if (lightningTask != null) {
            lightningTask.cancel();
            lightningTask = null;
        }
        if (skyDropTask != null) {
            skyDropTask.cancel();
            skyDropTask = null;
        }

        if (stage.lightningInterval() > 0) {
            long ticks = stage.lightningInterval() * 20L;
            lightningTask = new BukkitRunnable() {
                @Override
                public void run() {
                    strikeStageLightning(stage);
                }
            }.runTaskTimer(plugin, ticks, ticks);
        }

        if (stage.skyDropInterval() > 0) {
            long ticks = stage.skyDropInterval() * 20L;
            skyDropTask = new BukkitRunnable() {
                @Override
                public void run() {
                    spawnSkyDrop(stage);
                }
            }.runTaskTimer(plugin, ticks, ticks);
        }
    }

    private void startMainLoop() {
        if (mainTask != null) mainTask.cancel();

        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning()) {
                    cancel();
                    return;
                }

                updateBossBar();

                if (stageSecondsRemaining <= 0) {
                    if (currentStageNumber < 3) {
                        enterStage(currentStageNumber + 1);
                    } else {
                        stopEvent();
                        cancel();
                    }
                    return;
                }

                stageSecondsRemaining--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void strikeStageLightning(StageConfig stage) {
        if (eventCenter == null || eventCenter.getWorld() == null) return;

        World world = eventCenter.getWorld();
        int radius = stage.lightningRadius();
        int rx = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
        int rz = ThreadLocalRandom.current().nextInt(-radius, radius + 1);

        Location strikeLoc = eventCenter.clone().add(rx, 0, rz);
        strikeLoc.setY(world.getHighestBlockYAt(strikeLoc.getBlockX(), strikeLoc.getBlockZ()) + 1);

        world.strikeLightning(strikeLoc);

        if (stage.lightningDestroyingBlocks() != null && !stage.lightningDestroyingBlocks().isEmpty()) {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block block = strikeLoc.clone().add(x, y, z).getBlock();
                        if (stage.lightningDestroyingBlocks().contains(block.getType().name())) {
                            block.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }

        ItemStack drop = lootService.rollLightningDrop();
        if (drop != null) {
            world.dropItemNaturally(strikeLoc, drop);
        }
    }

    private void spawnSkyDrop(StageConfig stage) {
        if (eventCenter == null || eventCenter.getWorld() == null) return;

        World world = eventCenter.getWorld();
        int radius = stage.skyDropRadius();
        int rx = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
        int rz = ThreadLocalRandom.current().nextInt(-radius, radius + 1);

        Location dropLoc = eventCenter.clone().add(rx, 15, rz);
        ItemStack item = lootService.rollSkyDrop();
        if (item != null) {
            Item spawned = world.dropItem(dropLoc, item);
            try {
                spawned.setGlowing(true);
            } catch (Exception ignored) {}
            try {
                Particle particle = Particle.valueOf("FIREWORKS_SPARK");
                world.spawnParticle(particle, dropLoc, 20, 0.5, 0.5, 0.5, 0.05);
            } catch (Exception e) {
                try {
                    Particle particle = Particle.valueOf("FIREWORK");
                    world.spawnParticle(particle, dropLoc, 20, 0.5, 0.5, 0.5, 0.05);
                } catch (Exception ignored) {}
            }
        }
    }

    private void spawnZeusChests(StageConfig stage) {
        removeChests();
        if (eventCenter == null || eventCenter.getWorld() == null) return;

        actionExecutor.execute(config.actionList("chestSpawn"), eventCenter, buildPlaceholders());

        World world = eventCenter.getWorld();
        for (Map.Entry<Integer, ChestEntryConfig> entry : stage.chests().entrySet()) {
            int chestId = entry.getKey();
            ChestEntryConfig chestConfig = entry.getValue();

            Location chestLoc = eventCenter.clone().add(
                    chestConfig.offsetX(),
                    chestConfig.offsetY(),
                    chestConfig.offsetZ()
            );
            chestLoc.setYaw(chestConfig.offsetYaw());

            Block block = chestLoc.getBlock();
            block.setType(Material.CHEST, false);

            if (block.getState() instanceof Chest chestState) {
                Inventory inv = chestState.getInventory();
                DisguisedChest disguisedChest = new DisguisedChest(chestLoc, chestConfig.name(), inv);
                lootService.populateChest(disguisedChest, stage.chestDisguiseItems());
                activeChests.put(chestLoc.getBlock().getLocation(), disguisedChest);

                String holoId = "zevs_chest_" + chestId;
                hologramProvider.createHologram(holoId, chestLoc, chestConfig.hologramLines());
            }
        }
    }

    public DisguisedChest getDisguisedChest(Location loc) {
        if (loc == null) return null;
        return activeChests.get(loc.getBlock().getLocation());
    }

    public boolean canTakeChestItem(UUID playerUuid, long cooldownMillis) {
        long now = System.currentTimeMillis();
        long last = chestTakeCooldowns.getOrDefault(playerUuid, 0L);
        if (now - last < cooldownMillis) {
            return false;
        }
        chestTakeCooldowns.put(playerUuid, now);
        return true;
    }

    public long getRemainingCooldown(UUID playerUuid, long cooldownMillis) {
        long now = System.currentTimeMillis();
        long last = chestTakeCooldowns.getOrDefault(playerUuid, 0L);
        long diff = cooldownMillis - (now - last);
        return Math.max(0, diff);
    }

    private void removeChests() {
        for (Location loc : activeChests.keySet()) {
            if (loc.getWorld() != null) {
                Block block = loc.getBlock();
                if (block.getType() == Material.CHEST) {
                    block.setType(Material.AIR, false);
                }
            }
        }
        activeChests.clear();
        chestTakeCooldowns.clear();
    }

    private void initBossBar() {
        BarColor color;
        try {
            color = BarColor.valueOf(config.bossbarColor().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            color = BarColor.YELLOW;
        }

        BarStyle style;
        try {
            style = BarStyle.valueOf(config.bossbarStyle().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            style = BarStyle.SOLID;
        }

        activeBossBar = Bukkit.createBossBar("", color, style);
        activeBossBar.setVisible(true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            activeBossBar.addPlayer(player);
        }
    }

    private void updateBossBar() {
        if (activeBossBar == null) return;

        StageConfig stage = config.stage(currentStageNumber);
        if (stage == null) return;

        Map<String, String> placeholders = buildPlaceholders();
        String title = stage.bossbar();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            title = title.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        float progress = totalStageDuration > 0 ? (float) stageSecondsRemaining / totalStageDuration : 0.0f;
        progress = Math.max(0.0f, Math.min(1.0f, progress));

        activeBossBar.setTitle(ColorFormatter.colorize(title));
        activeBossBar.setProgress(progress);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!activeBossBar.getPlayers().contains(player)) {
                activeBossBar.addPlayer(player);
            }
        }
    }

    private void removeBossBar() {
        if (activeBossBar != null) {
            activeBossBar.removeAll();
            activeBossBar.setVisible(false);
            activeBossBar = null;
        }
    }

    private void cancelTasks() {
        if (mainTask != null) {
            mainTask.cancel();
            mainTask = null;
        }
        if (lightningTask != null) {
            lightningTask.cancel();
            lightningTask = null;
        }
        if (skyDropTask != null) {
            skyDropTask.cancel();
            skyDropTask = null;
        }
    }

    public Map<String, String> buildPlaceholders() {
        Map<String, String> map = new HashMap<>();
        if (eventCenter != null) {
            map.put("x", String.valueOf(eventCenter.getBlockX()));
            map.put("y", String.valueOf(eventCenter.getBlockY()));
            map.put("z", String.valueOf(eventCenter.getBlockZ()));
            map.put("world", eventCenter.getWorld() != null ? eventCenter.getWorld().getName() : "world");
        } else {
            map.put("x", "0");
            map.put("y", "0");
            map.put("z", "0");
            map.put("world", config.worldName());
        }
        map.put("timeLeft", String.valueOf(stageSecondsRemaining));
        map.put("stage", String.valueOf(currentStageNumber));
        return map;
    }

    private Location findRandomLocation(World world) {
        int min = config.distanceMin();
        int max = config.distanceMax();

        int maxWorldY = 256;
        try {
            maxWorldY = world.getMaxHeight();
        } catch (NoSuchMethodError | Exception ignored) {}

        for (int i = 0; i < config.searchMax(); i++) {
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double dist = min + ThreadLocalRandom.current().nextDouble() * (max - min);

            int x = (int) (Math.cos(angle) * dist);
            int z = (int) (Math.sin(angle) * dist);

            int highestY = world.getHighestBlockYAt(x, z);
            if (highestY > 0) {
                int targetY = highestY + config.yOffset();
                if (targetY < maxWorldY - 20) {
                    return new Location(world, x, targetY, z);
                }
            }
        }
        return null;
    }
}
