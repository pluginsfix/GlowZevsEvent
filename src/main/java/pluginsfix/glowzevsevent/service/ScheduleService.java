package pluginsfix.glowzevsevent.service;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pluginsfix.glowzevsevent.config.EventConfig;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ScheduleService {

    private final Plugin plugin;
    private final EventConfig config;
    private final EventService eventService;
    private final ActionExecutor actionExecutor;

    private BukkitTask timerTask;
    private String lastTriggeredMinute = "";

    public ScheduleService(Plugin plugin, EventConfig config, EventService eventService, ActionExecutor actionExecutor) {
        this.plugin = plugin;
        this.config = config;
        this.eventService = eventService;
        this.actionExecutor = actionExecutor;
    }

    public void start() {
        if (!config.timeEnabled()) return;

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkSchedule();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void stop() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    private void checkSchedule() {
        if (eventService.isRunning()) return;

        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(config.timeZone());
        } catch (Exception e) {
            zoneId = ZoneId.of("Europe/Moscow");
        }

        DateTimeFormatter formatter;
        try {
            formatter = DateTimeFormatter.ofPattern(config.timeFormat());
        } catch (Exception e) {
            formatter = DateTimeFormatter.ofPattern("HH:mm");
        }

        ZonedDateTime now = ZonedDateTime.now(zoneId);
        String currentTimeStr = now.format(formatter);

        if (currentTimeStr.equals(lastTriggeredMinute)) return;

        List<String> startTimes = config.timeStarts();
        if (startTimes.contains(currentTimeStr)) {
            lastTriggeredMinute = currentTimeStr;
            int online = Bukkit.getOnlinePlayers().size();
            if (online < config.minPlayers()) {
                actionExecutor.execute(config.actionList("minPlayers"), null, eventService.buildPlaceholders());
            } else {
                eventService.startEvent();
            }
        }
    }
}
