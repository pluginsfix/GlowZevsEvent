package pluginsfix.glowzevsevent;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class SchematicBuilderTest {

    @Test
    void generateShipSchematic() throws Exception {
        int width = 31;   // X: -10 to +20 (center x=10)
        int height = 18;  // Y: -5 to +12 (center y=5)
        int length = 15;  // Z: -7 to +7 (center z=7)

        int originX = 10;
        int originY = 5;
        int originZ = 7;

        Map<String, Integer> palette = new LinkedHashMap<>();
        palette.put("minecraft:air", 0);
        palette.put("minecraft:spruce_planks", 1);
        palette.put("minecraft:spruce_log[axis=y]", 2);
        palette.put("minecraft:spruce_log[axis=x]", 3);
        palette.put("minecraft:spruce_fence", 4);
        palette.put("minecraft:spruce_slab[type=bottom]", 5);
        palette.put("minecraft:dark_oak_stairs[facing=east,half=bottom,shape=straight]", 6);
        palette.put("minecraft:acacia_wood", 7);
        palette.put("minecraft:acacia_trapdoor[facing=north,half=bottom,open=false]", 8);
        palette.put("minecraft:white_wool", 9);
        palette.put("minecraft:cyan_wool", 10);
        palette.put("minecraft:snow", 11);
        palette.put("minecraft:gold_block", 12);
        palette.put("minecraft:lantern[hanging=false]", 13);
        palette.put("minecraft:chain[axis=y]", 14);

        int totalBlocks = width * height * length;
        byte[] blockData = new byte[totalBlocks];

        autoFillShip(blockData, width, height, length, originX, originY, originZ, palette);

        File resDir = new File("src/main/resources/schematics");
        resDir.mkdirs();
        File outFile = new File(resDir, "ship.schem");
        File rootFile = new File("src/main/resources/ship.schem");

        writeSchemFile(outFile, width, height, length, originX, originY, originZ, palette, blockData);
        Files.copy(outFile.toPath(), rootFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        System.out.println("Ship schematic successfully generated at " + outFile.getAbsolutePath());
    }

    private void autoFillShip(byte[] blocks, int w, int h, int l, int ox, int oy, int oz, Map<String, Integer> pal) {
        int AIR = pal.get("minecraft:air");
        int PLANKS = pal.get("minecraft:spruce_planks");
        int LOG_Y = pal.get("minecraft:spruce_log[axis=y]");
        int LOG_X = pal.get("minecraft:spruce_log[axis=x]");
        int FENCE = pal.get("minecraft:spruce_fence");
        int SLAB = pal.get("minecraft:spruce_slab[type=bottom]");
        int STAIRS = pal.get("minecraft:dark_oak_stairs[facing=east,half=bottom,shape=straight]");
        int ACACIA = pal.get("minecraft:acacia_wood");
        int TRAPDOOR = pal.get("minecraft:acacia_trapdoor[facing=north,half=bottom,open=false]");
        int WHITE = pal.get("minecraft:white_wool");
        int CYAN = pal.get("minecraft:cyan_wool");
        int SNOW = pal.get("minecraft:snow");
        int GOLD = pal.get("minecraft:gold_block");
        int LANTERN = pal.get("minecraft:lantern[hanging=false]");

        for (int x = -9; x <= 18; x++) {
            for (int z = -4; z <= 4; z++) {
                int distFromCenterZ = Math.abs(z);
                int boatWidth = 4 - (int) (Math.abs(x - 4) * 0.2);
                if (boatWidth < 1) boatWidth = 1;

                if (distFromCenterZ <= boatWidth) {
                    // Keel / Bottom hull
                    setBlock(blocks, w, h, l, ox + x, oy - 4, oz + z, LOG_X);
                    setBlock(blocks, w, h, l, ox + x, oy - 3, oz + z, PLANKS);
                    setBlock(blocks, w, h, l, ox + x, oy - 2, oz + z, PLANKS);
                    setBlock(blocks, w, h, l, ox + x, oy - 1, oz + z, PLANKS);
                    setBlock(blocks, w, h, l, ox + x, oy + 0, oz + z, PLANKS);

                    if (distFromCenterZ == boatWidth) {
                        setBlock(blocks, w, h, l, ox + x, oy + 1, oz + z, FENCE);
                        setBlock(blocks, w, h, l, ox + x, oy + 0, oz + z, ACACIA);
                    }
                }
            }
        }

        // Stern & Bow detailing
        for (int y = -3; y <= 2; y++) {
            setBlock(blocks, w, h, l, ox + 17, oy + y, oz, GOLD);
            setBlock(blocks, w, h, l, ox - 9, oy + y, oz, ACACIA);
        }

        // Mast 1 (center x=4)
        for (int y = 1; y <= 11; y++) {
            setBlock(blocks, w, h, l, ox + 4, oy + y, oz, LOG_Y);
        }
        // Mast 2 (bow x=12)
        for (int y = 1; y <= 9; y++) {
            setBlock(blocks, w, h, l, ox + 12, oy + y, oz, LOG_Y);
        }
        // Mast 3 (stern x=-4)
        for (int y = 1; y <= 8; y++) {
            setBlock(blocks, w, h, l, ox - 4, oy + y, oz, LOG_Y);
        }

        // Sails on center mast
        for (int y = 4; y <= 9; y++) {
            for (int z = -4; z <= 4; z++) {
                int mat = (Math.abs(z) == 1 || Math.abs(z) == 3) ? CYAN : WHITE;
                setBlock(blocks, w, h, l, ox + 4, oy + y, oz + z, mat);
                if (y == 9) {
                    setBlock(blocks, w, h, l, ox + 4, oy + y + 1, oz + z, SNOW);
                }
            }
        }

        // Sails on bow mast
        for (int y = 3; y <= 7; y++) {
            for (int z = -3; z <= 3; z++) {
                int mat = (Math.abs(z) == 0 || Math.abs(z) == 2) ? CYAN : WHITE;
                setBlock(blocks, w, h, l, ox + 12, oy + y, oz + z, mat);
            }
        }

        // Lanterns on deck
        setBlock(blocks, w, h, l, ox + 15, oy + 1, oz + 2, LANTERN);
        setBlock(blocks, w, h, l, ox + 15, oy + 1, oz - 2, LANTERN);
        setBlock(blocks, w, h, l, ox - 7, oy + 1, oz + 2, LANTERN);
        setBlock(blocks, w, h, l, ox - 7, oy + 1, oz - 2, LANTERN);
    }

    private void setBlock(byte[] blocks, int w, int h, int l, int x, int y, int z, int id) {
        if (x < 0 || x >= w || y < 0 || y >= h || z < 0 || z >= l) return;
        int index = y * (w * l) + z * w + x;
        blocks[index] = (byte) id;
    }

    private void writeSchemFile(File file, int w, int h, int l, int ox, int oy, int oz, Map<String, Integer> pal, byte[] blockData) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        // Root compound
        out.writeByte(10); // TAG_Compound
        out.writeUTF("Schematic");

        // Version = 2
        out.writeByte(3); // TAG_Int
        out.writeUTF("Version");
        out.writeInt(2);

        // DataVersion = 2586 (1.16.5)
        out.writeByte(3); // TAG_Int
        out.writeUTF("DataVersion");
        out.writeInt(2586);

        // Width, Height, Length
        out.writeByte(2); // TAG_Short
        out.writeUTF("Width");
        out.writeShort(w);

        out.writeByte(2); // TAG_Short
        out.writeUTF("Height");
        out.writeShort(h);

        out.writeByte(2); // TAG_Short
        out.writeUTF("Length");
        out.writeShort(l);

        // Offset
        out.writeByte(11); // TAG_Int_Array
        out.writeUTF("Offset");
        out.writeInt(3);
        out.writeInt(-ox);
        out.writeInt(-oy);
        out.writeInt(-oz);

        // PaletteMax
        out.writeByte(3); // TAG_Int
        out.writeUTF("PaletteMax");
        out.writeInt(pal.size());

        // Palette Compound
        out.writeByte(10); // TAG_Compound
        out.writeUTF("Palette");
        for (Map.Entry<String, Integer> entry : pal.entrySet()) {
            out.writeByte(3); // TAG_Int
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue());
        }
        out.writeByte(0); // TAG_End Palette

        // BlockData
        out.writeByte(7); // TAG_Byte_Array
        out.writeUTF("BlockData");
        out.writeInt(blockData.length);
        out.write(blockData);

        out.writeByte(0); // TAG_End Root

        out.flush();

        try (GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(file))) {
            gzos.write(baos.toByteArray());
        }
    }
}
