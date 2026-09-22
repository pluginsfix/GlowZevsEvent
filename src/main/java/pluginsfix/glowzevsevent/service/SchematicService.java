package pluginsfix.glowzevsevent.service;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SchematicService {

    private final Plugin plugin;
    private final boolean worldEditAvailable;
    private final Set<BlockPosition> modifiedBlocks = new HashSet<>();

    private record BlockPosition(String world, int x, int y, int z) {}

    public SchematicService(Plugin plugin) {
        this.plugin = plugin;
        this.worldEditAvailable = Bukkit.getPluginManager().isPluginEnabled("WorldEdit")
                || Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit");
    }

    public boolean pasteSchematic(String schemName, Location location) {
        modifiedBlocks.clear();
        if (location == null || location.getWorld() == null) return false;

        File schemFile = findSchematicFile(schemName);
        if (schemFile == null || !schemFile.exists()) {
            plugin.getLogger().warning("Schematic file not found: " + schemName);
            return false;
        }

        if (worldEditAvailable) {
            try {
                ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
                if (format == null) return false;

                try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
                    Clipboard clipboard = reader.read();
                    com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(location.getWorld());

                    try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                        Operation operation = new ClipboardHolder(clipboard)
                                .createPaste(editSession)
                                .to(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                                .ignoreAirBlocks(false)
                                .build();
                        Operations.complete(operation);
                    }

                    BlockVector3 min = clipboard.getMinimumPoint();
                    BlockVector3 max = clipboard.getMaximumPoint();
                    BlockVector3 origin = clipboard.getOrigin();

                    int dx = location.getBlockX() - origin.x();
                    int dy = location.getBlockY() - origin.y();
                    int dz = location.getBlockZ() - origin.z();

                    for (int x = min.x(); x <= max.x(); x++) {
                        for (int y = min.y(); y <= max.y(); y++) {
                            for (int z = min.z(); z <= max.z(); z++) {
                                modifiedBlocks.add(new BlockPosition(
                                        location.getWorld().getName(),
                                        x + dx,
                                        y + dy,
                                        z + dz
                                ));
                            }
                        }
                    }
                    return true;
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to paste schematic via WorldEdit: " + e.getMessage());
            }
        }
        return false;
    }

    public void clearSchematic(int batchSize, int clearDelayTicks, Runnable onComplete) {
        if (modifiedBlocks.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        List<BlockPosition> positions = new ArrayList<>(modifiedBlocks);
        modifiedBlocks.clear();

        new BukkitRunnable() {
            private int index = 0;

            @Override
            public void run() {
                int processed = 0;
                while (index < positions.size() && processed < batchSize) {
                    BlockPosition pos = positions.get(index++);
                    World world = Bukkit.getWorld(pos.world());
                    if (world != null) {
                        Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
                        if (block.getType() != Material.AIR) {
                            block.setType(Material.AIR, false);
                        }
                    }
                    processed++;
                }

                if (index >= positions.size()) {
                    cancel();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, Math.max(1, clearDelayTicks));
    }

    private File findSchematicFile(String name) {
        File f1 = new File("plugins/WorldEdit/schematics", name);
        if (f1.exists()) return f1;

        File f2 = new File("plugins/FastAsyncWorldEdit/schematics", name);
        if (f2.exists()) return f2;

        File f3 = new File(plugin.getDataFolder(), "schematics/" + name);
        if (f3.exists()) return f3;

        File f4 = new File(plugin.getDataFolder(), name);
        if (f4.exists()) return f4;

        return f1;
    }
}
