package pluginsfix.glowzevsevent.hook;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;
import pluginsfix.glowzevsevent.text.ColorFormatter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DecentHologramsProvider implements HologramProvider {

    private final Set<String> activeHolograms = new HashSet<>();

    @Override
    public void createHologram(String id, Location location, List<String> lines) {
        removeHologram(id);
        List<String> colorizedLines = lines.stream().map(ColorFormatter::colorize).toList();
        Hologram hologram = DHAPI.createHologram(id, location.clone().add(0.5, 1.8, 0.5), colorizedLines);
        if (hologram != null) {
            activeHolograms.add(id);
        }
    }

    @Override
    public void removeHologram(String id) {
        if (activeHolograms.remove(id) || DHAPI.getHologram(id) != null) {
            try {
                DHAPI.removeHologram(id);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void removeAll() {
        for (String id : new HashSet<>(activeHolograms)) {
            removeHologram(id);
        }
        activeHolograms.clear();
    }
}
