package nl.djorr.basketball;

import nl.djorr.basketball.commands.BasketballCommand;
import nl.djorr.basketball.listeners.BasketballListener;
import nl.djorr.basketball.listeners.RegionListener;
import nl.djorr.basketball.managers.BasketballManager;
import nl.djorr.basketball.managers.ConfigManager;
import nl.djorr.basketball.managers.DataManager;
import nl.djorr.basketball.managers.HologramManager;
import nl.djorr.basketball.managers.ScoreManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for the Basketball plugin
 * 
 * @author Djorr
 */
public class BasketballPlugin extends JavaPlugin {
    
    private static BasketballPlugin instance;
    private ConfigManager configManager;
    private BasketballManager basketballManager;
    private ScoreManager scoreManager;
    private DataManager dataManager;
    private HologramManager hologramManager;
    private RegionListener regionListener;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.basketballManager = new BasketballManager(this);
        this.scoreManager = new ScoreManager(this);
        this.dataManager = new DataManager(this);
        this.hologramManager = new HologramManager(this);
        
        // Load regions and player wins data
        this.dataManager.loadData();
        
        // Clean up any existing basketball entities and blocks on startup
        this.basketballManager.cleanupBasketballsOnStartup();
        
        // Register listeners
        BasketballListener basketballListener = new BasketballListener(this);
        this.regionListener = new RegionListener(this);
        
        getServer().getPluginManager().registerEvents(basketballListener, this);
        getServer().getPluginManager().registerEvents(this.regionListener, this);
        
        // Start physics task
        basketballListener.startPhysicsTask();
        
        // Register commands
        getCommand("basketball").setExecutor(new BasketballCommand(this));
        
        getLogger().info("Basketball plugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save regions and player wins data
        if (dataManager != null) {
            dataManager.saveData();
        }
        
        // Remove all holograms
        if (hologramManager != null) {
            hologramManager.removeAllPlayerHolograms();
            hologramManager.removeAllRegionHolograms();
        }
        
        // Clean up basketball entities
        if (basketballManager != null) {
            basketballManager.removeAllBasketballs();
        }
        
        getLogger().info("Basketball plugin has been disabled!");
    }
    
    /**
     * Get the plugin instance
     * 
     * @return The plugin instance
     */
    public static BasketballPlugin getInstance() {
        return instance;
    }
    
    /**
     * Get the config manager
     * 
     * @return The config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Get the basketball manager
     * 
     * @return The basketball manager
     */
    public BasketballManager getBasketballManager() {
        return basketballManager;
    }
    
    /**
     * Get the score manager
     * 
     * @return The score manager
     */
    public ScoreManager getScoreManager() {
        return scoreManager;
    }
    
    /**
     * Get the data manager
     * 
     * @return The data manager
     */
    public DataManager getDataManager() {
        return dataManager;
    }
    
    /**
     * Get the hologram manager
     * 
     * @return The hologram manager
     */
    public HologramManager getHologramManager() {
        return hologramManager;
    }

    /**
     * Get the region listener
     * 
     * @return The region listener
     */
    public RegionListener getRegionListener() {
        return regionListener;
    }
} 