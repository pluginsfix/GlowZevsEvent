package pluginsfix.glowzevsevent.service;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pluginsfix.glowzevsevent.config.LootConfig;
import pluginsfix.glowzevsevent.domain.DisguisedChest;
import pluginsfix.glowzevsevent.text.ColorFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class LootService {

    private final LootConfig lootConfig;

    public LootService(LootConfig lootConfig) {
        this.lootConfig = lootConfig;
    }

    public ItemStack rollLightningDrop() {
        return rollSingleDrop(lootConfig.getLightningDrops());
    }

    public ItemStack rollSkyDrop() {
        return rollSingleDrop(lootConfig.getSkyDrops());
    }

    private ItemStack rollSingleDrop(List<LootConfig.LootItem> items) {
        if (items.isEmpty()) return null;
        List<LootConfig.LootItem> shuffled = new ArrayList<>(items);
        Collections.shuffle(shuffled);
        for (LootConfig.LootItem entry : shuffled) {
            double roll = ThreadLocalRandom.current().nextDouble(0.0, 100.0);
            if (roll <= entry.chance()) {
                return entry.itemStack().clone();
            }
        }
        return null;
    }

    public void populateChest(DisguisedChest chest, List<String> disguiseTemplates) {
        Inventory inv = chest.getInventory();
        inv.clear();

        List<LootConfig.LootItem> chestItems = lootConfig.getChestDrops();
        if (chestItems.isEmpty()) return;

        int size = inv.getSize();
        int itemsToPlace = Math.min(size, ThreadLocalRandom.current().nextInt(5, 12));

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < size; i++) slots.add(i);
        Collections.shuffle(slots);

        for (int i = 0; i < itemsToPlace && i < slots.size(); i++) {
            ItemStack realItem = rollSingleDrop(chestItems);
            if (realItem == null && !chestItems.isEmpty()) {
                realItem = chestItems.get(ThreadLocalRandom.current().nextInt(chestItems.size())).itemStack().clone();
            }
            if (realItem == null) continue;

            int slot = slots.get(i);
            chest.setRealItem(slot, realItem);

            ItemStack disguiseItem = createDisguiseItem(disguiseTemplates);
            inv.setItem(slot, disguiseItem);
        }
    }

    private ItemStack createDisguiseItem(List<String> disguiseTemplates) {
        if (disguiseTemplates != null && !disguiseTemplates.isEmpty()) {
            String template = disguiseTemplates.get(ThreadLocalRandom.current().nextInt(disguiseTemplates.size()));
            String[] parts = template.split(";", 2);
            Material mat = Material.matchMaterial(parts[0]);
            if (mat != null) {
                ItemStack item = new ItemStack(mat);
                if (parts.length > 1) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        ColorFormatter.applyItemMeta(meta, parts[1], null);
                        item.setItemMeta(meta);
                    }
                }
                return item;
            }
        }
        return new ItemStack(Material.GRAY_DYE);
    }
}
