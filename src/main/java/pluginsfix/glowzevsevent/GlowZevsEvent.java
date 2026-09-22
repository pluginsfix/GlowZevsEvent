package pluginsfix.glowzevsevent;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import pluginsfix.glowzevsevent.command.GlowZevsEventCommand;
import pluginsfix.glowzevsevent.config.EventConfig;
import pluginsfix.glowzevsevent.config.LootConfig;
import pluginsfix.glowzevsevent.hook.*;
import pluginsfix.glowzevsevent.listener.ChestListener;
import pluginsfix.glowzevsevent.listener.ProtectionListener;
import pluginsfix.glowzevsevent.service.*;
import pluginsfix.glowzevsevent.text.Messages;

public final class GlowZevsEvent extends JavaPlugin {

    private Messages messages;
    private EventConfig eventConfig;
    private LootConfig lootConfig;
    private PlaceholderHook placeholderHook;
    private WorldGuardHook worldGuardHook;
    private HologramProvider hologramProvider;
    private ActionExecutor actionExecutor;
    private SchematicService schematicService;
    private LootService lootService;
    private EventService eventService;
    private ScheduleService scheduleService;

    @Override
    public void onEnable() {
        this.messages = new Messages(this);
        this.eventConfig = EventConfig.load(this);
        this.lootConfig = new LootConfig(this);

        this.placeholderHook = new PlaceholderHook();
        this.worldGuardHook = new WorldGuardHook();

        if ("DecentHolograms".equalsIgnoreCase(eventConfig.hologramProvider())
                && getServer().getPluginManager().isPluginEnabled("DecentHolograms")) {
            this.hologramProvider = new DecentHologramsProvider();
        } else {
            this.hologramProvider = new VanillaHologramProvider();
        }

        this.actionExecutor = new ActionExecutor(placeholderHook);
        this.schematicService = new SchematicService(this);
        this.lootService = new LootService(lootConfig);
        this.eventService = new EventService(
                this,
                eventConfig,
                actionExecutor,
                schematicService,
                lootService,
                worldGuardHook,
                hologramProvider
        );
        this.scheduleService = new ScheduleService(this, eventConfig, eventService, actionExecutor);

        PluginCommand cmd = getCommand("glowzevsevent");
        if (cmd != null) {
            GlowZevsEventCommand executor = new GlowZevsEventCommand(eventService, lootConfig, messages, this::reloadAll);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getServer().getPluginManager().registerEvents(new ChestListener(eventService, eventConfig, messages), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(eventService, eventConfig), this);

        this.scheduleService.start();
    }

    @Override
    public void onDisable() {
        if (scheduleService != null) {
            scheduleService.stop();
        }
        if (eventService != null) {
            eventService.stopEvent();
        }
        if (hologramProvider != null) {
            hologramProvider.removeAll();
        }
    }

    private void reloadAll() {
        if (scheduleService != null) {
            scheduleService.stop();
        }
        this.messages.reload();
        this.eventConfig = EventConfig.load(this);
        this.lootConfig.load();
        this.scheduleService = new ScheduleService(this, eventConfig, eventService, actionExecutor);
        this.scheduleService.start();
    }
}
