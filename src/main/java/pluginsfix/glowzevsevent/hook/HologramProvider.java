package pluginsfix.glowzevsevent.hook;

import org.bukkit.Location;

import java.util.List;

public interface HologramProvider {
    void createHologram(String id, Location location, List<String> lines);
    void removeHologram(String id);
    void removeAll();
}
