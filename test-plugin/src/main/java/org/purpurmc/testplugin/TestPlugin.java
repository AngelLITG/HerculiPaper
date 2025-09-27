package org.purpurmc.testplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import io.papermc.paper.threadedregions.RegionizedServerInitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.ThreadLocalRandom;

public class TestPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        boolean folia = false;
        try {
            java.lang.reflect.Method m = this.getDescription().getClass().getMethod("isFoliaSupported");
            Object v = m.invoke(this.getDescription());
            if (v instanceof Boolean) folia = (Boolean) v;
        } catch (Throwable ignored) {}
        boolean global = false;
        try {
            global = Bukkit.isGlobalTickThread();
        } catch (Throwable ignored) {}
        getLogger().info("[Herculi-Test] Folia support declared: " + folia + "; GlobalTickThread now: " + global);
        getLogger().info("[Herculi-Test] Type /herculi_test to run async handoff tests (command intercepted by plugin).");
    }

    private void handleHealthScaleCommand(@NotNull Player player, @NotNull String raw) {
        String[] parts = raw.split("\\s+");
        if (parts.length == 1) {
            player.sendMessage("[Herculi-Test] Usage: /hs on [scale] | /hs off | /hs set <scale>");
            return;
        }
        String sub = parts[1].toLowerCase(java.util.Locale.ROOT);
        try {
            switch (sub) {
                case "on": {
                    double scale = 20.0;
                    if (parts.length >= 3) scale = Double.parseDouble(parts[2]);
                    player.setHealthScale(scale);
                    player.setHealthScaled(true);
                    player.sendMessage("[Herculi-Test] Health scaling ON. scale=" + scale);
                    break;
                }
                case "off": {
                    player.setHealthScaled(false);
                    player.sendMessage("[Herculi-Test] Health scaling OFF.");
                    break;
                }
                case "set": {
                    if (parts.length < 3) {
                        player.sendMessage("[Herculi-Test] Usage: /hs set <scale>");
                        return;
                    }
                    double scale = Double.parseDouble(parts[2]);
                    player.setHealthScale(scale);
                    if (!player.isHealthScaled()) player.setHealthScaled(true);
                    player.sendMessage("[Herculi-Test] Health scale set to " + scale + ".");
                    break;
                }
                default: {
                    player.sendMessage("[Herculi-Test] Usage: /hs on [scale] | /hs off | /hs set <scale>");
                }
            }
        } catch (IllegalArgumentException ex) {
            player.sendMessage("[Herculi-Test] Error: " + ex.getMessage());
        }
    }

    @EventHandler
    public void onRegionizedInit(RegionizedServerInitEvent event) {
        // Should fire once before regions tick in parallel
        getLogger().info("[Herculi-Test] RegionizedServerInitEvent fired; regions will begin ticking after this.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage("[Herculi-Test] Type /herculi_test to run async handoff tests.");
    }

    @EventHandler
    public void onPreprocess(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        if (msg.equalsIgnoreCase("/herculi_test") || msg.toLowerCase(java.util.Locale.ROOT).startsWith("/herculi_test ")) {
            event.setCancelled(true);
            runHandoffTests(event.getPlayer());
        }

        // /hs [on|off|set <value>] - player health scaling quick test
        if (msg.equalsIgnoreCase("/hs") || msg.toLowerCase(java.util.Locale.ROOT).startsWith("/hs ")) {
            event.setCancelled(true);
            handleHealthScaleCommand(event.getPlayer(), msg);
        }
    }

    private void runHandoffTests(@NotNull CommandSender sender) {
        // Gather context on main thread to avoid AsyncCatcher (e.g., getNearbyEntities)
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) {
            sender.sendMessage("[Herculi-Test] No worlds loaded.");
            return;
        }
        Location loc = world.getSpawnLocation();
        Entity target = world.getNearbyEntities(loc, 16, 16, 16).stream().findFirst().orElse(null);
        // Choose a player recipient (prefer the sender if a player)
        Player recipient = null;
        if (sender instanceof Player) {
            recipient = (Player) sender;
        } else {
            recipient = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        }

        // Show thread/region context to the tester
        try {
            boolean global = Bukkit.isGlobalTickThread();
            sender.sendMessage("[Herculi-Test] GlobalTickThread=" + global);
        } catch (Throwable ignored) {}
        if (sender instanceof Player) {
            try {
                Player p = (Player) sender;
                boolean owned = Bukkit.getServer().isOwnedByCurrentRegion(p.getLocation());
                sender.sendMessage("[Herculi-Test] OwnedByCurrentRegion@you=" + owned + " chunk=" + (p.getLocation().getBlockX() >> 4) + "," + (p.getLocation().getBlockZ() >> 4));
            } catch (Throwable ignored) {}
        }

        sender.sendMessage("[Herculi-Test] Scheduling async particle/sound tests...");

        final Player finalRecipient = recipient; // capture for lambda
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {

            // Particle test (off-main) via Player API to exercise handoff
            try {
                if (finalRecipient != null) {
                    finalRecipient.spawnParticle(Particle.CRIT, finalRecipient.getLocation().add(0, 1.0, 0), 10, 0.3, 0.3, 0.3, 0.01);
                    sender.sendMessage("[Herculi-Test] Async particle spawn requested via Player API.");
                } else {
                    sender.sendMessage("[Herculi-Test] No player online to receive particles.");
                }
            } catch (Throwable t) {
                sender.sendMessage("[Herculi-Test] Particle test threw: " + t.getClass().getSimpleName());
            }

            // Location sound test (off-main) via Player API
            try {
                if (finalRecipient != null) {
                    finalRecipient.playSound(finalRecipient.getLocation(), Sound.BLOCK_ANVIL_LAND, SoundCategory.MASTER, 1.0f, 1.0f);
                    sender.sendMessage("[Herculi-Test] Async location sound requested via Player API.");
                } else {
                    sender.sendMessage("[Herculi-Test] No player online to receive location sound.");
                }
            } catch (Throwable t) {
                sender.sendMessage("[Herculi-Test] Location sound test threw: " + t.getClass().getSimpleName());
            }

            // Entity sound test (off-main) targeted to player location if entity existed
            try {
                if (finalRecipient != null) {
                    if (target != null) {
                        long seed = ThreadLocalRandom.current().nextLong();
                        // Use player API; seed ignored in this variant but keeps semantics close
                        finalRecipient.playSound(target.getLocation(), Sound.ENTITY_COW_AMBIENT, SoundCategory.NEUTRAL, 1.0f, 1.0f);
                        sender.sendMessage("[Herculi-Test] Async entity sound requested via Player API (entity=" + target.getUniqueId() + ").");
                    } else {
                        sender.sendMessage("[Herculi-Test] No nearby entity found for entity-sound test.");
                    }
                } else {
                    sender.sendMessage("[Herculi-Test] No player online to receive entity sound.");
                }
            } catch (Throwable t) {
                sender.sendMessage("[Herculi-Test] Entity sound test threw: " + t.getClass().getSimpleName());
            }
        });
    }
}
