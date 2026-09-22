package pluginsfix.glowzevsevent.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import pluginsfix.glowzevsevent.config.LootConfig;
import pluginsfix.glowzevsevent.service.EventService;
import pluginsfix.glowzevsevent.text.Messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class GlowZevsEventCommand implements CommandExecutor, TabCompleter {

    private final EventService eventService;
    private final LootConfig lootConfig;
    private final Messages messages;
    private final Runnable reloadAction;

    public GlowZevsEventCommand(EventService eventService, LootConfig lootConfig, Messages messages, Runnable reloadAction) {
        this.eventService = eventService;
        this.lootConfig = lootConfig;
        this.messages = messages;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("glowzevsevent.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            messages.sendList(sender, "help");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "start" -> {
                if (eventService.isRunning()) {
                    messages.send(sender, "event-already-running");
                    return true;
                }
                boolean started = eventService.startEvent();
                if (started) {
                    messages.send(sender, "event-started");
                } else {
                    messages.send(sender, "location-not-found");
                }
                return true;
            }
            case "stop" -> {
                if (!eventService.isRunning()) {
                    messages.send(sender, "event-not-running");
                    return true;
                }
                eventService.stopEvent();
                messages.send(sender, "event-stopped");
                return true;
            }
            case "reload" -> {
                reloadAction.run();
                messages.send(sender, "config-reloaded");
                return true;
            }
            case "add" -> {
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "player-only");
                    return true;
                }
                if (args.length < 3) {
                    messages.sendList(sender, "help");
                    return true;
                }

                String dropType = args[1].toLowerCase(Locale.ROOT);
                if (!dropType.equals("lightning") && !dropType.equals("chest") && !dropType.equals("skydrop")) {
                    messages.send(player, "unknown-type");
                    return true;
                }

                double chance;
                try {
                    chance = Double.parseDouble(args[2]);
                    if (chance <= 0 || chance > 100) {
                        messages.send(player, "invalid-chance");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    messages.send(player, "invalid-chance");
                    return true;
                }

                ItemStack inHand = player.getInventory().getItemInMainHand();
                if (inHand.isEmpty()) {
                    messages.send(player, "item-in-hand-empty");
                    return true;
                }

                boolean added = lootConfig.addItem(dropType, inHand.clone(), chance);
                if (added) {
                    messages.send(player, "drop-added", "type", dropType, "chance", String.valueOf(chance));
                }
                return true;
            }
            default -> {
                messages.sendList(sender, "help");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("glowzevsevent.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> list = List.of("start", "stop", "add", "reload");
            return list.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            List<String> list = List.of("lightning", "chest", "skydrop");
            return list.stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            return List.of("10", "25", "50", "75", "100");
        }

        return Collections.emptyList();
    }
}
