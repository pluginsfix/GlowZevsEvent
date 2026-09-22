package pluginsfix.glowzevsevent.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import pluginsfix.glowzevsevent.config.EventConfig;
import pluginsfix.glowzevsevent.config.StageConfig;
import pluginsfix.glowzevsevent.domain.EventState;
import pluginsfix.glowzevsevent.service.EventService;

public final class ProtectionListener implements Listener {

    private final EventService eventService;
    private final EventConfig eventConfig;

    public ProtectionListener(EventService eventService, EventConfig eventConfig) {
        this.eventService = eventService;
        this.eventConfig = eventConfig;
    }

    private boolean isInsideEventZone(Location location) {
        if (!eventService.isRunning()) return false;
        Location center = eventService.getEventCenter();
        if (center == null || center.getWorld() == null || location.getWorld() == null) return false;
        if (!center.getWorld().equals(location.getWorld())) return false;

        int size = eventConfig.rgSize();
        return Math.abs(center.getBlockX() - location.getBlockX()) <= size
                && Math.abs(center.getBlockZ() - location.getBlockZ()) <= size
                && Math.abs(center.getBlockY() - location.getBlockY()) <= size;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;
        if (!isInsideEventZone(event.getEntity().getLocation())) return;

        EventState state = eventService.getState();
        if (state == EventState.STAGE_1 || state == EventState.STAGE_2) {
            StageConfig stageConfig = eventConfig.stage(state.ordinal());
            if (stageConfig != null && "DENY".equalsIgnoreCase(stageConfig.flags().get("PVP"))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!isInsideEventZone(event.getEntity().getLocation())) return;

        EventState state = eventService.getState();
        if (state == EventState.STAGE_1 || state == EventState.STAGE_2) {
            StageConfig stageConfig = eventConfig.stage(state.ordinal());
            if (stageConfig != null && "DENY".equalsIgnoreCase(stageConfig.flags().get("FALL-DAMAGE"))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isInsideEventZone(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isInsideEventZone(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
}
