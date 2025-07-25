package nl.djorr.basketball.managers;

import nl.djorr.basketball.BasketballPlugin;
import nl.djorr.basketball.objects.BasketballRegion;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages data persistence for basketball regions and player wins
 * 
 * @author Djorr
 */
public class DataManager {
    
    private final BasketballPlugin plugin;
    private final File dataFile;
    private final FileConfiguration dataConfig;
    
    public DataManager(BasketballPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "regions.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    /**
     * Save all regions and player wins data
     */
    public void saveData() {
        try {
            // Clear existing data
            dataConfig.set("regions", null);
            
            // Save regions
            ConfigurationSection regionsSection = dataConfig.createSection("regions");
            Map<String, BasketballRegion> regions = plugin.getBasketballManager().getRegions();
            
            for (Map.Entry<String, BasketballRegion> entry : regions.entrySet()) {
                String regionName = entry.getKey();
                BasketballRegion region = entry.getValue();
                
                ConfigurationSection regionSection = regionsSection.createSection(regionName);
                
                // Save region data
                regionSection.set("center", region.getCenter());
                regionSection.set("spawnLocation", region.getSpawnLocation());
                
                if (region.getLeftHoop() != null) {
                    regionSection.set("leftHoop", region.getLeftHoop());
                }
                if (region.getRightHoop() != null) {
                    regionSection.set("rightHoop", region.getRightHoop());
                }
                if (region.getLeftBackboard() != null) {
                    regionSection.set("leftBackboard", region.getLeftBackboard());
                }
                if (region.getRightBackboard() != null) {
                    regionSection.set("rightBackboard", region.getRightBackboard());
                }
                
                // Save player wins
                ConfigurationSection winsSection = regionSection.createSection("playerWins");
                Map<Player, Integer> playerWins = region.getPlayerWins();
                
                for (Map.Entry<Player, Integer> winEntry : playerWins.entrySet()) {
                    Player player = winEntry.getKey();
                    int wins = winEntry.getValue();
                    winsSection.set(player.getUniqueId().toString(), wins);
                }
            }
            
            dataConfig.save(dataFile);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Saved " + regions.size() + " regions with player wins data");
            }
            
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save regions data: " + e.getMessage());
        }
    }
    
    /**
     * Load all regions and player wins data
     */
    public void loadData() {
        if (!dataFile.exists()) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("No regions data file found, starting fresh");
            }
            return;
        }
        
        try {
            ConfigurationSection regionsSection = dataConfig.getConfigurationSection("regions");
            if (regionsSection == null) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("No regions data found in file");
                }
                return;
            }
            
            int loadedRegions = 0;
            
            for (String regionName : regionsSection.getKeys(false)) {
                ConfigurationSection regionSection = regionsSection.getConfigurationSection(regionName);
                if (regionSection == null) continue;
                
                // Load region data
                Location center = (Location) regionSection.get("center");
                Location spawnLocation = (Location) regionSection.get("spawnLocation");
                
                if (center == null || spawnLocation == null) {
                    plugin.getLogger().warning("Invalid region data for " + regionName + ", skipping");
                    continue;
                }
                
                // Create region
                BasketballRegion region = new BasketballRegion(regionName, center, spawnLocation);
                
                // Load optional hoop and backboard data
                Location leftHoop = (Location) regionSection.get("leftHoop");
                Location rightHoop = (Location) regionSection.get("rightHoop");
                Location leftBackboard = (Location) regionSection.get("leftBackboard");
                Location rightBackboard = (Location) regionSection.get("rightBackboard");
                
                if (leftHoop != null) region.setLeftHoop(leftHoop);
                if (rightHoop != null) region.setRightHoop(rightHoop);
                if (leftBackboard != null) region.setLeftBackboard(leftBackboard);
                if (rightBackboard != null) region.setRightBackboard(rightBackboard);
                
                // Load player wins
                ConfigurationSection winsSection = regionSection.getConfigurationSection("playerWins");
                if (winsSection != null) {
                    for (String playerUUID : winsSection.getKeys(false)) {
                        int wins = winsSection.getInt(playerUUID, 0);
                        if (wins > 0) {
                            // We'll need to load this when players join
                            region.setPlayerWinsFromUUID(UUID.fromString(playerUUID), wins);
                        }
                    }
                }
                
                // Register region
                plugin.getBasketballManager().registerRegion(regionName, region);
                loadedRegions++;
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Loaded region: " + regionName + " with " + 
                        (winsSection != null ? winsSection.getKeys(false).size() : 0) + " player wins");
                }
            }
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Loaded " + loadedRegions + " regions from data file");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load regions data: " + e.getMessage());
        }
    }
    
    /**
     * Update player wins when a player joins (UUID to Player mapping)
     */
    public void updatePlayerWins(Player player) {
        for (BasketballRegion region : plugin.getBasketballManager().getRegions().values()) {
            region.updatePlayerWinsFromUUID(player);
        }
    }
} 