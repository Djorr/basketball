package nl.djorr.basketball.managers;

import nl.djorr.basketball.BasketballPlugin;
import nl.djorr.basketball.objects.Basketball;
import nl.djorr.basketball.objects.BasketballRegion;
import nl.djorr.basketball.utils.ItemUtil;
import nl.djorr.basketball.utils.ItemBuilder;
import nl.djorr.basketball.utils.BasketballTextureUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import nl.djorr.basketball.listeners.BasketballListener;

/**
 * Manages basketball entities and game logic
 * 
 * @author Djorr
 */
public class BasketballManager {
    
    private final BasketballPlugin plugin;
    private final Map<UUID, Basketball> basketballs;
    private final Map<String, BasketballRegion> regions;
    
    private static final String BASKETBALL_UUID = "9a869760-a4ae-49ac-9598-e136ce74ba73";
    private static final String BASKETBALL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWRmODQ3MTVhNjRkYzQ1NTg2ZjdhNjA3OWY4ZTQ5YTk0NzdjMGZlOTY1ODliNGNmZDcxY2JhMzIyNTRhYzgifX19";
    
    /**
     * Constructor for BasketballManager
     * 
     * @param plugin The plugin instance
     */
    public BasketballManager(BasketballPlugin plugin) {
        this.plugin = plugin;
        this.basketballs = new HashMap<>();
        this.regions = new HashMap<>();
    }
    
    /**
     * Create a basketball entity
     * 
     * @param location The location to spawn the basketball
     * @param regionName The region name (optional)
     * @return The basketball object
     */
    public Basketball createBasketball(Location location, String regionName) {
        // Create armor stand with basketball head
        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
        armorStand.setVisible(false);
        armorStand.setGravity(true); // Enable gravity for physics
        armorStand.setSmall(true);
        armorStand.setMarker(false); // Disable marker for physics
        armorStand.setCollidable(false); // Prevent collision with players
        
        // Set basketball head using BasketballTextureUtil
        ItemStack skull = BasketballTextureUtil.createBasketballSkullItem(plugin.getConfigManager().getBasketballName());
        armorStand.setHelmet(skull);
        
        // Create basketball object
        Basketball basketball = new Basketball(armorStand, plugin);
        basketballs.put(armorStand.getUniqueId(), basketball);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Created basketball entity with GameProfile texture: " + armorStand.getUniqueId() + 
                " at " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + 
                (regionName != null ? " in region '" + regionName + "'" : ""));
        }
        
        return basketball;
    }
    
    /**
     * Create a basketball entity (legacy method)
     * 
     * @param location The location to spawn the basketball
     * @return The basketball object
     */
    public Basketball createBasketball(Location location) {
        return createBasketball(location, null);
    }
    
    /**
     * Remove a basketball
     * 
     * @param basketball The basketball to remove
     */
    public void removeBasketball(Basketball basketball) {
        if (basketball != null && basketball.getEntity() != null) {
            basketballs.remove(basketball.getEntity().getUniqueId());
            basketball.getEntity().remove();
        }
    }
    
    /**
     * Remove all basketballs
     */
    public void removeAllBasketballs() {
        for (Basketball basketball : basketballs.values()) {
            if (basketball.getEntity() != null) {
                basketball.getEntity().remove();
            }
        }
        basketballs.clear();
    }
    
    /**
     * Get basketball by entity
     * 
     * @param entity The entity
     * @return The basketball object or null
     */
    public Basketball getBasketball(Entity entity) {
        return basketballs.get(entity.getUniqueId());
    }
    
    /**
     * Check if entity is a basketball
     * 
     * @param entity The entity to check
     * @return True if it's a basketball
     */
    public boolean isBasketball(Entity entity) {
        return basketballs.containsKey(entity.getUniqueId());
    }
    
    /**
     * Throw basketball
     * 
     * @param player The player throwing
     * @param basketball The basketball being thrown
     */
    public void throwBasketball(Player player, Basketball basketball) {
        if (basketball == null || basketball.getEntity() == null) {
            return;
        }
        
        // Remove basketball from player's inventory
        removeBasketballFromInventory(player);
        
        // Make basketball visible when thrown
        basketball.getEntity().setVisible(true);
        
        // Calculate throw direction
        Vector direction = player.getLocation().getDirection();
        double velocity = plugin.getConfigManager().getThrowVelocity();
        double arc = plugin.getConfigManager().getThrowArc();
        
        // Add arc to the throw
        Vector velocityVector = direction.multiply(velocity);
        velocityVector.setY(velocityVector.getY() + arc);
        
        // Set basketball velocity
        basketball.getEntity().setVelocity(velocityVector);
        basketball.setThrown(true);
        basketball.setBounces(0);
        
        // Send message
        player.sendMessage(plugin.getConfigManager().getMessageWithPrefix("basketball_thrown"));
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Basketball thrown with velocity: " + velocityVector);
        }
    }
    
    /**
     * Handle basketball physics with realistic bounce physics and auto-pickup
     * 
     * @param basketball The basketball
     */
    public void handlePhysics(Basketball basketball) {
        if (!basketball.isThrown()) {
            return;
        }
        
        Vector velocity = basketball.getVelocity();
        Location ballLocation = basketball.getLocation();
        
        // NEW: Check if basketball is outside region or stuck on ground
        if (checkBasketballOutOfBounds(basketball)) {
            return; // Basketball will be respawned at bedrock
        }
        
        // Apply realistic gravity
        velocity.setY(velocity.getY() - 0.08); // Basketball gravity
        
        // Check for ground collision with realistic detection
        if (velocity.getY() < 0) {
            Location groundCheck = ballLocation.clone().add(0, -1, 0); // Check below the ball
            if (groundCheck.getBlock().getType().isSolid()) {
                // Ball hit ground, bounce with realistic physics
                handleRealisticBounce(basketball);
                return;
            }
        }
        
        // Check for wall collision (ALWAYS check region boundaries)
        handleWallCollision(basketball);
        
        // Apply realistic air resistance
        velocity.multiply(0.995); // Less air resistance for longer flight
        
        // Stop ball if velocity is very low
        if (Math.abs(velocity.getX()) < 0.05 && Math.abs(velocity.getZ()) < 0.05 && Math.abs(velocity.getY()) < 0.05) {
            velocity.setX(0);
            velocity.setY(0);
            velocity.setZ(0);
            basketball.setThrown(false);
            basketball.setVelocity(velocity);
            
            // Place basketball as skull block on the ground
            Location groundLocation = basketball.getLocation().clone();
            groundLocation.setY(groundLocation.getBlockY()); // Zorg dat Y een heel getal is
            
            // Check of het blok op de grond vrij is
            if (groundLocation.getBlock().getType() == org.bukkit.Material.AIR) {
                // Plaats basketball skull blok
                groundLocation.getBlock().setType(org.bukkit.Material.SKULL);
                groundLocation.getBlock().setData((byte) 1); // Floor skull
                
                // Set de skull texture met BasketballTextureUtil
                org.bukkit.block.BlockState state = groundLocation.getBlock().getState();
                if (state instanceof org.bukkit.block.Skull) {
                    org.bukkit.block.Skull skull = (org.bukkit.block.Skull) state;
                    
                    // Apply basketball texture using BasketballTextureUtil (this also sets the skull type)
                    BasketballTextureUtil.applyBasketballTexture(skull);
                    
                    // Also apply texture to the material data
                    try {
                        org.bukkit.material.Skull materialSkull = (org.bukkit.material.Skull) skull.getData();
                        BasketballTextureUtil.applyBasketballTexture(materialSkull);
                        skull.setData(materialSkull);
                        skull.update();
                    } catch (Exception e) {
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().warning("Could not apply material skull texture: " + e.getMessage());
                        }
                    }
                    
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Basketball placed as skull block on ground at " + groundLocation);
                    }
                }
                
                // Particle effect
                groundLocation.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, groundLocation.clone().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
                groundLocation.getWorld().spawnParticle(org.bukkit.Particle.CRIT, groundLocation.clone().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
                // Geluid
                groundLocation.getWorld().playSound(groundLocation, org.bukkit.Sound.ENTITY_SLIME_SQUISH, 0.8f, 1.2f);
            } else {
                // Blok is niet vrij, probeer 1 blok hoger
                groundLocation.add(0, 1, 0);
                if (groundLocation.getBlock().getType() == org.bukkit.Material.AIR) {
                    groundLocation.getBlock().setType(org.bukkit.Material.SKULL);
                    groundLocation.getBlock().setData((byte) 1);
                    
                    // Set de skull texture met BasketballTextureUtil
                    org.bukkit.block.BlockState state = groundLocation.getBlock().getState();
                    if (state instanceof org.bukkit.block.Skull) {
                        org.bukkit.block.Skull skull = (org.bukkit.block.Skull) state;
                        
                        // Apply basketball texture using BasketballTextureUtil (this also sets the skull type)
                        BasketballTextureUtil.applyBasketballTexture(skull);
                        
                        // Also apply texture to the material data
                        try {
                            org.bukkit.material.Skull materialSkull = (org.bukkit.material.Skull) skull.getData();
                            BasketballTextureUtil.applyBasketballTexture(materialSkull);
                            skull.setData(materialSkull);
                            skull.update();
                        } catch (Exception e) {
                            if (plugin.getConfigManager().isDebugEnabled()) {
                                plugin.getLogger().warning("Could not apply material skull texture: " + e.getMessage());
                            }
                        }
                        
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("Basketball placed as skull block (1 block up) at " + groundLocation);
                        }
                    }
                    
                    // Particle effect
                    groundLocation.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, groundLocation.clone().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
                    groundLocation.getWorld().spawnParticle(org.bukkit.Particle.CRIT, groundLocation.clone().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
                    // Geluid
                    groundLocation.getWorld().playSound(groundLocation, org.bukkit.Sound.ENTITY_SLIME_SQUISH, 0.8f, 1.2f);
                }
            }
            
            // Remove the basketball entity
            removeBasketball(basketball);
            
            // Check for auto-pickup by owner
            checkAutoPickup(basketball);
            
            return;
        }
        
        // Apply velocity and ensure the entity actually moves
        basketball.getEntity().setVelocity(velocity);
        
        // Debug logging
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Basketball physics - Velocity: " + velocity + 
                ", Location: " + ballLocation.getBlockX() + "," + ballLocation.getBlockY() + "," + ballLocation.getBlockZ());
        }
    }
    
    /**
     * Handle realistic basketball bounce physics
     * 
     * @param basketball The basketball
     */
    private void handleRealisticBounce(Basketball basketball) {
        int maxBounces = plugin.getConfigManager().getMaxBounces();
        double bounceHeight = plugin.getConfigManager().getBounceHeight();
        double bounceDecay = plugin.getConfigManager().getBounceDecay();
        
        if (basketball.getBounces() >= maxBounces) {
            // Stop the basketball
            basketball.setVelocity(new Vector(0, 0, 0));
            basketball.setThrown(false);
            
            // Place basketball as skull block on the ground
            Location groundLocation = basketball.getLocation().clone();
            groundLocation.setY(groundLocation.getBlockY()); // Zorg dat Y een heel getal is
            
            // Check of het blok op de grond vrij is
            if (groundLocation.getBlock().getType() == org.bukkit.Material.AIR) {
                // Plaats basketball skull blok
                groundLocation.getBlock().setType(org.bukkit.Material.SKULL);
                groundLocation.getBlock().setData((byte) 1); // Floor skull
                
                // Set de skull texture met BasketballTextureUtil
                org.bukkit.block.BlockState state = groundLocation.getBlock().getState();
                if (state instanceof org.bukkit.block.Skull) {
                    org.bukkit.block.Skull skull = (org.bukkit.block.Skull) state;
                    
                    // Apply basketball texture using BasketballTextureUtil (this also sets the skull type)
                    BasketballTextureUtil.applyBasketballTexture(skull);
                    
                    // Also apply texture to the material data
                    try {
                        org.bukkit.material.Skull materialSkull = (org.bukkit.material.Skull) skull.getData();
                        BasketballTextureUtil.applyBasketballTexture(materialSkull);
                        skull.setData(materialSkull);
                        skull.update();
                    } catch (Exception e) {
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().warning("Could not apply material skull texture: " + e.getMessage());
                        }
                    }
                    
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Basketball placed as skull block on ground (max bounces) at " + groundLocation);
                    }
                }
                
                // Particle effect
                groundLocation.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, groundLocation.clone().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
                groundLocation.getWorld().spawnParticle(org.bukkit.Particle.CRIT, groundLocation.clone().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
                // Geluid
                groundLocation.getWorld().playSound(groundLocation, org.bukkit.Sound.ENTITY_SLIME_SQUISH, 0.8f, 1.2f);
            } else {
                // Blok is niet vrij, probeer 1 blok hoger
                groundLocation.add(0, 1, 0);
                if (groundLocation.getBlock().getType() == org.bukkit.Material.AIR) {
                    groundLocation.getBlock().setType(org.bukkit.Material.SKULL);
                    groundLocation.getBlock().setData((byte) 1);
                    
                    // Set de skull texture met BasketballTextureUtil
                    org.bukkit.block.BlockState state = groundLocation.getBlock().getState();
                    if (state instanceof org.bukkit.block.Skull) {
                        org.bukkit.block.Skull skull = (org.bukkit.block.Skull) state;
                        
                        // Apply basketball texture using BasketballTextureUtil (this also sets the skull type)
                        BasketballTextureUtil.applyBasketballTexture(skull);
                        
                        // Also apply texture to the material data
                        try {
                            org.bukkit.material.Skull materialSkull = (org.bukkit.material.Skull) skull.getData();
                            BasketballTextureUtil.applyBasketballTexture(materialSkull);
                            skull.setData(materialSkull);
                            skull.update();
                        } catch (Exception e) {
                            if (plugin.getConfigManager().isDebugEnabled()) {
                                plugin.getLogger().warning("Could not apply material skull texture: " + e.getMessage());
                            }
                        }
                        
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("Basketball placed as skull block (1 block up, max bounces) at " + groundLocation);
                        }
                    }
                    
                    // Particle effect
                    groundLocation.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, groundLocation.clone().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
                    groundLocation.getWorld().spawnParticle(org.bukkit.Particle.CRIT, groundLocation.clone().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
                    // Geluid
                    groundLocation.getWorld().playSound(groundLocation, org.bukkit.Sound.ENTITY_SLIME_SQUISH, 0.8f, 1.2f);
                }
            }
            
            // Remove the basketball entity
            removeBasketball(basketball);
            
            return;
        }
        
        // Get current velocity and calculate realistic bounce
        Vector velocity = basketball.getVelocity();
        double impactSpeed = Math.abs(velocity.getY());
        
        // Calculate bounce based on impact speed with more realistic physics
        // Basketball bounces well but loses energy progressively
        double bounceMultiplier = 0.75; // Basketball bounces well
        double energyLoss = 0.15; // Energy lost per bounce
        
        // Calculate bounce height based on impact speed and bounce count
        double newY = impactSpeed * bounceMultiplier;
        
        // Apply progressive energy loss (more realistic than exponential decay)
        for (int i = 0; i < basketball.getBounces(); i++) {
            newY *= (1.0 - energyLoss);
        }
        
        // Ensure minimum bounce height for realism
        double minBounceHeight = 0.3;
        if (newY < minBounceHeight && impactSpeed > 0.5) {
            newY = minBounceHeight;
        }
        
        // Apply realistic bounce physics
        velocity.setY(newY);
        
        // Apply realistic horizontal friction (basketball loses speed on bounce)
        double horizontalFriction = 0.85; // Basketball maintains some horizontal speed
        velocity.setX(velocity.getX() * horizontalFriction);
        velocity.setZ(velocity.getZ() * horizontalFriction);
        
        // Apply bounce
        basketball.setVelocity(velocity);
        basketball.setBounces(basketball.getBounces() + 1);
        
        // Spawn realistic bounce particles
        Location bounceLocation = basketball.getEntity().getLocation();
        BasketballRegion region = getBasketballRegion(basketball);
        if (region != null && !region.getPlayersInRegion().isEmpty()) {
            // Spawn particles for all players in region
            for (Player player : region.getPlayersInRegion()) {
                // Impact particles
                player.spawnParticle(
                    org.bukkit.Particle.SMOKE_NORMAL,
                    bounceLocation.add(0, 0.3, 0),
                    2, 0.1, 0.05, 0.1, 0.02
                );
                
                // Bounce particles
                player.spawnParticle(
                    org.bukkit.Particle.VILLAGER_HAPPY,
                    bounceLocation.add(0, 0.5, 0),
                    1, 0.1, 0.1, 0.1, 0.01
                );
            }
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Realistic basketball bounce! Bounce #" + basketball.getBounces() + 
                ", Impact speed: " + impactSpeed + ", New velocity: " + velocity + ", Bounce height: " + newY);
        }
    }
    
    /**
     * Handle basketball bounce (legacy method for compatibility)
     * 
     * @param basketball The basketball
     */
    private void handleBounce(Basketball basketball) {
        handleRealisticBounce(basketball);
    }
    
    /**
     * Handle realistic wall collision and region boundaries
     * 
     * @param basketball The basketball
     */
    private void handleWallCollision(Basketball basketball) {
        Vector velocity = basketball.getVelocity();
        Location ballLocation = basketball.getLocation();
        
        // Check region boundaries first (optimized)
        BasketballRegion region = getBasketballRegion(basketball);
        if (region != null) {
            int[] bounds = region.getRegionBounds();
            if (bounds != null) {
                boolean hitBoundary = false;
                
                // DIRECT CHECK: if the ball is outside the region, bounce it back immediately
                if (ballLocation.getBlockX() < bounds[0] || ballLocation.getBlockX() > bounds[1] ||
                    ballLocation.getBlockZ() < bounds[4] || ballLocation.getBlockZ() > bounds[5]) {
                    // Ball is outside the region, bounce it back to center with extra force
                    Vector toRegionCenter = region.getCenter().toVector().subtract(ballLocation.toVector()).normalize();
                    velocity.setX(toRegionCenter.getX() * 2.0); // Extra force
                    velocity.setZ(toRegionCenter.getZ() * 2.0);
                    velocity.setY(Math.abs(velocity.getY()) + 1.5); // Add upward bounce
                    hitBoundary = true;
                    
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Basketball outside region bounds, bouncing back to center with force");
                    }
                }
                
                // Check X boundaries with keiharde terugstuitering (more aggressive)
                if (ballLocation.getBlockX() <= bounds[0] + 2 || ballLocation.getBlockX() >= bounds[1] - 2) {
                    // Keiharde terugstuitering - bereken richting naar dichtstbijzijnde speler toe
                    Player nearestPlayer = getNearestPlayer(region, ballLocation);
                    if (nearestPlayer != null && nearestPlayer.isOnline()) {
                        // Bounce back towards the nearest player with extra force
                        Vector toPlayer = nearestPlayer.getLocation().toVector().subtract(ballLocation.toVector()).normalize();
                        velocity.setX(toPlayer.getX() * 3.0); // Extra force
                        velocity.setZ(toPlayer.getZ() * 3.0);
                        velocity.setY(Math.abs(velocity.getY()) + 1.5); // Add upward bounce
                    } else {
                        // Fallback: reverse with extra force
                        velocity.setX(-velocity.getX() * 2.0); // Extra force instead of energy loss
                    }
                    hitBoundary = true;
                    
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Basketball hit X boundary, bouncing back");
                    }
                }
                
                // Check Z boundaries with keiharde terugstuitering (more aggressive)
                if (ballLocation.getBlockZ() <= bounds[4] + 2 || ballLocation.getBlockZ() >= bounds[5] - 2) {
                    // Keiharde terugstuitering - bereken richting naar dichtstbijzijnde speler toe
                    Player nearestPlayer = getNearestPlayer(region, ballLocation);
                    if (nearestPlayer != null && nearestPlayer.isOnline()) {
                        // Bounce back towards the nearest player with extra force
                        Vector toPlayer = nearestPlayer.getLocation().toVector().subtract(ballLocation.toVector()).normalize();
                        velocity.setX(toPlayer.getX() * 3.0); // Extra force
                        velocity.setZ(toPlayer.getZ() * 3.0);
                        velocity.setY(Math.abs(velocity.getY()) + 1.5); // Add upward bounce
                    } else {
                        // Fallback: reverse with extra force
                        velocity.setZ(-velocity.getZ() * 2.0); // Extra force instead of energy loss
                    }
                    hitBoundary = true;
                    
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Basketball hit Z boundary, bouncing back");
                    }
                }
                
                // Check Y boundaries (keep ball within height limits)
                if (ballLocation.getBlockY() <= bounds[2] || ballLocation.getBlockY() >= bounds[3]) {
                    velocity.setY(-velocity.getY() * 0.75);
                    hitBoundary = true;
                    
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Basketball hit Y boundary, bouncing back");
                    }
                }
                
                if (hitBoundary) {
                    // Spawn wall collision particles with extra effects for keiharde terugstuitering
                    Location collisionLocation = ballLocation.clone();
                    if (region.getPlayersInRegion().size() > 0) {
                        Player firstPlayer = region.getPlayersInRegion().iterator().next();
                        
                        // Extra particles for keiharde terugstuitering
                        firstPlayer.spawnParticle(
                            org.bukkit.Particle.EXPLOSION_NORMAL,
                            collisionLocation.add(0, 0.5, 0),
                            5, 0.3, 0.3, 0.3, 0.05
                        );
                        
                        firstPlayer.spawnParticle(
                            org.bukkit.Particle.SMOKE_NORMAL,
                            collisionLocation,
                            8, 0.2, 0.2, 0.2, 0.02
                        );
                        
                        // Add bounce sound effect with extra volume
                        firstPlayer.playSound(collisionLocation, org.bukkit.Sound.BLOCK_STONE_HIT, 1.2f, 0.8f);
                        firstPlayer.playSound(collisionLocation, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.2f);
                    }
                    
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Keiharde terugstuitering triggered - New velocity: " + velocity);
                    }
                    
                    // Apply velocity and let the ball bounce naturally
                    basketball.setVelocity(velocity);
                    return; // Don't do regular wall collision if we hit region boundary
                }
            }
        }
        
        // Regular wall collision - check for actual blocks
        Location frontCheck = ballLocation.clone().add(velocity.getX() * 0.5, 0, velocity.getZ() * 0.5);
        if (frontCheck.getBlock().getType().isSolid()) {
            // Ball hit a wall, reverse direction with energy loss
            if (Math.abs(velocity.getX()) > 0.1) {
                velocity.setX(-velocity.getX() * 0.75);
            }
            if (Math.abs(velocity.getZ()) > 0.1) {
                velocity.setZ(-velocity.getZ() * 0.75);
            }
            
            // Spawn wall collision particles
            Location collisionLocation = ballLocation.clone();
            BasketballRegion region2 = getBasketballRegion(basketball);
            if (region2 != null && region2.getPlayersInRegion().size() > 0) {
                Player firstPlayer = region2.getPlayersInRegion().iterator().next();
                firstPlayer.spawnParticle(
                    org.bukkit.Particle.SMOKE_NORMAL,
                    collisionLocation.add(0, 0.5, 0),
                    3, 0.1, 0.1, 0.1, 0.02
                );
                
                // Add bounce sound effect
                firstPlayer.playSound(collisionLocation, org.bukkit.Sound.BLOCK_STONE_HIT, 0.5f, 1.0f);
            }
        }
        
        basketball.setVelocity(velocity);
    }
    
    /**
     * Get the basketball region for a basketball
     * 
     * @param basketball The basketball
     * @return The basketball region or null
     */
    private BasketballRegion getBasketballRegion(Basketball basketball) {
        for (BasketballRegion region : regions.values()) {
            if (region.getCurrentBasketball() == basketball) {
                return region;
            }
        }
        return null;
    }
    
    /**
     * Check if basketball is in pickup range of player
     * 
     * @param player The player
     * @param basketball The basketball
     * @return True if in pickup range
     */
    public boolean isInPickupRange(Player player, Basketball basketball) {
        if (basketball == null) {
            return false;
        }
        
        double pickupRange = plugin.getConfigManager().getPickupRange();
        double distance = player.getLocation().distance(basketball.getLocation());
        
        // Increased pickup range for better usability
        double effectiveRange = pickupRange * 2.0;
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Pickup check - Distance: " + distance + ", Range: " + effectiveRange);
        }
        
        return distance <= effectiveRange;
    }
    
    /**
     * Pick up basketball
     * 
     * @param player The player picking up
     * @param basketball The basketball to pick up
     */
    public void pickupBasketball(Player player, Basketball basketball) {
        if (basketball == null) {
            return;
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Player " + player.getName() + " attempting to pick up basketball...");
        }
        
        // Check if player already has a basketball in inventory
        if (hasBasketballInInventory(player)) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Player " + player.getName() + " already has a basketball, cannot pick up another");
            }
            return;
        }
        
        // Give basketball item to player in first available slot (not slot 8/9)
        ItemStack basketballItem = createBasketballItem();
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Adding basketball item to " + player.getName() + "'s inventory...");
        }
        
        // Find first available slot (skip slot 8 which is the 9th slot)
        boolean itemAdded = false;
        for (int i = 0; i < 8; i++) { // Only use slots 0-7 (first 8 slots)
            if (player.getInventory().getItem(i) == null) {
                player.getInventory().setItem(i, basketballItem);
                itemAdded = true;
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Basketball added to slot " + i + " for " + player.getName());
                }
                break;
            }
        }
        
        // If no slot found in 0-7, use addItem as fallback
        if (!itemAdded) {
            player.getInventory().addItem(basketballItem);
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Basketball added via addItem fallback for " + player.getName());
            }
        }
        
        // Verify the item was added correctly
        if (plugin.getConfigManager().isDebugEnabled()) {
            boolean hasItem = hasBasketballInInventory(player);
            plugin.getLogger().info("Basketball added to inventory - verification: " + hasItem);
        }
        
        // Remove basketball entity
        removeBasketball(basketball);
        
        // Update region
        for (BasketballRegion region : regions.values()) {
            if (region.getCurrentBasketball() == basketball) {
                region.setCurrentBasketball(null);
                // Don't reset owner - anyone can pick up the basketball now!
                break;
            }
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Player " + player.getName() + " picked up basketball");
        }

        // Start auto-drop timer
        if (BasketballListener.instance != null) {
            BasketballListener.instance.startAutoDropTask(player);
        }
    }
    
    /**
     * Check for auto-pickup by any player in the region (multiplayer basketball!)
     * 
     * @param basketball The basketball to check
     */
    private void checkAutoPickup(Basketball basketball) {
        BasketballRegion region = getBasketballRegion(basketball);
        if (region == null) {
            return;
        }
        
        // Check all players in the region for auto-pickup (multiplayer basketball!)
        for (Player player : region.getPlayersInRegion()) {
            if (!player.isOnline()) {
                continue;
            }
            
            // Check if player is close enough for auto-pickup
            double autoPickupRange = 3.0; // 3 blocks auto-pickup range
            double distance = player.getLocation().distance(basketball.getLocation());
            
            if (distance <= autoPickupRange) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Auto-pickup triggered for " + player.getName() + " at distance " + distance);
                }
                
                // Auto-pickup the basketball (anyone can pick it up!)
                pickupBasketball(player, basketball);
                return; // Only pickup by one player
            }
        }
    }
    
    /**
     * Get the nearest player to a location in a region
     * 
     * @param region The basketball region
     * @param location The location to check from
     * @return The nearest player or null
     */
    private Player getNearestPlayer(BasketballRegion region, Location location) {
        Player nearestPlayer = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (Player player : region.getPlayersInRegion()) {
            if (!player.isOnline()) {
                continue;
            }
            
            double distance = player.getLocation().distance(location);
            if (distance < closestDistance) {
                closestDistance = distance;
                nearestPlayer = player;
            }
        }
        
        return nearestPlayer;
    }
    
    /**
     * Check for auto-pickup by any player in the region (legacy method)
     * 
     * @param basketball The basketball to check
     */
    private void checkAutoPickupAnyPlayer(Basketball basketball) {
        BasketballRegion region = getBasketballRegion(basketball);
        if (region == null) {
            return;
        }
        
        // Check all players in the region for auto-pickup
        for (Player player : region.getPlayersInRegion()) {
            if (!player.isOnline()) {
                continue;
            }
            
            // Check if player is close enough for auto-pickup
            double autoPickupRange = 3.0; // 3 blocks auto-pickup range
            double distance = player.getLocation().distance(basketball.getLocation());
            
            if (distance <= autoPickupRange) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Auto-pickup triggered for " + player.getName() + " at distance " + distance);
                }
                
                // Auto-pickup the basketball
                pickupBasketball(player, basketball);
                return; // Only pickup by one player
            }
        }
    }
    
    /**
     * Check if a player has a basketball in their inventory
     * 
     * @param player The player to check
     * @return True if the player has a basketball
     */
    public boolean hasBasketballInInventory(Player player) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Checking if " + player.getName() + " has basketball in inventory...");
        }
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && ItemUtil.isBasketballItem(item)) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Found basketball in " + player.getName() + "'s inventory");
                }
                return true;
            }
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("No basketball found in " + player.getName() + "'s inventory");
        }
        return false;
    }
    
    /**
     * Create basketball item
     * 
     * @return The basketball item
     */
    public ItemStack createBasketballItem() {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Creating basketball item...");
        }
        
        // Create basketball skull item with proper texture
        ItemStack item = BasketballTextureUtil.createBasketballSkullItem(plugin.getConfigManager().getBasketballName());
        
        // Add NBT tag to identify as basketball
        item = io.github.bananapuncher714.nbteditor.NBTEditor.set(item, true, "basketball");
        
        // Verify NBT was added correctly
        if (plugin.getConfigManager().isDebugEnabled()) {
            boolean isBasketball = ItemUtil.isBasketballItem(item);
            plugin.getLogger().info("Created basketball item with NBT - Verification: " + isBasketball);
        }
        
        return item;
    }
    
    /**
     * Register a basketball region
     * 
     * @param name The region name
     * @param region The basketball region
     */
    public void registerRegion(String name, BasketballRegion region) {
        regions.put(name, region);
    }
    
    /**
     * Unregister a basketball region
     * 
     * @param name The region name
     */
    public void unregisterRegion(String name) {
        regions.remove(name);
    }
    
    /**
     * Get basketball region by name
     * 
     * @param regionName The region name
     * @return The basketball region or null
     */
    public BasketballRegion getRegion(String regionName) {
        return regions.get(regionName);
    }
    
    /**
     * Get all basketball regions
     * 
     * @return Map of region names to basketball regions
     */
    public Map<String, BasketballRegion> getRegions() {
        return regions;
    }
    
    /**
     * Get all basketballs
     * 
     * @return Map of basketball UUIDs to basketball objects
     */
    public Map<UUID, Basketball> getBasketballs() {
        return basketballs;
    }
    
    /**
     * Update player wins when a player joins
     * 
     * @param player The player who joined
     */
    public void updatePlayerWins(Player player) {
        for (BasketballRegion region : regions.values()) {
            region.updatePlayerWinsFromUUID(player);
        }
    }

    /**
     * Clean up all basketball entities and blocks in all regions on startup
     */
    public void cleanupBasketballsOnStartup() {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Starting basketball cleanup on startup...");
        }
        
        int removedEntities = 0;
        int removedBlocks = 0;
        
        // Clean up all basketball entities
        for (Basketball basketball : basketballs.values()) {
            if (basketball != null && basketball.getEntity() != null) {
                basketball.getEntity().remove();
                removedEntities++;
            }
        }
        basketballs.clear();
        
        // Clean up basketball skull blocks in all regions
        for (BasketballRegion region : regions.values()) {
            int[] bounds = region.getRegionBounds();
            if (bounds != null) {
                for (int x = bounds[0]; x <= bounds[1]; x++) {
                    for (int y = bounds[2]; y <= bounds[3]; y++) {
                        for (int z = bounds[4]; z <= bounds[5]; z++) {
                            Location loc = new Location(region.getCenter().getWorld(), x, y, z);
                            if (loc.getBlock().getType() == Material.SKULL) {
                                // Check if it's a basketball skull
                                org.bukkit.block.BlockState state = loc.getBlock().getState();
                                if (state instanceof org.bukkit.block.Skull) {
                                    org.bukkit.block.Skull skull = (org.bukkit.block.Skull) state;
                                    if (skull.getSkullType() == org.bukkit.SkullType.PLAYER) {
                                        try {
                                            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) skull.getData();
                                            if (meta.hasOwner() && meta.getOwner().equals("9a869760-a4ae-49ac-9598-e136ce74ba73")) {
                                                // This is a basketball skull block, remove it
                                                loc.getBlock().setType(Material.AIR);
                                                removedBlocks++;
                                                
                                                if (plugin.getConfigManager().isDebugEnabled()) {
                                                    plugin.getLogger().info("Removed basketball skull block at " + loc);
                                                }
                                            }
                                        } catch (Exception e) {
                                            if (plugin.getConfigManager().isDebugEnabled()) {
                                                plugin.getLogger().warning("Error checking basketball skull block: " + e.getMessage());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Basketball cleanup complete - Removed " + removedEntities + " entities and " + removedBlocks + " blocks");
        }
    }

    /**
     * Drop de bal op de grond op een locatie, met particle/geluid
     * Plaatst de bal als blok op de grond binnen de region (2e blok vanaf lijn)
     */
    public void dropBasketballOnGround(Location location) {
        // Check of de locatie binnen een basketball region is
        BasketballRegion region = null;
        for (BasketballRegion r : regions.values()) {
            if (r.containsLocation(location)) {
                region = r;
                break;
            }
        }
        
        if (region == null) {
            // Niet binnen region, plaats niet
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Cannot drop basketball outside region at " + location);
            }
            return;
        }
        
        // Plaats de bal als blok op de grond (2e blok vanaf region lijn)
        Location groundLocation = location.clone();
        groundLocation.setY(groundLocation.getBlockY()); // Zorg dat Y een heel getal is
        
        // Check of het blok op de grond vrij is
        if (groundLocation.getBlock().getType() == Material.AIR) {
            // Plaats basketball skull blok
            groundLocation.getBlock().setType(Material.SKULL);
            groundLocation.getBlock().setData((byte) 1); // Floor skull
            
            // Set de skull texture met BasketballTextureUtil
            org.bukkit.block.BlockState state = groundLocation.getBlock().getState();
            if (state instanceof org.bukkit.block.Skull) {
                org.bukkit.block.Skull skull = (org.bukkit.block.Skull) state;
                
                // Apply basketball texture using BasketballTextureUtil (this also sets the skull type)
                BasketballTextureUtil.applyBasketballTexture(skull);
                
                // Also apply texture to the material data
                try {
                    org.bukkit.material.Skull materialSkull = (org.bukkit.material.Skull) skull.getData();
                    BasketballTextureUtil.applyBasketballTexture(materialSkull);
                    skull.setData(materialSkull);
                    skull.update();
                } catch (Exception e) {
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().warning("Could not apply material skull texture: " + e.getMessage());
                    }
                }
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Basketball skull block created with GameProfile texture");
                }
            }
            
            // Particle effect
            location.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, location.clone().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
            location.getWorld().spawnParticle(org.bukkit.Particle.CRIT, location.clone().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
            // Geluid
            location.getWorld().playSound(location, org.bukkit.Sound.ENTITY_SLIME_SQUISH, 0.8f, 1.2f);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Basketball dropped on ground at " + groundLocation);
            }
        } else {
            // Blok is niet vrij, probeer 1 blok hoger
            groundLocation.add(0, 1, 0);
            if (groundLocation.getBlock().getType() == Material.AIR) {
                groundLocation.getBlock().setType(Material.SKULL);
                groundLocation.getBlock().setData((byte) 1);
                
                // Set de skull texture met BasketballTextureUtil
                org.bukkit.block.BlockState state = groundLocation.getBlock().getState();
                if (state instanceof org.bukkit.block.Skull) {
                    org.bukkit.block.Skull skull = (org.bukkit.block.Skull) state;
                    
                    // Apply basketball texture using BasketballTextureUtil (this also sets the skull type)
                    BasketballTextureUtil.applyBasketballTexture(skull);
                    
                    // Also apply texture to the material data
                    try {
                        org.bukkit.material.Skull materialSkull = (org.bukkit.material.Skull) skull.getData();
                        BasketballTextureUtil.applyBasketballTexture(materialSkull);
                        skull.setData(materialSkull);
                        skull.update();
                    } catch (Exception e) {
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().warning("Could not apply material skull texture: " + e.getMessage());
                        }
                    }
                    
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Basketball skull block created (1 block up) with GameProfile texture");
                    }
                }
                
                // Particle effect
                location.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, location.clone().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
                location.getWorld().spawnParticle(org.bukkit.Particle.CRIT, location.clone().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
                // Geluid
                location.getWorld().playSound(location, org.bukkit.Sound.ENTITY_SLIME_SQUISH, 0.8f, 1.2f);
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Basketball dropped on ground (1 block up) at " + groundLocation);
                }
            }
        }
    }

    /**
     * Verwijder alle basketballen uit de inventory van de speler
     */
    public void removeBasketballFromInventory(Player player) {
        boolean removedAny = false;
        int removedCount = 0;
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("BasketballManager: Checking inventory for basketball items...");
        }
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("BasketballManager: Slot " + i + ": " + item.getType() + ":" + item.getData().getData());
                }
                
                if (ItemUtil.isBasketballItem(item)) {
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("BasketballManager: Found basketball in slot " + i + ", removing...");
                    }
                    // Set the item to null (remove it completely)
                    player.getInventory().setItem(i, null);
                    removedCount++;
                    removedAny = true;
                }
            }
        }
        
        if (removedAny) {
            player.updateInventory();
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("BasketballManager: Removed " + removedCount + " basketball(s) from " + player.getName() + "'s inventory");
            }
        } else if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("BasketballManager: No basketball found in " + player.getName() + "'s inventory");
        }
    }

    /**
     * Check if an item is a basketball item
     * 
     * @param item The item to check
     * @return True if it's a basketball item
     */
    public boolean isBasketballItem(ItemStack item) {
        if (item == null || item.getType() != org.bukkit.Material.SKULL_ITEM) {
            return false;
        }
        
        // Check NBT data for basketball identifier
        return ItemUtil.isBasketballItem(item);
    }

    /**
     * Check if basketball is out of bounds (outside region or stuck on ground)
     * and respawn it at bedrock if needed
     * 
     * @param basketball The basketball to check
     * @return True if basketball was respawned, false otherwise
     */
    private boolean checkBasketballOutOfBounds(Basketball basketball) {
        Location ballLocation = basketball.getLocation();
        BasketballRegion region = getBasketballRegion(basketball);
        
        if (region == null) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Basketball has no region, removing...");
            }
            removeBasketball(basketball);
            return true;
        }
        
        int[] bounds = region.getRegionBounds();
        if (bounds == null) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Region has no bounds, removing basketball...");
            }
            removeBasketball(basketball);
            return true;
        }
        
        // Check if basketball is outside region boundaries
        boolean outsideRegion = ballLocation.getBlockX() < bounds[0] - 1 || ballLocation.getBlockX() > bounds[1] + 1 ||
                              ballLocation.getBlockZ() < bounds[4] - 1 || ballLocation.getBlockZ() > bounds[5] + 1 ||
                              ballLocation.getBlockY() < bounds[2] - 1 || ballLocation.getBlockY() > bounds[3] + 1;
        
        // Check if basketball is stuck on ground (not moving and on ground)
        boolean stuckOnGround = Math.abs(basketball.getVelocity().getX()) < 0.01 && 
                              Math.abs(basketball.getVelocity().getZ()) < 0.01 && 
                              Math.abs(basketball.getVelocity().getY()) < 0.01 &&
                              ballLocation.getBlock().getRelative(0, -1, 0).getType().isSolid();
        
        if (outsideRegion || stuckOnGround) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Basketball out of bounds - Outside: " + outsideRegion + 
                    ", Stuck: " + stuckOnGround + ", Location: " + ballLocation);
            }
            
            // Remove the basketball entity
            removeBasketball(basketball);
            
            // Respawn basketball at bedrock with animation
            respawnBasketballAtBedrock(region);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Respawn basketball at spawn location with animation
     * 
     * @param region The basketball region
     */
    private void respawnBasketballAtBedrock(BasketballRegion region) {
        // Use the region's spawnBasketball method instead of bedrock location
        region.spawnBasketball();
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Basketball respawned using region spawn method");
        }
    }
    
    /**
     * Animate basketball spawning from bedrock
     * 
     * @param basketball The basketball
     * @param spawnLocation The spawn location
     * @param region The basketball region
     */
    private void animateBasketballSpawn(Basketball basketball, Location spawnLocation, BasketballRegion region) {
        new org.bukkit.scheduler.BukkitRunnable() {
            private int ticks = 0;
            private final int maxTicks = 40; // 2 seconds
            private final double maxHeight = 3.0; // Maximum height to rise
            
            @Override
            public void run() {
                if (ticks >= maxTicks || basketball.getEntity() == null || basketball.getEntity().isDead()) {
                    this.cancel();
                    return;
                }
                
                // Calculate rising animation
                double progress = (double) ticks / maxTicks;
                double height = maxHeight * (1 - Math.pow(1 - progress, 2)); // Smooth curve
                
                // Update basketball position
                Location newLocation = spawnLocation.clone().add(0, height, 0);
                basketball.getEntity().teleport(newLocation);
                
                // Add particle effects
                Location particleLocation = newLocation.clone().add(0, 0.5, 0);
                particleLocation.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, particleLocation, 5, 0.2, 0.2, 0.2, 0.02);
                particleLocation.getWorld().spawnParticle(org.bukkit.Particle.CRIT, particleLocation, 3, 0.1, 0.1, 0.1, 0.05);
                
                // Add sound effect
                if (ticks % 10 == 0) { // Every 0.5 seconds
                    particleLocation.getWorld().playSound(particleLocation, org.bukkit.Sound.BLOCK_STONE_PLACE, 0.5f, 1.0f);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Spawn a basketball at the spawn location of a region
     * 
     * @param regionName The region name
     */
    public void spawnBasketball(String regionName) {
        BasketballRegion region = getRegion(regionName);
        if (region == null) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Cannot spawn basketball: Region '" + regionName + "' not found");
            }
            return;
        }
        
        Location spawnLocation = region.getSpawnLocation();
        if (spawnLocation == null) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Cannot spawn basketball: No spawn location for region '" + regionName + "'");
            }
            return;
        }
        
        // Spawn basketball on the ground (at spawn location, not bedrock)
        Basketball basketball = createBasketball(spawnLocation, regionName);
        if (basketball != null) {
            basketballs.put(basketball.getEntity().getUniqueId(), basketball);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Spawned basketball in region '" + regionName + "' at " + 
                    spawnLocation.getBlockX() + "," + spawnLocation.getBlockY() + "," + spawnLocation.getBlockZ());
            }
        }
    }
} 