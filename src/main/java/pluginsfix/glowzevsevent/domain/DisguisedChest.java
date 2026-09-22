package pluginsfix.glowzevsevent.domain;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class DisguisedChest {

    private final Location location;
    private final String name;
    private final Inventory inventory;
    private final Map<Integer, ItemStack> realItems = new HashMap<>();
    private final Map<Integer, Boolean> revealedSlots = new HashMap<>();

    public DisguisedChest(Location location, String name, Inventory inventory) {
        this.location = location;
        this.name = name;
        this.inventory = inventory;
    }

    public Location getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Map<Integer, ItemStack> getRealItems() {
        return realItems;
    }

    public Map<Integer, Boolean> getRevealedSlots() {
        return revealedSlots;
    }

    public void setRealItem(int slot, ItemStack realItem) {
        realItems.put(slot, realItem);
        revealedSlots.put(slot, false);
    }

    public boolean isRevealed(int slot) {
        return revealedSlots.getOrDefault(slot, false);
    }

    public void reveal(int slot) {
        revealedSlots.put(slot, true);
    }
}
