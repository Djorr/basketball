package nl.djorr.basketball.listeners;

import nl.djorr.basketball.BasketballPlugin;
import nl.djorr.basketball.objects.BasketballRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldedit.Vector;

/**
 * Listener for WorldGuard region events
 * 
 * @author Djorr
 */
public class RegionListener implements Listener {
    
    private final BasketballPlugin plugin;
    private final Map<Player, String> playerRegions;
    
    /**
     * Constructor for RegionListener
     * 
     * @param plugin The plugin instance
     */
    public RegionListener(BasketballPlugin plugin) {
        this.plugin = plugin;
        this.playerRegions = new HashMap<>();
    }
    
    /**
     * Handle player movement to detect region entry/exit
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // Only check if player actually moved to a different block
        if (from.getBlockX() == to.getBlockX() && 
            from.getBlockY() == to.getBlockY() && 
            from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        
        // Check for region entry/exit
        checkRegionChange(player, to);
    }
    
    /**
     * Handle player quit
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Remove player from any basketball regions
        String currentRegion = playerRegions.get(player);
        if (currentRegion != null) {
            BasketballRegion basketballRegion = plugin.getBasketballManager().getRegion(currentRegion);
            if (basketballRegion != null) {
                basketballRegion.removePlayer(player);
            }
            playerRegions.remove(player);
        }
    }
    
    /**
     * Handle player join
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Update player wins when they join
        plugin.getBasketballManager().updatePlayerWins(player);
    }
    
    /**
     * Check if a player has entered or exited a basketball region
     * 
     * @param player The player
     * @param location The player's location
     */
    private void checkRegionChange(Player player, Location location) {
        String currentRegion = playerRegions.get(player);
        String newRegion = getCurrentBasketballRegion(player, location);
        
        // Debug logging
        if (plugin.getConfigManager().shouldLogRegionChecks()) {
            plugin.getLogger().info("Player " + player.getName() + " at " + location.getBlockX() + "," + 
                location.getBlockY() + "," + location.getBlockZ() + " - Current: " + currentRegion + ", New: " + newRegion);
        }
        
        // Player entered a new basketball region
        if (newRegion != null && !newRegion.equals(currentRegion)) {
            // Leave old region if any
            if (currentRegion != null) {
                BasketballRegion oldBasketballRegion = plugin.getBasketballManager().getRegion(currentRegion);
                if (oldBasketballRegion != null) {
                    oldBasketballRegion.removePlayer(player);
                }
            }
            
            // Check if basketball region exists, if not create it
            BasketballRegion newBasketballRegion = plugin.getBasketballManager().getRegion(newRegion);
            if (newBasketballRegion == null) {
                // Create new basketball region automatically
                newBasketballRegion = createBasketballRegionFromWorldGuard(newRegion, location);
                if (newBasketballRegion != null) {
                    plugin.getBasketballManager().registerRegion(newRegion, newBasketballRegion);
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Created new basketball region: " + newRegion);
                    }
                }
            }
            
            // Enter new region
            if (newBasketballRegion != null) {
                newBasketballRegion.addPlayer(player);
                playerRegions.put(player, newRegion);
            }
        }
        // Player left a basketball region
        else if (currentRegion != null && newRegion == null) {
            BasketballRegion basketballRegion = plugin.getBasketballManager().getRegion(currentRegion);
            if (basketballRegion != null) {
                basketballRegion.removePlayer(player);
            }
            playerRegions.remove(player);
        }
    }
    
    // Removed old findBasketballRegionDirect method as it's no longer needed
    
    /**
     * Simple fallback method that checks all basketball regions
     * This is used when WorldGuard integration fails
     */
    private String findBasketballRegionFallback(Location location) {
        // Check all basketball regions to see if player is in any of them
        for (String regionName : plugin.getBasketballManager().getRegions().keySet()) {
            BasketballRegion region = plugin.getBasketballManager().getRegion(regionName);
            if (region != null) {
                // Get region bounds and check if player is within them
                int[] bounds = region.getRegionBounds();
                if (bounds != null) {
                    int x = location.getBlockX();
                    int y = location.getBlockY();
                    int z = location.getBlockZ();
                    
                    // Check if player is within region bounds
                    if (x >= bounds[0] && x <= bounds[1] &&
                        y >= bounds[2] && y <= bounds[3] &&
                        z >= bounds[4] && z <= bounds[5]) {
                        
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("Player in basketball region via fallback: " + regionName);
                        }
                        return regionName;
                    }
                } else {
                    // Fallback to distance check if bounds are not available
                    Location center = region.getCenter();
                    if (location.distance(center) <= 20) {
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("Player in basketball region via distance fallback: " + regionName);
                        }
                        return regionName;
                    }
                }
            }
        }
        return null;
    }
    
    // Removed old isWorldGuardAvailable method as it's no longer needed
    
    /**
     * Get the current basketball region for a player
     * Only check regions that are registered in the basketball manager
     * 
     * @param player The player
     * @param location The player's location
     * @return The region name or null
     */
    private String getCurrentBasketballRegion(Player player, Location location) {
        // Check all registered basketball regions
        for (String regionName : plugin.getBasketballManager().getRegions().keySet()) {
            BasketballRegion region = plugin.getBasketballManager().getRegion(regionName);
            if (region != null) {
                // Get region bounds and check if player is within them
                int[] bounds = region.getRegionBounds();
                if (bounds != null) {
                    int x = location.getBlockX();
                    int y = location.getBlockY();
                    int z = location.getBlockZ();
                    
                    // Check if player is within region bounds
                    if (x >= bounds[0] && x <= bounds[1] &&
                        y >= bounds[2] && y <= bounds[3] &&
                        z >= bounds[4] && z <= bounds[5]) {
                        
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("Player " + player.getName() + " is in basketball region: " + regionName);
                        }
                        return regionName;
                    }
                } else {
                    // Fallback to distance check if bounds are not available
                    Location center = region.getCenter();
                    if (location.distance(center) <= 20) {
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("Player " + player.getName() + " is in basketball region via distance: " + regionName);
                        }
                        return regionName;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Find a basketball region at a location (legacy method)
     * 
     * @param location The location to check
     * @return The region name or null
     */
    private String findBasketballRegion(Location location) {
        return getCurrentBasketballRegion(null, location);
    }
    
    /**
     * Create a BasketballRegion from a WorldGuard region
     * 
     * @param regionName The WorldGuard region name
     * @param playerLocation The player's location (used to find center)
     * @return The created BasketballRegion or null
     */
    private BasketballRegion createBasketballRegionFromWorldGuard(String regionName, Location playerLocation) {
        try {
            WorldGuardPlugin worldGuard = WorldGuardPlugin.inst();
            RegionManager regionManager = worldGuard.getRegionManager(playerLocation.getWorld());
            ProtectedRegion worldGuardRegion = regionManager.getRegion(regionName);
            
            if (worldGuardRegion == null) {
                plugin.getLogger().warning("WorldGuard region not found: " + regionName);
                return null;
            }
            
            // Get region bounds
            Vector min = worldGuardRegion.getMinimumPoint();
            Vector max = worldGuardRegion.getMaximumPoint();
            
            // Calculate center (bedrock block location)
            Location center = new Location(
                playerLocation.getWorld(),
                (min.getX() + max.getX()) / 2.0,
                min.getY(), // Use minimum Y as ground level
                (min.getZ() + max.getZ()) / 2.0
            );
            
            // Find the bedrock block at the center
            Location bedrockLocation = findBedrockBlock(center);
            if (bedrockLocation == null) {
                // Use center if no bedrock found
                bedrockLocation = center;
            }
            
            // Create basketball region
            BasketballRegion basketballRegion = new BasketballRegion(regionName, bedrockLocation, bedrockLocation.clone().add(0, 1, 0));
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Created basketball region: " + regionName + " at " + bedrockLocation.getBlockX() + "," + 
                    bedrockLocation.getBlockY() + "," + bedrockLocation.getBlockZ());
            }
            
            return basketballRegion;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error creating basketball region from WorldGuard: " + e.getMessage());
            if (plugin.getConfigManager().isDebugEnabled()) {
                e.printStackTrace();
            }
            return null;
        }
    }
    
    /**
     * Find a bedrock block near the given location
     * 
     * @param location The location to search around
     * @return The bedrock location or null
     */
    private Location findBedrockBlock(Location location) {
        // Search in a 5x5 area around the location
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Location checkLocation = location.clone().add(x, 0, z);
                if (checkLocation.getBlock().getType() == org.bukkit.Material.BEDROCK) {
                    return checkLocation;
                }
            }
        }
        return null;
    }
    
    // Removed old error tracking variables as they're no longer needed
    
    /**
     * Get the current region for a player
     * 
     * @param player The player
     * @return The region name or null
     */
    public String getPlayerRegion(Player player) {
        return playerRegions.get(player);
    }
    
    /**
     * Get all player regions
     * 
     * @return Map of players to their regions
     */
    public Map<Player, String> getPlayerRegions() {
        return new HashMap<>(playerRegions);
    }
} 