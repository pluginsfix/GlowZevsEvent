package pluginsfix.glowzevsevent.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public final class SchematicService {

    private final Plugin plugin;
    private final Set<BlockPosition> modifiedBlocks = new HashSet<>();

    private record BlockPosition(String world, int x, int y, int z) {}

    public SchematicService(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean pasteSchematic(String schemName, Location location) {
        modifiedBlocks.clear();
        if (location == null || location.getWorld() == null) return false;

        File schemFile = findSchematicFile(schemName);
        if (schemFile == null || !schemFile.exists()) {
            plugin.getLogger().warning("Schematic file not found: " + schemName);
            return false;
        }

        try {
            return pasteDirectNbt(schemFile, location);
        } catch (Throwable e) {
            plugin.getLogger().warning("Direct schematic paste failed, trying fallback: " + e.getMessage());
            return pasteWorldEditFallback(schemFile, location);
        }
    }

    private boolean pasteDirectNbt(File file, Location location) {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
            byte rootType = in.readByte();
            if (rootType != 10) return false;
            in.readUTF();

            int width = 0;
            int height = 0;
            int length = 0;
            int offsetX = 0;
            int offsetY = 0;
            int offsetZ = 0;
            Map<Integer, String> palette = new HashMap<>();
            byte[] blockDataBytes = null;

            Stack<String> compoundStack = new Stack<>();
            compoundStack.push("ROOT");

            while (!compoundStack.isEmpty()) {
                byte tagType = in.readByte();
                if (tagType == 0) {
                    compoundStack.pop();
                    continue;
                }

                String tagName = in.readUTF();

                if (tagType == 2) {
                    short val = in.readShort();
                    if ("Width".equalsIgnoreCase(tagName)) width = val;
                    else if ("Height".equalsIgnoreCase(tagName)) height = val;
                    else if ("Length".equalsIgnoreCase(tagName)) length = val;
                } else if (tagType == 3) {
                    int val = in.readInt();
                    if ("Palette".equalsIgnoreCase(compoundStack.peek())) {
                        palette.put(val, tagName);
                    }
                } else if (tagType == 7) {
                    int len = in.readInt();
                    byte[] bytes = new byte[len];
                    in.readFully(bytes);
                    if ("BlockData".equalsIgnoreCase(tagName)) {
                        blockDataBytes = bytes;
                    }
                } else if (tagType == 10) {
                    compoundStack.push(tagName);
                } else if (tagType == 11) {
                    int len = in.readInt();
                    int[] ints = new int[len];
                    for (int i = 0; i < len; i++) ints[i] = in.readInt();
                    if ("Offset".equalsIgnoreCase(tagName) && len >= 3) {
                        offsetX = ints[0];
                        offsetY = ints[1];
                        offsetZ = ints[2];
                    }
                } else {
                    skipTagPayload(in, tagType);
                }
            }

            if (width <= 0 || height <= 0 || length <= 0 || blockDataBytes == null || palette.isEmpty()) {
                return false;
            }

            World world = location.getWorld();
            int originX = location.getBlockX() + offsetX;
            int originY = location.getBlockY() + offsetY;
            int originZ = location.getBlockZ() + offsetZ;

            int byteIndex = 0;
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        if (byteIndex >= blockDataBytes.length) break;

                        int palId = 0;
                        int shift = 0;
                        while (true) {
                            byte b = blockDataBytes[byteIndex++];
                            palId |= (b & 0x7F) << shift;
                            if ((b & 0x80) == 0) break;
                            shift += 7;
                        }

                        String blockStr = palette.get(palId);
                        if (blockStr != null && !blockStr.contains("air")) {
                            int targetX = originX + x;
                            int targetY = originY + y;
                            int targetZ = originZ + z;

                            Block targetBlock = world.getBlockAt(targetX, targetY, targetZ);
                            try {
                                BlockData bd = Bukkit.createBlockData(blockStr);
                                targetBlock.setBlockData(bd, false);
                                modifiedBlocks.add(new BlockPosition(world.getName(), targetX, targetY, targetZ));
                            } catch (Exception ignored) {
                                Material mat = Material.matchMaterial(blockStr.split("\\[")[0].replace("minecraft:", "").toUpperCase(Locale.ROOT));
                                if (mat != null && mat != Material.AIR) {
                                    targetBlock.setType(mat, false);
                                    modifiedBlocks.add(new BlockPosition(world.getName(), targetX, targetY, targetZ));
                                }
                            }
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing schematic NBT: " + e.getMessage());
            return false;
        }
    }

    private void skipTagPayload(DataInputStream in, byte tagType) throws IOException {
        switch (tagType) {
            case 1 -> in.skipBytes(1);
            case 2 -> in.skipBytes(2);
            case 3 -> in.skipBytes(4);
            case 4 -> in.skipBytes(8);
            case 5 -> in.skipBytes(4);
            case 6 -> in.skipBytes(8);
            case 7 -> {
                int len = in.readInt();
                in.skipBytes(len);
            }
            case 8 -> {
                int len = in.readUnsignedShort();
                in.skipBytes(len);
            }
            case 9 -> {
                byte elemType = in.readByte();
                int count = in.readInt();
                for (int i = 0; i < count; i++) {
                    skipTagPayload(in, elemType);
                }
            }
            case 11 -> {
                int len = in.readInt();
                in.skipBytes(len * 4);
            }
            case 12 -> {
                int len = in.readInt();
                in.skipBytes(len * 8);
            }
        }
    }

    private boolean pasteWorldEditFallback(File file, Location location) {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldEdit")
                && !Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")) {
            return false;
        }
        try {
            com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat format =
                    com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats.findByFile(file);
            if (format == null) return false;

            try (com.sk89q.worldedit.extent.clipboard.io.ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                com.sk89q.worldedit.extent.clipboard.Clipboard clipboard = reader.read();
                com.sk89q.worldedit.world.World weWorld = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location.getWorld());

                try (com.sk89q.worldedit.EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(weWorld)) {
                    com.sk89q.worldedit.function.operation.Operation op = new com.sk89q.worldedit.session.ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .to(com.sk89q.worldedit.math.BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                            .ignoreAirBlocks(false)
                            .build();
                    com.sk89q.worldedit.function.operation.Operations.complete(op);
                }
                return true;
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("WorldEdit paste fallback failed: " + t.getMessage());
            return false;
        }
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
        File f1 = new File(plugin.getDataFolder(), "schematics/" + name);
        if (f1.exists()) return f1;

        File f2 = new File(plugin.getDataFolder(), name);
        if (f2.exists()) return f2;

        File f3 = new File("plugins/WorldEdit/schematics", name);
        if (f3.exists()) return f3;

        File f4 = new File("plugins/FastAsyncWorldEdit/schematics", name);
        if (f4.exists()) return f4;

        return f1;
    }
}
