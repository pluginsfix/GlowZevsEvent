package pluginsfix.glowzevsevent.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import pluginsfix.glowzevsevent.text.ColorFormatter;

import java.util.*;

public final class VanillaHologramProvider implements HologramProvider {

    private final Map<String, List<UUID>> holograms = new HashMap<>();

    @Override
    public void createHologram(String id, Location location, List<String> lines) {
        removeHologram(id);
        if (location.getWorld() == null || lines.isEmpty()) return;

        List<UUID> standUuids = new ArrayList<>();
        double yOffset = 1.6;
        double lineSpacing = 0.28;

        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            Location spawnLoc = location.clone().add(0.5, yOffset, 0.5);
            ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCanPickupItems(false);
            stand.setCustomName(ColorFormatter.colorize(line));
            stand.setCustomNameVisible(true);
            stand.setMarker(true);
            stand.setInvulnerable(true);
            standUuids.add(stand.getUniqueId());
            yOffset += lineSpacing;
        }

        holograms.put(id, standUuids);
    }

    @Override
    public void removeHologram(String id) {
        List<UUID> uuids = holograms.remove(id);
        if (uuids != null) {
            for (UUID uuid : uuids) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity != null) {
                    entity.remove();
                }
            }
        }
    }

    @Override
    public void removeAll() {
        for (List<UUID> uuids : holograms.values()) {
            for (UUID uuid : uuids) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity != null) {
                    entity.remove();
                }
            }
        }
        holograms.clear();
    }
}
