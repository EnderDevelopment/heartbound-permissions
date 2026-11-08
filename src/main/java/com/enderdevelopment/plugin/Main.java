package com.enderdevelopment.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements CommandExecutor, Listener {
    private HeartManager heartManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        heartManager = new HeartManager(this);
        getCommand("perm").setExecutor(this);
        getCommand("d").setExecutor(this);
        getCommand("t").setExecutor(this);
        getCommand("s").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, heartManager::updateAllOnlinePlayers, 20L, 20L);
    }

    @Override
    public void onDisable() {
        heartManager.updateAllOnlinePlayers();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        if (!player.isOp()) {
            player.sendMessage(Component.text("You must be an operator to use this command.", NamedTextColor.RED));
            return true;
        }

        if (command.getName().equals("perm")) {
            return handlePermanent(player, args);
        }
        return handleTimed(player, command.getName(), args);
    }

    private boolean handlePermanent(Player player, String[] args) {
        if (args.length != 2 || !args[0].equalsIgnoreCase("heart")) {
            sendUsage(player, "/perm heart <amount>");
            return true;
        }
        Integer hearts = parsePositiveInt(args[1]);
        if (hearts == null) {
            sendUsage(player, "/perm heart <amount>");
            return true;
        }
        heartManager.addPermanentHearts(player, hearts);
        player.sendMessage(Component.text("Added " + hearts + " permanent hearts.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleTimed(Player player, String unit, String[] args) {
        if ((args.length != 2 && args.length != 3) || !args[1].equalsIgnoreCase("heart")) {
            sendUsage(player, "/" + unit + " <time> heart [amount]");
            return true;
        }
        Long time = parsePositiveLong(args[0]);
        Integer hearts = args.length == 3 ? parsePositiveInt(args[2]) : 1;
        if (time == null || hearts == null) {
            sendUsage(player, "/" + unit + " <time> heart [amount]");
            return true;
        }
        long durationMillis;
        try {
            durationMillis = switch (unit) {
                case "s" -> Math.multiplyExact(time, 1000L);
                case "d" -> Math.multiplyExact(time, 86_400_000L);
                case "t" -> Math.multiplyExact(time, 50L);
                default -> throw new IllegalArgumentException("Unknown time unit");
            };
        } catch (ArithmeticException exception) {
            player.sendMessage(Component.text("The duration is too large.", NamedTextColor.RED));
            return true;
        }
        heartManager.addTimedHearts(player, hearts, durationMillis);
        player.sendMessage(Component.text("Added " + hearts + " hearts for " + time + " " + unit + ".", NamedTextColor.GREEN));
        return true;
    }

    private Integer parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long parsePositiveLong(String value) {
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void sendUsage(Player player, String usage) {
        player.sendMessage(Component.text("Usage: " + usage, NamedTextColor.YELLOW));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        heartManager.updateHealth(event.getPlayer());
    }
}
