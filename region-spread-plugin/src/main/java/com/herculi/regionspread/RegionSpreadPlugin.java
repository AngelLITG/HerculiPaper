package com.herculi.regionspread;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class RegionSpreadPlugin extends JavaPlugin {

    private int regionShift;
    private int regionSizeBlocks;

    @Override
    public void onEnable() {
        // Read region shift from server (HerculiConfig is server-side); mirror logic from RegionSchedulers.regionKeyFor
        this.regionShift = readRegionShiftFromConfig();
        this.regionSizeBlocks = 1 << this.regionShift; // blocks per region along one axis
        getLogger().info("[RegionSpread] region.shift=" + regionShift + " => region size: " + regionSizeBlocks + "x" + regionSizeBlocks + " blocks (" + (regionSizeBlocks >> 4) + "x" + (regionSizeBlocks >> 4) + " chunks)");
    }

    private int readRegionShiftFromConfig() {
        try {
            java.io.File f = new java.io.File(Bukkit.getWorldContainer(), "../HerculiPaper/config/herculi.yml");
            if (!f.exists()) {
                // Fallback: assume default 4
                return 4;
            }
            java.util.List<String> lines = java.nio.file.Files.readAllLines(f.toPath());
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("threading.region_shift:")) {
                    String[] parts = line.split(":");
                    if (parts.length >= 2) {
                        return Integer.parseInt(parts[1].trim());
                    }
                }
            }
        } catch (Throwable ignored) {}
        return 4; // default from config
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("regionspread.command")) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /regionspread <playersPerRegion> [radiusRegions]");
            sender.sendMessage("Current region size: " + regionSizeBlocks + "x" + regionSizeBlocks + " blocks");
            return true;
        }

        int perRegion = parseInt(args[0], 5);
        int radiusRegions = (args.length >= 2) ? parseInt(args[1], 2) : 2; // square radius in regions

        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) {
            sender.sendMessage("No worlds loaded.");
            return true;
        }

        // Compute region grid centered at world spawn
        Location spawn = world.getSpawnLocation();
        int centerRx = floorDiv(spawn.getBlockX(), regionSizeBlocks);
        int centerRz = floorDiv(spawn.getBlockZ(), regionSizeBlocks);

        // Build target region centers
        List<Location> regionCenters = new ArrayList<>();
        for (int rz = centerRz - radiusRegions; rz <= centerRz + radiusRegions; rz++) {
            for (int rx = centerRx - radiusRegions; rx <= centerRx + radiusRegions; rx++) {
                int minX = rx * regionSizeBlocks;
                int minZ = rz * regionSizeBlocks;
                int cx = minX + regionSizeBlocks / 2;
                int cz = minZ + regionSizeBlocks / 2;
                regionCenters.add(new Location(world, cx + 0.5, spawn.getY(), cz + 0.5));
            }
        }

        // Gather online players and distribute Round-Robin
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) {
            sender.sendMessage("No players online.");
            return true;
        }

        int assigned = 0;
        int regionIdx = 0;
        Map<String, Integer> countsByRegionKey = new LinkedHashMap<>();

        for (Player p : players) {
            // Keep existing distribution limit
            if (assigned >= regionCenters.size() * perRegion) break;

            Location target = regionCenters.get(regionIdx);
            // Async-safe: schedule on main thread for teleport (Bukkit constraint)
            Bukkit.getScheduler().runTask(this, () -> {
                p.teleportAsync(target);
            });

            String key = ( (floorDiv(target.getBlockX(), regionSizeBlocks)) + "," + (floorDiv(target.getBlockZ(), regionSizeBlocks)) );
            countsByRegionKey.put(key, countsByRegionKey.getOrDefault(key, 0) + 1);

            assigned++;
            // Next region slot if filled
            if (countsByRegionKey.get(key) >= perRegion) {
                regionIdx = (regionIdx + 1) % regionCenters.size();
            }
        }

        sender.sendMessage("Assigned " + assigned + " players across " + regionCenters.size() + " regions (max " + perRegion + " per region)");
        sender.sendMessage("Region size: " + regionSizeBlocks + "x" + regionSizeBlocks + " blocks, shift=" + regionShift);
        return true;
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Throwable ignored) { return def; }
    }

    private static int floorDiv(int x, int y) {
        int r = x / y;
        if ((x ^ y) < 0 && (r * y != x)) r--;
        return r;
    }
}
