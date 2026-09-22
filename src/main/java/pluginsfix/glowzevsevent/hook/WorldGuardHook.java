package pluginsfix.glowzevsevent.hook;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;

public final class WorldGuardHook {

    private final boolean available;
    private final String regionId = "glowzevsevent_zone";

    public WorldGuardHook() {
        this.available = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    public boolean isAvailable() {
        return available;
    }

    public void createRegion(Location center, int sizeRadius) {
        if (!available || center.getWorld() == null) return;

        World world = center.getWorld();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) return;

        int minWorldY = getMinHeight(world);
        int maxWorldY = getMaxHeight(world);

        int minX = center.getBlockX() - sizeRadius;
        int maxX = center.getBlockX() + sizeRadius;
        int minY = Math.max(minWorldY, center.getBlockY() - sizeRadius);
        int maxY = Math.min(maxWorldY, center.getBlockY() + sizeRadius);
        int minZ = center.getBlockZ() - sizeRadius;
        int maxZ = center.getBlockZ() + sizeRadius;

        BlockVector3 min = BlockVector3.at(minX, minY, minZ);
        BlockVector3 max = BlockVector3.at(maxX, maxY, maxZ);

        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, min, max);
        manager.addRegion(region);
    }

    public void applyFlags(World world, Map<String, String> flags) {
        if (!available || world == null) return;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) return;

        ProtectedRegion region = manager.getRegion(regionId);
        if (region == null) return;

        for (Map.Entry<String, String> entry : flags.entrySet()) {
            String flagName = entry.getKey();
            String flagValue = entry.getValue();
            Flag<?> flag = Flags.fuzzyMatchFlag(WorldGuard.getInstance().getFlagRegistry(), flagName);
            if (flag instanceof StateFlag stateFlag) {
                StateFlag.State state = "ALLOW".equalsIgnoreCase(flagValue) ? StateFlag.State.ALLOW : StateFlag.State.DENY;
                region.setFlag(stateFlag, state);
            }
        }
    }

    public void removeRegion(World world) {
        if (!available || world == null) return;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) return;

        manager.removeRegion(regionId);
    }

    private int getMinHeight(World world) {
        try {
            return world.getMinHeight();
        } catch (NoSuchMethodError | Exception e) {
            return 0;
        }
    }

    private int getMaxHeight(World world) {
        try {
            return world.getMaxHeight();
        } catch (NoSuchMethodError | Exception e) {
            return 256;
        }
    }
}
