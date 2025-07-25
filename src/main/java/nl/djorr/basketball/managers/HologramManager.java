package nl.djorr.basketball.managers;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import nl.djorr.basketball.BasketballPlugin;
import nl.djorr.basketball.objects.BasketballRegion;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages hologram displays for basketball leaderboards
 * 
 * @author Djorr
 */
public class HologramManager {
    
    private final BasketballPlugin plugin;
    private final Map<Player, Hologram> playerHolograms;
    private final Map<String, Hologram> regionHolograms;
    
    public HologramManager(BasketballPlugin plugin) {
        this.plugin = plugin;
        this.playerHolograms = new HashMap<>();
        this.regionHolograms = new HashMap<>();
    }
    
    /**
     * Show leaderboard hologram to a player
     * 
     * @param player The player to show the hologram to
     * @param regionName The region name
     */
    public void showLeaderboardHologram(Player player, String regionName) {
        // Remove existing hologram for this player
        removePlayerHologram(player);
        
        BasketballRegion region = plugin.getBasketballManager().getRegion(regionName);
        if (region == null) {
            return;
        }
        
        // Get player location for hologram placement - place higher to avoid ground clipping
        Location hologramLocation = player.getLocation().add(0, 5, 0); // 5 blocks above player
        
        // Create hologram lines
        List<String> lines = createLeaderboardLines(region);
        
        // Adjust height based on number of lines to ensure visibility
        int lineCount = lines.size();
        if (lineCount > 15) {
            // If many lines, place even higher
            hologramLocation = player.getLocation().add(0, 7, 0);
        } else if (lineCount > 10) {
            // If moderate lines, place higher
            hologramLocation = player.getLocation().add(0, 6, 0);
        }
        
        // Ensure hologram doesn't clip into blocks
        hologramLocation = findSafeHologramLocation(hologramLocation, lineCount);
        
        // Create hologram
        String hologramId = "basketball_leaderboard_" + player.getUniqueId();
        Hologram hologram = DHAPI.createHologram(hologramId, hologramLocation, lines);
        
        // Show to player
        hologram.show(player, 0);
        
        // Store reference
        playerHolograms.put(player, hologram);
        
        // Auto-remove after 10 seconds
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            removePlayerHologram(player);
        }, 200L); // 10 seconds (20 ticks per second)
    }
    
    /**
     * Create leaderboard lines for a region
     * 
     * @param region The basketball region
     * @return List of formatted lines
     */
    private List<String> createLeaderboardLines(BasketballRegion region) {
        List<String> lines = new ArrayList<>();
        
        // Header
        lines.add(ChatColor.GOLD + "╔══════════════════════════════════════╗");
        lines.add(ChatColor.GOLD + "║" + ChatColor.YELLOW + "        [Basketbal Leaderboard]        " + ChatColor.GOLD + "║");
        lines.add(ChatColor.GOLD + "║" + ChatColor.GREEN + "    Behaal de 10 punten en win!    " + ChatColor.GOLD + "║");
        lines.add(ChatColor.GOLD + "╠══════════════════════════════════════╣");
        
        Set<Player> playersInRegion = region.getPlayersInRegion();
        
        if (playersInRegion.isEmpty()) {
            // No players in region - show top 10 winners
            lines.add(ChatColor.GOLD + "║" + ChatColor.GRAY + "                                    " + ChatColor.GOLD + "║");
            lines.add(ChatColor.GOLD + "║" + ChatColor.YELLOW + "           Top Winners            " + ChatColor.GOLD + "║");
            lines.add(ChatColor.GOLD + "╠══════════════════════════════════════╣");
            
            Map<Player, Integer> playerWins = region.getPlayerWins();
            List<Map.Entry<Player, Integer>> sortedWins = playerWins.entrySet().stream()
                .sorted(Map.Entry.<Player, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());
            
            if (sortedWins.isEmpty()) {
                lines.add(ChatColor.GOLD + "║" + ChatColor.GRAY + "         Nog geen wins!          " + ChatColor.GOLD + "║");
            } else {
                for (int i = 0; i < sortedWins.size(); i++) {
                    Map.Entry<Player, Integer> entry = sortedWins.get(i);
                    Player player = entry.getKey();
                    int wins = entry.getValue();
                    
                    ChatColor color;
                    if (i == 0) color = ChatColor.GOLD;      // 1st place
                    else if (i == 1) color = ChatColor.GRAY;  // 2nd place
                    else if (i == 2) color = ChatColor.RED;   // 3rd place
                    else color = ChatColor.WHITE;             // 4th-10th place
                    
                    String line = ChatColor.GOLD + "║" + color + " " + player.getName() + ": " + wins + " wins";
                    // Pad to 36 characters
                    while (line.length() < 36) {
                        line += " ";
                    }
                    line += ChatColor.GOLD + "║";
                    lines.add(line);
                }
            }
        } else {
            // Players in region - show live scores
            lines.add(ChatColor.GOLD + "║" + ChatColor.YELLOW + "           Live Scores            " + ChatColor.GOLD + "║");
            lines.add(ChatColor.GOLD + "╠══════════════════════════════════════╣");
            
            Map<Player, Integer> playerScores = region.getPlayerScores();
            List<Map.Entry<Player, Integer>> sortedScores = playerScores.entrySet().stream()
                .sorted(Map.Entry.<Player, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
            
            for (int i = 0; i < sortedScores.size(); i++) {
                Map.Entry<Player, Integer> entry = sortedScores.get(i);
                Player player = entry.getKey();
                int score = entry.getValue();
                
                ChatColor color;
                if (i == 0) color = ChatColor.GOLD;      // 1st place
                else if (i == 1) color = ChatColor.GRAY;  // 2nd place
                else if (i == 2) color = ChatColor.RED;   // 3rd place
                else color = ChatColor.WHITE;             // 4th+ place
                
                String line = ChatColor.GOLD + "║" + color + " " + player.getName() + ": " + score;
                // Pad to 36 characters
                while (line.length() < 36) {
                    line += " ";
                }
                line += ChatColor.GOLD + "║";
                lines.add(line);
            }
        }
        
        // Footer
        lines.add(ChatColor.GOLD + "╚══════════════════════════════════════╝");
        
        return lines;
    }
    
    /**
     * Remove hologram for a specific player
     * 
     * @param player The player
     */
    public void removePlayerHologram(Player player) {
        Hologram hologram = playerHolograms.remove(player);
        if (hologram != null) {
            hologram.delete();
        }
    }
    
    /**
     * Remove all player holograms
     */
    public void removeAllPlayerHolograms() {
        for (Hologram hologram : playerHolograms.values()) {
            hologram.delete();
        }
        playerHolograms.clear();
    }
    
    /**
     * Create a permanent region hologram
     * 
     * @param regionName The region name
     * @param location The location for the hologram
     */
    public void createRegionHologram(String regionName, Location location) {
        // Remove existing hologram for this region
        removeRegionHologram(regionName);
        
        BasketballRegion region = plugin.getBasketballManager().getRegion(regionName);
        if (region == null) {
            return;
        }
        
        // Create hologram lines
        List<String> lines = createLeaderboardLines(region);
        
        // Ensure safe placement
        Location safeLocation = findSafeHologramLocation(location, lines.size());
        
        // Create permanent hologram
        String hologramId = "basketball_region_" + regionName;
        Hologram hologram = DHAPI.createHologram(hologramId, safeLocation, lines);
        
        // Store reference
        regionHolograms.put(regionName, hologram);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Created permanent leaderboard hologram for region: " + regionName);
        }
    }
    
    /**
     * Remove hologram for a specific region
     * 
     * @param regionName The region name
     */
    public void removeRegionHologram(String regionName) {
        Hologram hologram = regionHolograms.remove(regionName);
        if (hologram != null) {
            hologram.delete();
        }
    }
    
    /**
     * Update all region holograms with live data
     */
    public void updateAllRegionHolograms() {
        for (Map.Entry<String, Hologram> entry : regionHolograms.entrySet()) {
            String regionName = entry.getKey();
            Hologram hologram = entry.getValue();
            
            BasketballRegion region = plugin.getBasketballManager().getRegion(regionName);
            if (region != null) {
                // Recreate hologram with updated lines
                List<String> lines = createLeaderboardLines(region);
                hologram.delete();
                String hologramId = "basketball_region_" + regionName;
                Hologram newHologram = DHAPI.createHologram(hologramId, hologram.getLocation(), lines);
                regionHolograms.put(regionName, newHologram);
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Updated leaderboard hologram for region: " + regionName);
                }
            }
        }
    }
    
    /**
     * Remove all region holograms
     */
    public void removeAllRegionHolograms() {
        for (Hologram hologram : regionHolograms.values()) {
            hologram.delete();
        }
        regionHolograms.clear();
    }
    
    /**
     * Check if a region has a hologram
     * 
     * @param regionName The region name
     * @return True if the region has a hologram
     */
    public boolean hasRegionHologram(String regionName) {
        return regionHolograms.containsKey(regionName);
    }
    
    /**
     * Find a safe location for hologram placement
     * 
     * @param baseLocation The base location
     * @param lineCount The number of lines in the hologram
     * @return Safe location for hologram
     */
    private Location findSafeHologramLocation(Location baseLocation, int lineCount) {
        Location safeLocation = baseLocation.clone();
        
        // Check if the base location is safe
        if (isLocationSafe(safeLocation, lineCount)) {
            return safeLocation;
        }
        
        // Try higher locations until we find a safe one
        for (int y = 1; y <= 10; y++) {
            Location testLocation = baseLocation.clone().add(0, y, 0);
            if (isLocationSafe(testLocation, lineCount)) {
                return testLocation;
            }
        }
        
        // If no safe location found, return the highest possible
        return baseLocation.clone().add(0, 10, 0);
    }
    
    /**
     * Check if a location is safe for hologram placement
     * 
     * @param location The location to check
     * @param lineCount The number of lines in the hologram
     * @return True if location is safe
     */
    private boolean isLocationSafe(Location location, int lineCount) {
        // Check if there are solid blocks in the hologram area
        for (int y = 0; y < lineCount; y++) {
            Location checkLocation = location.clone().add(0, y, 0);
            if (checkLocation.getBlock().getType().isSolid()) {
                return false;
            }
        }
        return true;
    }
} 