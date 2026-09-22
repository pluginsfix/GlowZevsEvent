package pluginsfix.glowzevsevent.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pluginsfix.glowzevsevent.config.EventConfig;
import pluginsfix.glowzevsevent.config.StageConfig;
import pluginsfix.glowzevsevent.domain.DisguisedChest;
import pluginsfix.glowzevsevent.service.EventService;
import pluginsfix.glowzevsevent.text.Messages;

public final class ChestListener implements Listener {

    private final EventService eventService;
    private final EventConfig eventConfig;
    private final Messages messages;

    public ChestListener(EventService eventService, EventConfig eventConfig, Messages messages) {
        this.eventService = eventService;
        this.eventConfig = eventConfig;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Chest)) return;

        if (!eventService.isRunning()) return;

        DisguisedChest chest = eventService.getDisguisedChest(block.getLocation());
        if (chest == null) return;

        if (eventService.getState().ordinal() < 3) {
            StageConfig currentStage = eventConfig.stage(eventService.getState().ordinal());
            if (currentStage != null && "DENY".equalsIgnoreCase(currentStage.flags().get("CHEST-ACCESS"))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!eventService.isRunning()) return;

        Inventory clickedInv = event.getClickedInventory();
        Inventory topInv = event.getView().getTopInventory();

        if (topInv.getLocation() == null) return;
        DisguisedChest chest = eventService.getDisguisedChest(topInv.getLocation());
        if (chest == null) return;

        StageConfig stage3 = eventConfig.stage(3);
        long cooldownMillis = stage3 != null ? stage3.chestTakeCooldownMillis() : 1100L;

        if (clickedInv != null && clickedInv.equals(topInv)) {
            int slot = event.getSlot();
            if (chest.getRealItems().containsKey(slot)) {
                if (!chest.isRevealed(slot)) {
                    event.setCancelled(true);

                    if (!eventService.canTakeChestItem(player.getUniqueId(), cooldownMillis)) {
                        long rem = eventService.getRemainingCooldown(player.getUniqueId(), cooldownMillis);
                        String formatted = String.format("%.1f", rem / 1000.0);
                        messages.send(player, "chest-cooldown", "time", formatted);
                        return;
                    }

                    ItemStack real = chest.getRealItems().get(slot);
                    chest.reveal(slot);
                    topInv.setItem(slot, real);
                    return;
                }
            }
        }

        if (event.isShiftClick()) {
            if (clickedInv != null && clickedInv.equals(topInv)) {
                int slot = event.getSlot();
                if (chest.getRealItems().containsKey(slot) && !chest.isRevealed(slot)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!eventService.isRunning()) return;

        Inventory topInv = event.getView().getTopInventory();
        if (topInv.getLocation() == null) return;

        DisguisedChest chest = eventService.getDisguisedChest(topInv.getLocation());
        if (chest != null) {
            for (int slot : event.getRawSlots()) {
                if (slot < topInv.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
