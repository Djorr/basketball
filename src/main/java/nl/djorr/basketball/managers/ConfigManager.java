package nl.djorr.basketball.managers;

import nl.djorr.basketball.BasketballPlugin;
import nl.djorr.basketball.utils.ItemUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages configuration settings for the basketball plugin
 * 
 * @author Djorr
 */
public class ConfigManager {
    
    private final BasketballPlugin plugin;
    private FileConfiguration config;
    
    // Basketball item settings
    private Material basketballMaterial;
    private byte basketballData;
    private String basketballName;
    private String basketballLore;
    
    // Physics settings
    private int maxBounces;
    private double bounceHeight;
    private double bounceDecay;
    private double throwVelocity;
    private double throwArc;
    private double pickupRange;
    
    // Scoring settings
    private int pointsPerBasket;
    private int titleDuration;
    private int titleFadeIn;
    private int titleFadeOut;
    
    // Hoop settings
    private double hoopDetectionRadius;
    private double scoreHeight;
    private Material backboardMaterial;
    private byte backboardData;
    
    // Messages
    private Map<String, String> messages;
    
    // Debug settings
    private boolean debugEnabled;
    private boolean logRegionChecks;
    
    /**
     * Constructor for ConfigManager
     * 
     * @param plugin The plugin instance
     */
    public ConfigManager(BasketballPlugin plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        loadConfig();
    }
    
    /**
     * Load configuration from file
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
        
        loadDebugSettings();
        loadBasketballSettings();
        loadPhysicsSettings();
        loadScoringSettings();
        loadHoopSettings();
        loadMessages();
    }
    
    /**
     * Load debug settings
     */
    private void loadDebugSettings() {
        ConfigurationSection debugSection = config.getConfigurationSection("debug");
        if (debugSection != null) {
            this.debugEnabled = debugSection.getBoolean("enabled", false);
            this.logRegionChecks = debugSection.getBoolean("log_region_checks", false);
        }
        

    }
    
    /**
     * Load basketball item settings
     */
    private void loadBasketballSettings() {
        ConfigurationSection basketballSection = config.getConfigurationSection("basketball.item");
        if (basketballSection != null) {
            this.basketballMaterial = Material.valueOf(basketballSection.getString("material", "SKULL_ITEM"));
            this.basketballData = (byte) basketballSection.getInt("data", 3);
            this.basketballName = basketballSection.getString("name", "&6Basketball");
            this.basketballLore = basketballSection.getString("lore.0", "&7Throw this basketball into the hoop!");
        }
    }
    
    /**
     * Load physics settings
     */
    private void loadPhysicsSettings() {
        ConfigurationSection physicsSection = config.getConfigurationSection("basketball.physics");
        if (physicsSection != null) {
            this.maxBounces = physicsSection.getInt("max_bounces", 2);
            this.bounceHeight = physicsSection.getDouble("bounce_height", 0.8);
            this.bounceDecay = physicsSection.getDouble("bounce_decay", 0.7);
            this.throwVelocity = physicsSection.getDouble("throw_velocity", 1.2);
            this.throwArc = physicsSection.getDouble("throw_arc", 0.3);
            this.pickupRange = physicsSection.getDouble("pickup_range", 2.0);
        }
    }
    
    /**
     * Load scoring settings
     */
    private void loadScoringSettings() {
        ConfigurationSection scoringSection = config.getConfigurationSection("basketball.scoring");
        if (scoringSection != null) {
            this.pointsPerBasket = scoringSection.getInt("points_per_basket", 2);
            this.titleDuration = scoringSection.getInt("title_duration", 60);
            this.titleFadeIn = scoringSection.getInt("title_fade_in", 10);
            this.titleFadeOut = scoringSection.getInt("title_fade_out", 20);
        }
    }
    
    /**
     * Load hoop settings
     */
    private void loadHoopSettings() {
        ConfigurationSection hoopSection = config.getConfigurationSection("basketball.hoop");
        if (hoopSection != null) {
            this.hoopDetectionRadius = hoopSection.getDouble("detection_radius", 1.5);
            this.scoreHeight = hoopSection.getDouble("score_height", 2.0);
            this.backboardMaterial = Material.valueOf(hoopSection.getString("backboard_material", "STAINED_GLASS"));
            this.backboardData = (byte) hoopSection.getInt("backboard_data", 11);
        }
    }
    
    /**
     * Load messages
     */
    private void loadMessages() {
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                messages.put(key, messagesSection.getString(key));
            }
        }
    }
    
    // Getters for basketball settings
    public Material getBasketballMaterial() {
        return basketballMaterial;
    }
    
    public byte getBasketballData() {
        return basketballData;
    }
    
    public String getBasketballName() {
        return basketballName;
    }
    
    public List<String> getBasketballLore() {
        List<String> lore = new ArrayList<>();
        if (config.contains("basketball.item.lore")) {
            for (Object line : config.getList("basketball.item.lore")) {
                lore.add(line.toString());
            }
        } else if (basketballLore != null) {
            lore.add(basketballLore);
        }
        return lore;
    }
    
    // Getters for physics settings
    public int getMaxBounces() {
        return maxBounces;
    }
    
    public double getBounceHeight() {
        return bounceHeight;
    }
    
    public double getBounceDecay() {
        return bounceDecay;
    }
    
    public double getThrowVelocity() {
        return throwVelocity;
    }
    
    public double getThrowArc() {
        return throwArc;
    }
    
    public double getPickupRange() {
        return pickupRange;
    }
    
    // Getters for scoring settings
    public int getPointsPerBasket() {
        return pointsPerBasket;
    }
    
    public int getTitleDuration() {
        return titleDuration;
    }
    
    public int getTitleFadeIn() {
        return titleFadeIn;
    }
    
    public int getTitleFadeOut() {
        return titleFadeOut;
    }
    
    // Getters for hoop settings
    public double getHoopDetectionRadius() {
        return hoopDetectionRadius;
    }
    
    public double getScoreHeight() {
        return scoreHeight;
    }
    
    public Material getBackboardMaterial() {
        return backboardMaterial;
    }
    
    public byte getBackboardData() {
        return backboardData;
    }
    
    // Getters for debug settings
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    public boolean shouldLogRegionChecks() {
        return logRegionChecks;
    }
    

    
    /**
     * Get a message from the configuration
     * 
     * @param key The message key
     * @return The message
     */
    public String getMessage(String key) {
        String message = messages.getOrDefault(key, "Message not found: " + key);
        return ItemUtil.translateColors(message);
    }
    
    /**
     * Get a message with prefix
     * 
     * @param key The message key
     * @return The message with prefix
     */
    public String getMessageWithPrefix(String key) {
        String prefix = messages.getOrDefault("prefix", "&8[&6Basketball&8] ");
        String message = getMessage(key);
        return ItemUtil.translateColors(prefix) + message;
    }
    
    /**
     * Reload the configuration
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }
} 