package com.enderdevelopment.plugin;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HeartManager {
    private final Main plugin;
    private final Map<UUID, Integer> permanentHearts = new HashMap<>();
    private final Map<UUID, List<TimedHearts>> timedHearts = new HashMap<>();

    public HeartManager(Main plugin) {
        this.plugin = plugin;
        load();
    }

    public void addPermanentHearts(Player player, int hearts) {
        permanentHearts.merge(player.getUniqueId(), hearts, Integer::sum);
        save();
        updateHealth(player);
    }

    public void addTimedHearts(Player player, int hearts, long durationMillis) {
        timedHearts.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayList<>())
                .add(new TimedHearts(hearts, System.currentTimeMillis() + durationMillis));
        save();
        updateHealth(player);
    }

    public void updateHealth(Player player) {
        int activeHearts = permanentHearts.getOrDefault(player.getUniqueId(), 0);
        List<TimedHearts> entries = timedHearts.get(player.getUniqueId());
        if (entries != null) {
            for (TimedHearts entry : entries) {
                if (entry.expiresAt() > System.currentTimeMillis()) {
                    activeHearts += entry.hearts();
                }
            }
        }

        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        double value = Math.min(1024.0, 20.0 + activeHearts * 2.0);
        maxHealth.setBaseValue(value);
        if (player.getHealth() > value) {
            player.setHealth(value);
        }
    }

    public void updateAllOnlinePlayers() {
        boolean changed = removeExpired();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updateHealth(player);
        }
        if (changed) {
            save();
        }
    }

    private boolean removeExpired() {
        boolean changed = false;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, List<TimedHearts>>> users = timedHearts.entrySet().iterator();
        while (users.hasNext()) {
            List<TimedHearts> entries = users.next().getValue();
            if (entries.removeIf(entry -> entry.expiresAt() <= now)) {
                changed = true;
            }
            if (entries.isEmpty()) {
                users.remove();
            }
        }
        return changed;
    }

    private void load() {
        ConfigurationSection permanent = plugin.getConfig().getConfigurationSection("permanent");
        if (permanent != null) {
            for (String key : permanent.getKeys(false)) {
                try {
                    permanentHearts.put(UUID.fromString(key), permanent.getInt(key));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Ignoring invalid permanent heart UUID: " + key);
                }
            }
        }

        ConfigurationSection timed = plugin.getConfig().getConfigurationSection("timed");
        if (timed != null) {
            for (String key : timed.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    List<TimedHearts> entries = new ArrayList<>();
                    for (Map<?, ?> raw : timed.getMapList(key)) {
                        Object hearts = raw.get("hearts");
                        Object expiresAt = raw.get("expiresAt");
                        if (hearts instanceof Number heartNumber && expiresAt instanceof Number expiryNumber) {
                            entries.add(new TimedHearts(heartNumber.intValue(), expiryNumber.longValue()));
                        }
                    }
                    if (!entries.isEmpty()) {
                        timedHearts.put(uuid, entries);
                    }
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Ignoring invalid timed heart UUID: " + key);
                }
            }
        }
        removeExpired();
    }

    private void save() {
        plugin.getConfig().set("permanent", null);
        plugin.getConfig().set("timed", null);
        for (Map.Entry<UUID, Integer> entry : permanentHearts.entrySet()) {
            plugin.getConfig().set("permanent." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<UUID, List<TimedHearts>> entry : timedHearts.entrySet()) {
            List<Map<String, Object>> values = new ArrayList<>();
            for (TimedHearts timedEntry : entry.getValue()) {
                Map<String, Object> value = new HashMap<>();
                value.put("hearts", timedEntry.hearts());
                value.put("expiresAt", timedEntry.expiresAt());
                values.add(value);
            }
            plugin.getConfig().set("timed." + entry.getKey(), values);
        }
        plugin.saveConfig();
    }

    private record TimedHearts(int hearts, long expiresAt) {
    }
}
