package nl.djorr.basketball.objects;

import nl.djorr.basketball.BasketballPlugin;
import org.bukkit.entity.ArmorStand;

/**
 * Represents a basketball entity
 * 
 * @author Djorr
 */
public class Basketball {
    
    private final ArmorStand entity;
    private final BasketballPlugin plugin;
    private boolean thrown;
    private int bounces;
    private long lastBounceTime;
    
    /**
     * Constructor for Basketball
     * 
     * @param entity The armor stand entity
     * @param plugin The plugin instance
     */
    public Basketball(ArmorStand entity, BasketballPlugin plugin) {
        this.entity = entity;
        this.plugin = plugin;
        this.thrown = false;
        this.bounces = 0;
        this.lastBounceTime = System.currentTimeMillis();
    }
    
    /**
     * Get the armor stand entity
     * 
     * @return The armor stand entity
     */
    public ArmorStand getEntity() {
        return entity;
    }
    
    /**
     * Check if the basketball is thrown
     * 
     * @return True if thrown
     */
    public boolean isThrown() {
        return thrown;
    }
    
    /**
     * Set if the basketball is thrown
     * 
     * @param thrown Whether the basketball is thrown
     */
    public void setThrown(boolean thrown) {
        this.thrown = thrown;
    }
    
    /**
     * Get the number of bounces
     * 
     * @return The number of bounces
     */
    public int getBounces() {
        return bounces;
    }
    
    /**
     * Set the number of bounces
     * 
     * @param bounces The number of bounces
     */
    public void setBounces(int bounces) {
        this.bounces = bounces;
    }
    
    /**
     * Get the last bounce time
     * 
     * @return The last bounce time in milliseconds
     */
    public long getLastBounceTime() {
        return lastBounceTime;
    }
    
    /**
     * Set the last bounce time
     * 
     * @param lastBounceTime The last bounce time in milliseconds
     */
    public void setLastBounceTime(long lastBounceTime) {
        this.lastBounceTime = lastBounceTime;
    }
    
    /**
     * Check if the basketball is on the ground
     * 
     * @return True if on ground
     */
    public boolean isOnGround() {
        return entity.isOnGround();
    }
    
    /**
     * Get the basketball's velocity
     * 
     * @return The velocity vector
     */
    public org.bukkit.util.Vector getVelocity() {
        return entity.getVelocity();
    }
    
    /**
     * Set the basketball's velocity
     * 
     * @param velocity The velocity vector
     */
    public void setVelocity(org.bukkit.util.Vector velocity) {
        entity.setVelocity(velocity);
    }
    
    /**
     * Get the basketball's location
     * 
     * @return The location
     */
    public org.bukkit.Location getLocation() {
        return entity.getLocation();
    }
    
    /**
     * Remove the basketball
     */
    public void remove() {
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
    }
} 