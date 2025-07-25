package nl.djorr.basketball.listeners;

import nl.djorr.basketball.BasketballPlugin;
import nl.djorr.basketball.objects.Basketball;
import nl.djorr.basketball.objects.BasketballRegion;
import nl.djorr.basketball.utils.ItemUtil;
import nl.djorr.basketball.utils.BasketballAnimation;
import nl.djorr.basketball.utils.BasketballTextureUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Bukkit;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.event.entity.PlayerDeathEvent;
import java.lang.reflect.Field;
import org.bukkit.ChatColor;

/**
 * Listener for basketball-related events
 * 
 * @author Djorr
 */
public class BasketballListener implements Listener {
    
    private final BasketballPlugin plugin;
    public static BasketballListener instance;
    
    // Map om per speler de auto-drop runnable te beheren
    private final Map<Player, BukkitRunnable> autoDropTasks = new HashMap<>();
    
    /**
     * Constructor for BasketballListener
     * 
     * @param plugin The plugin instance
     */
    public BasketballListener(BasketballPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }
    
    /**
     * Handle basketball drop
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        
        // Check if dropped item is a basketball
        if (plugin.getBasketballManager().isBasketballItem(droppedItem)) {
            event.setCancelled(true); // Cancel the drop
            
            // Remove from inventory
            plugin.getBasketballManager().removeBasketballFromInventory(player);
            
            // Get player's current region
            String regionName = plugin.getRegionListener().getPlayerRegion(player);
            if (regionName == null) {
                player.sendMessage(ChatColor.RED + "Je moet in een basketball region staan om een bal te gooien!");
                return;
            }
            
            // Check if region exists in cache
            BasketballRegion region = plugin.getBasketballManager().getRegion(regionName);
            if (region == null) {
                player.sendMessage(ChatColor.RED + "Basketball region niet gevonden!");
                return;
            }
            
            // Create basketball at player location and throw it
            Location throwLocation = player.getLocation().add(0, 1.5, 0); // Spawn at head level
            Basketball basketball = plugin.getBasketballManager().createBasketball(throwLocation, regionName);
            
            if (basketball != null) {
                // Set as current basketball for the region
                region.setCurrentBasketball(basketball);
                region.setBasketballOwner(player);
                
                // Throw the basketball with animation
                plugin.getBasketballManager().throwBasketball(player, basketball);
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Player " + player.getName() + " threw basketball in region: " + regionName);
                }
            }
        }
    }
    

    
    /**
     * Handle basketball physics using a scheduled task
     */
    public void startPhysicsTask() {
        new org.bukkit.scheduler.BukkitRunnable() {
            private int tickCounter = 0;
            
            @Override
            public void run() {
                tickCounter++;
                
                // Only process if there are thrown basketballs to reduce lag
                boolean hasThrownBasketballs = false;
                for (Basketball basketball : plugin.getBasketballManager().getBasketballs().values()) {
                    if (basketball.isThrown()) {
                        hasThrownBasketballs = true;
                        break;
                    }
                }
                
                if (!hasThrownBasketballs) {
                    return; // Skip processing if no thrown basketballs
                }
                
                for (Basketball basketball : plugin.getBasketballManager().getBasketballs().values()) {
                    if (basketball.isThrown()) {
                        // Handle physics
                        plugin.getBasketballManager().handlePhysics(basketball);
                        
                        // Check for scoring (only every 2 ticks to reduce lag)
                        if (tickCounter % 2 == 0) {
                            checkForScore(basketball);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 2L); // Run every 2 ticks for better performance but still frequent enough
    }
    
    /**
     * Handle basketball collision with blocks
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        
        // Check if entity is a basketball
        if (plugin.getBasketballManager().isBasketball(entity)) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Handle player pickup of basketball (left-click or auto-pickup)
     * Start auto-drop timer bij pickup
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Handle right-clicking with basketball item (place basketball)
        if (item != null && ItemUtil.isBasketballItem(item) && event.getAction() == Action.RIGHT_CLICK_AIR) {
            // Place basketball in world
            event.setCancelled(true);
            
            // Remove item from inventory - find and remove the basketball item with NBT tag
            if (removeBasketballFromInventory(player)) {
                // Find basketball region for player
                BasketballRegion region = getBasketballRegionForPlayer(player);
                if (region != null) {
                    // Create basketball at player location
                    Basketball basketball = plugin.getBasketballManager().createBasketball(player.getLocation());
                    region.setCurrentBasketball(basketball);
                    
                    player.sendMessage(plugin.getConfigManager().getMessageWithPrefix("basketball_spawned"));
                }
            } else {
                player.sendMessage(plugin.getConfigManager().getMessageWithPrefix("no_basketball"));
            }
        }
        
        // Handle left-click pickup of basketball blocks (PRIORITY)
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Player " + player.getName() + " left-clicked block: " + 
                    (event.getClickedBlock() != null ? event.getClickedBlock().getType() : "null"));
            }
            
            // Check if the clicked block is a basketball skull
            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.SKULL) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Clicked block is a SKULL");
                }
                
                org.bukkit.block.BlockState state = event.getClickedBlock().getState();
                if (state instanceof org.bukkit.block.Skull) {
                    org.bukkit.block.Skull skull = (org.bukkit.block.Skull) state;
                    
                    // Check if it's a basketball skull by checking the texture (works for both PLAYER and SKELETON types)
                    try {
                        // Get the material data from the skull block
                        org.bukkit.material.Skull materialSkull = (org.bukkit.material.Skull) skull.getData();
                        
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("Checking skull block - Type: " + skull.getSkullType() + 
                                ", Material: " + materialSkull.getItemType() + ", Data: " + materialSkull.getData());
                        }
                        
                        if (BasketballTextureUtil.isBasketballSkull(materialSkull)) {
                            if (plugin.getConfigManager().isDebugEnabled()) {
                                plugin.getLogger().info("This is a basketball skull block!");
                            }
                            
                            // This is a basketball block!
                            event.setCancelled(true);
                            
                            // Check if player is in a basketball region
                            BasketballRegion region = getBasketballRegionForPlayer(player);
                            if (region != null) {
                                if (plugin.getConfigManager().isDebugEnabled()) {
                                    plugin.getLogger().info("Player is in region, removing block and adding to inventory");
                                }
                                
                                // Remove the basketball block
                                event.getClickedBlock().setType(Material.AIR);
                                
                                // Add basketball to player's inventory
                                ItemStack basketballItem = plugin.getBasketballManager().createBasketballItem();
                                player.getInventory().addItem(basketballItem);
                                player.updateInventory();
                                
                                // Start auto-drop timer
                                startAutoDropTask(player);
                                
                                // Particle effect
                                Location blockLoc = event.getClickedBlock().getLocation().add(0.5, 0.5, 0.5);
                                blockLoc.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, blockLoc, 20, 0.3, 0.3, 0.3, 0.05);
                                blockLoc.getWorld().spawnParticle(org.bukkit.Particle.CRIT, blockLoc, 10, 0.2, 0.2, 0.2, 0.1);
                                // Geluid
                                blockLoc.getWorld().playSound(blockLoc, org.bukkit.Sound.ENTITY_SLIME_SQUISH, 0.8f, 1.2f);
                                
                                if (plugin.getConfigManager().isDebugEnabled()) {
                                    plugin.getLogger().info("Player " + player.getName() + " picked up basketball block");
                                }
                                
                                // Return early to prevent legacy entity pickup
                                return;
                            } else {
                                if (plugin.getConfigManager().isDebugEnabled()) {
                                    plugin.getLogger().info("Player is not in a basketball region");
                                }
                            }
                        } else {
                            if (plugin.getConfigManager().isDebugEnabled()) {
                                // Get more debug info about the skull
                                plugin.getLogger().info("Material skull data: " + materialSkull.getData() + " (Expected: 3 for player skull)");
                                plugin.getLogger().info("Skull is not a basketball skull - Data check failed");
                            }
                        }
                    } catch (Exception e) {
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().warning("Error checking basketball block: " + e.getMessage());
                        }
                    }
                } else {
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Block state is not a skull");
                    }
                }
            } else {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Clicked block is not a SKULL");
                }
            }
        }
        
        // Handle left-click pickup of basketballs (legacy entity pickup) - ONLY if no block was picked up
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            handleLegacyEntityPickup(player);
        }
    }
    
    /**
     * Handle legacy entity pickup (for backward compatibility)
     */
    private void handleLegacyEntityPickup(Player player) {
        // Check if there's a basketball nearby that can be picked up
        for (Basketball basketball : plugin.getBasketballManager().getBasketballs().values()) {
            if (plugin.getBasketballManager().isInPickupRange(player, basketball)) {
                // Check if region is not animating
                BasketballRegion region = getBasketballRegionForPlayer(player);
                if (region != null && !region.isAnimating()) {
                    // Pick up the basketball (region update happens in pickupBasketball)
                    plugin.getBasketballManager().pickupBasketball(player, basketball);
                    
                    // Debug logging
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Player " + player.getName() + " picked up basketball via left-click");
                    }
                    
                    // Voeg na pickupBasketball() aanroep toe:
                    startAutoDropTask(player);
                    
                    break;
                }
            }
        }
    }
    
    /**
     * Handle player quit: drop bal op de grond als speler hem heeft
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        BasketballRegion region = getBasketballRegionForPlayer(player);
        if (region != null) {
            if (plugin.getBasketballManager().hasBasketballInInventory(player)) {
                // Drop de bal op de grond op de plek van de speler
                plugin.getBasketballManager().dropBasketballOnGround(player.getLocation());
                plugin.getBasketballManager().removeBasketballFromInventory(player);
                stopAutoDropTask(player);
            }
            
            // Check of dit de laatste speler is die de region verlaat
            if (region.getPlayerCount() == 1) {
                // Dit is de laatste speler, verwijder alle basketball blokken
                region.removeAllBasketballBlocks();
            }
        }
    }
    
    /**
     * Handle player death: drop bal op de grond als speler hem heeft
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        BasketballRegion region = getBasketballRegionForPlayer(player);
        if (region != null) {
            if (plugin.getBasketballManager().hasBasketballInInventory(player)) {
                plugin.getBasketballManager().dropBasketballOnGround(player.getLocation());
                plugin.getBasketballManager().removeBasketballFromInventory(player);
                stopAutoDropTask(player);
            }
            
            // Check of dit de laatste speler is die de region verlaat
            if (region.getPlayerCount() == 1) {
                // Dit is de laatste speler, verwijder alle basketball blokken
                region.removeAllBasketballBlocks();
            }
        }
    }
    
    /**
     * Check if a basketball scored
     * 
     * @param basketball The basketball to check
     */
    private void checkForScore(Basketball basketball) {
        Location ballLocation = basketball.getLocation();
        
        // Find the region this basketball belongs to
        for (BasketballRegion region : plugin.getBasketballManager().getRegions().values()) {
            if (region.getCurrentBasketball() == basketball) {
                // Check if ball is near a hoop (hopper)
                if (region.isNearHoop(ballLocation)) {
                    // Check if ball is at the right height for scoring
                    double scoreHeight = plugin.getConfigManager().getScoreHeight();
                    Location nearestHoop = region.getNearestHoop(ballLocation);
                    
                    if (Math.abs(ballLocation.getY() - nearestHoop.getY()) <= scoreHeight) {
                        // Score! Handle hopper scoring
                        handleHopperScore(basketball, region, nearestHoop);
                    }
                }
                break;
            }
        }
    }
    
    /**
     * Handle a basketball scoring through a hopper
     * 
     * @param basketball The basketball that scored
     * @param region The region where the score occurred
     * @param hopperLocation The hopper location
     */
    private void handleHopperScore(Basketball basketball, BasketballRegion region, Location hopperLocation) {
        // Find the player who threw the basketball (closest player)
        Player scoringPlayer = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (Player player : region.getPlayersInRegion()) {
            double distance = player.getLocation().distance(basketball.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                scoringPlayer = player;
            }
        }
        
        if (scoringPlayer != null) {
            // Add score
            int points = plugin.getConfigManager().getPointsPerBasket();
            plugin.getScoreManager().addScore(scoringPlayer, points);
            
            // Create Rocket League-style particle explosion from hopper
            createScoreExplosion(hopperLocation, region);
            
            // Send title to all players in region
            sendScoreTitle(scoringPlayer, region);
            
            // Make basketball invisible when scoring
            basketball.getEntity().setVisible(false);
            
            // Animate basketball falling through hopper
            animateBasketballThroughHopper(basketball, hopperLocation, region);
            
            // Add score animation
            BasketballAnimation animation = new BasketballAnimation(plugin);
            animation.animateBasketballScore(hopperLocation, region);
        }
    }
    
    /**
     * Animate basketball falling through hopper
     * 
     * @param basketball The basketball
     * @param hopperLocation The hopper location
     * @param region The basketball region
     */
    private void animateBasketballThroughHopper(Basketball basketball, Location hopperLocation, BasketballRegion region) {
        // Remove basketball from game
        plugin.getBasketballManager().removeBasketball(basketball);
        region.setCurrentBasketball(null);
        
        // Create falling animation
        Location fallStart = hopperLocation.clone().add(0, 1, 0);
        Location fallEnd = hopperLocation.clone().add(0, -2, 0);
        
        // Create falling basketball entity
        Basketball fallingBasketball = plugin.getBasketballManager().createBasketball(fallStart);
        
        // Animate falling
        new BukkitRunnable() {
            private int ticks = 0;
            private final int maxTicks = 40; // 2 seconds
            
            @Override
            public void run() {
                ticks++;
                double progress = (double) ticks / maxTicks;
                
                // Calculate position
                Location currentPos = fallStart.clone().add(
                    (fallEnd.getX() - fallStart.getX()) * progress,
                    (fallEnd.getY() - fallStart.getY()) * progress,
                    (fallEnd.getZ() - fallStart.getZ()) * progress
                );
                
                fallingBasketball.getEntity().teleport(currentPos);
                
                if (ticks >= maxTicks) {
                    // Animation complete, remove falling basketball
                    plugin.getBasketballManager().removeBasketball(fallingBasketball);
                    
                    // Place basketball as skull block on the ground
                    Location groundLocation = fallEnd.clone();
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
                                plugin.getLogger().info("Basketball placed as skull block on ground (after score) at " + groundLocation);
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
                                    plugin.getLogger().info("Basketball placed as skull block (1 block up, after score) at " + groundLocation);
                                }
                            }
                            
                            // Particle effect
                            groundLocation.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, groundLocation.clone().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
                            groundLocation.getWorld().spawnParticle(org.bukkit.Particle.CRIT, groundLocation.clone().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
                            // Geluid
                            groundLocation.getWorld().playSound(groundLocation, org.bukkit.Sound.ENTITY_SLIME_SQUISH, 0.8f, 1.2f);
                        }
                    }
                    
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
    
    /**
     * Handle a basketball scoring
     * 
     * @param basketball The basketball that scored
     * @param region The region where the score occurred
     */
    private void handleScore(Basketball basketball, BasketballRegion region) {
        // Get the player who threw the basketball
        Player scoringPlayer = region.getBasketballOwner();
        if (scoringPlayer == null) {
            // If no owner, try to find the nearest player
            double minDistance = Double.MAX_VALUE;
            for (Player player : region.getPlayersInRegion()) {
                double distance = player.getLocation().distance(basketball.getLocation());
                if (distance < minDistance) {
                    minDistance = distance;
                    scoringPlayer = player;
                }
            }
        }
        
        if (scoringPlayer != null) {
            // Add score to the player
            boolean gameWon = region.addScore(scoringPlayer);
            
            // Show score message to all players in region
            for (Player player : region.getPlayersInRegion()) {
                player.sendMessage("§e§lSCORE! §f" + scoringPlayer.getName() + " §ehas scored! §f(" + region.getPlayerScore(scoringPlayer) + "/10)");
            }
            
            // If game is won, don't spawn new basketball
            if (gameWon) {
                return;
            }
        }
        
        // Remove the basketball entity
        plugin.getBasketballManager().removeBasketball(basketball);
        region.setCurrentBasketball(null);
        region.setBasketballOwner(null);
        
        // Spawn new basketball after a short delay
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!region.isGameWon()) {
                    region.spawnBasketball();
                }
            }
        }.runTaskLater(plugin, 20L); // 1 second delay
    }
    
    /**
     * Animate basketball throw with realistic physics
     * 
     * @param player The player throwing
     * @param basketball The basketball being thrown
     * @param region The basketball region
     */
    private void animateBasketballThrow(Player player, Basketball basketball, BasketballRegion region) {
        // Get player's look direction and position
        org.bukkit.Location playerLoc = player.getLocation();
        org.bukkit.util.Vector direction = playerLoc.getDirection();
        
        // Calculate realistic throw physics with trajectory
        double maxDistance = 5.0; // Maximum throw distance
        final double throwPower = 3.0; // Higher power for higher arc
        final double throwAngle = 1.2; // Higher arc
        
        // Calculate trajectory based on player's pitch (looking up/down)
        double pitch = Math.toRadians(playerLoc.getPitch());
        
        // Create realistic arc trajectory
        // When looking up (negative pitch), throw higher
        // When looking down (positive pitch), throw lower
        double horizontalPower = throwPower * Math.cos(pitch);
        double verticalPower = throwPower * Math.sin(pitch) + throwAngle;
        
        // Limit horizontal distance to maxDistance
        double horizontalDistance = Math.sqrt(direction.getX() * direction.getX() + direction.getZ() * direction.getZ());
        if (horizontalDistance > 0) {
            double scaleFactor = Math.min(maxDistance / horizontalDistance, 1.0);
            horizontalPower *= scaleFactor;
        }
        
        // Create velocity vector with realistic arc
        org.bukkit.util.Vector velocityVector = new org.bukkit.util.Vector(
            direction.getX() * horizontalPower,
            verticalPower,
            direction.getZ() * horizontalPower
        );
        
        // Position basketball at player's hand level
        org.bukkit.Location throwLocation = playerLoc.clone().add(0, 1.2, 0); // Hand level
        basketball.getEntity().teleport(throwLocation);
        basketball.getEntity().setVisible(false); // Keep invisible during flight
        
        // Apply velocity to basketball
        basketball.getEntity().setVelocity(velocityVector);
        basketball.setThrown(true);
        basketball.setBounces(0);
        
        // Add particles during flight
        addBasketballFlightParticles(basketball, region);
        
        // Add realistic rotation based on velocity
        new org.bukkit.scheduler.BukkitRunnable() {
            private int ticks = 0;
            private final int maxTicks = 40; // 2 seconds of rotation
            private final double rotationSpeed = 15.0; // degrees per tick
            
            @Override
            public void run() {
                ticks++;
                
                // Only rotate if basketball is still moving
                if (basketball.isThrown() && basketball.getEntity().getVelocity().lengthSquared() > 0.1) {
                    org.bukkit.Location currentLoc = basketball.getEntity().getLocation();
                    
                    // Calculate rotation based on movement direction
                    org.bukkit.util.Vector velocity = basketball.getEntity().getVelocity();
                    double horizontalSpeed = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
                    
                    // Rotate faster when moving faster
                    double currentRotationSpeed = rotationSpeed * (horizontalSpeed / throwPower);
                    float yaw = currentLoc.getYaw() + (float) currentRotationSpeed;
                    currentLoc.setYaw(yaw);
                    basketball.getEntity().teleport(currentLoc);
                    
                    // Spawn particles only when moving fast
                    if (horizontalSpeed > 0.5 && ticks % 2 == 0) {
                        org.bukkit.Location particleLoc = basketball.getEntity().getLocation().add(0, 0.3, 0);
                        for (Player p : region.getPlayersInRegion()) {
                            p.spawnParticle(
                                org.bukkit.Particle.SMOKE_NORMAL,
                                particleLoc,
                                1, 0.05, 0.05, 0.05, 0.005
                            );
                        }
                    }
                }
                
                if (ticks >= maxTicks || !basketball.isThrown()) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        
        // Don't send message to player (as requested)
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Realistic basketball throw - Power: " + throwPower + 
                ", Angle: " + throwAngle + ", Pitch: " + Math.toDegrees(pitch) + "°" +
                ", Max Distance: " + maxDistance + ", Velocity: " + velocityVector);
        }
    }
    
    /**
     * Add particles during basketball flight
     * 
     * @param basketball The basketball
     * @param region The basketball region
     */
    private void addBasketballFlightParticles(Basketball basketball, BasketballRegion region) {
        new org.bukkit.scheduler.BukkitRunnable() {
            private int ticks = 0;
            private final int maxTicks = 60; // 3 seconds
            
            @Override
            public void run() {
                ticks++;
                
                // Only add particles if basketball is still thrown and moving
                if (!basketball.isThrown() || basketball.getEntity().getVelocity().lengthSquared() < 0.1) {
                    this.cancel();
                    return;
                }
                
                // Add particles every 3 ticks to reduce lag
                if (ticks % 3 == 0) {
                    org.bukkit.Location particleLoc = basketball.getEntity().getLocation().add(0, 0.3, 0);
                    
                    // Send particles to all players in region
                    for (Player player : region.getPlayersInRegion()) {
                        // Basketball trail particles
                        player.spawnParticle(
                            org.bukkit.Particle.SMOKE_NORMAL,
                            particleLoc,
                            1, 0.05, 0.05, 0.05, 0.005
                        );
                        
                        // Basketball glow particles
                        player.spawnParticle(
                            org.bukkit.Particle.VILLAGER_HAPPY,
                            particleLoc,
                            1, 0.1, 0.1, 0.1, 0.01
                        );
                    }
                }
                
                if (ticks >= maxTicks) {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
    
    /**
     * Check if an item is a basketball
     * 
     * @param item The item to check
     * @return True if it's a basketball
     */
    private boolean isBasketballItem(ItemStack item) {
        return ItemUtil.isBasketballItem(item);
    }
    
    /**
     * Verwijder alle basketballen uit de inventory van de speler
     * 
     * @param player The player
     * @return True if any basketball was found and removed
     */
    private boolean removeBasketballFromInventory(Player player) {
        boolean removedAny = false;
        int removedCount = 0;
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Checking inventory for basketball items...");
        }
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Slot " + i + ": " + item.getType() + ":" + item.getData().getData());
                }
                
                if (ItemUtil.isBasketballItem(item)) {
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Found basketball in slot " + i + ", removing...");
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
            
            // Stop auto-drop timer when basketball is removed from inventory
            stopAutoDropTask(player);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Removed " + removedCount + " basketball(s) from " + player.getName() + "'s inventory");
            }
        } else if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("No basketball found in " + player.getName() + "'s inventory");
        }
        
        return removedAny;
    }
    
    /**
     * Get the basketball region for a player
     * 
     * @param player The player
     * @return The basketball region or null
     */
    private BasketballRegion getBasketballRegionForPlayer(Player player) {
        for (BasketballRegion region : plugin.getBasketballManager().getRegions().values()) {
            if (region.hasPlayer(player)) {
                return region;
            }
        }
        return null;
    }
    
    /**
     * Create a Rocket League-style particle explosion from the hopper
     * 
     * @param hopperLocation The hopper location
     * @param region The basketball region
     */
    private void createScoreExplosion(Location hopperLocation, BasketballRegion region) {
        // Create explosion particles in a sphere pattern
        new BukkitRunnable() {
            private int tick = 0;
            private final int maxTicks = 20; // 1 second
            
            @Override
            public void run() {
                if (tick >= maxTicks) {
                    this.cancel();
                    return;
                }
                
                // Create particles in expanding sphere
                double radius = 1.0 + (tick * 0.3); // Expand over time
                int particles = 15 + (tick * 2); // More particles as explosion grows
                
                for (int i = 0; i < particles; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double phi = Math.acos(2 * Math.random() - 1);
                    
                    double x = hopperLocation.getX() + radius * Math.sin(phi) * Math.cos(angle);
                    double y = hopperLocation.getY() + radius * Math.cos(phi);
                    double z = hopperLocation.getZ() + radius * Math.sin(phi) * Math.sin(angle);
                    
                    Location particleLoc = new Location(hopperLocation.getWorld(), x, y, z);
                    
                    // Send particles only to players in the region
                    for (Player player : region.getPlayersInRegion()) {
                        player.spawnParticle(org.bukkit.Particle.FIREWORKS_SPARK, particleLoc, 1, 0, 0, 0, 0);
                        player.spawnParticle(org.bukkit.Particle.SPELL_WITCH, particleLoc, 1, 0, 0, 0, 0);
                    }
                }
                
                tick++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
    
    /**
     * Send score title to all players in the region
     * 
     * @param scoringPlayer The player who scored
     * @param region The basketball region
     */
    private void sendScoreTitle(Player scoringPlayer, BasketballRegion region) {
        // Send title to all players in the region
        for (Player player : region.getPlayersInRegion()) {
            // Use reflection for 1.12.2 compatibility
            try {
                // Send times packet first
                Object timesPacket = createTimesPacket(10, 40, 10);
                sendPacket(player, timesPacket);
                
                // Send title packet
                Object titlePacket = createTitlePacket(scoringPlayer.getName() + " §fheeft gescoord!");
                sendPacket(player, titlePacket);
                
                // Send subtitle packet
                Object subtitlePacket = createSubtitlePacket("§e" + scoringPlayer.getName() + " §fheeft gescoord!");
                sendPacket(player, subtitlePacket);
                
            } catch (Exception e) {
                // Fallback: send chat message
                player.sendMessage("§e§lSCORE! §f" + scoringPlayer.getName() + " heeft gescoord!");
            }
        }
    }
    
    /**
     * Send a packet to a player using reflection
     * 
     * @param player The player
     * @param packet The packet to send
     */
    private void sendPacket(Player player, Object packet) throws Exception {
        Object playerConnection = player.getClass().getMethod("getHandle").invoke(player);
        Object networkManager = playerConnection.getClass().getField("playerConnection").get(playerConnection);
        networkManager.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(networkManager, packet);
    }
    
    /**
     * Create a times packet using reflection
     * 
     * @param fadeIn Fade in ticks
     * @param stay Stay ticks
     * @param fadeOut Fade out ticks
     * @return The packet object
     */
    private Object createTimesPacket(int fadeIn, int stay, int fadeOut) throws Exception {
        Class<?> packetClass = getNMSClass("PacketPlayOutTitle");
        Class<?> enumClass = getNMSClass("PacketPlayOutTitle$EnumTitleAction");
        
        return packetClass.getConstructor(enumClass, getNMSClass("IChatBaseComponent"), int.class, int.class, int.class).newInstance(
            enumClass.getField("TIMES").get(null),
            null,
            fadeIn,
            stay,
            fadeOut
        );
    }
    
    /**
     * Create a title packet using reflection
     * 
     * @param title The title text
     * @return The packet object
     */
    private Object createTitlePacket(String title) throws Exception {
        Class<?> packetClass = getNMSClass("PacketPlayOutTitle");
        Class<?> enumClass = getNMSClass("PacketPlayOutTitle$EnumTitleAction");
        
        return packetClass.getConstructor(enumClass, getNMSClass("IChatBaseComponent")).newInstance(
            enumClass.getField("TITLE").get(null),
            createChatComponent(title)
        );
    }
    
    /**
     * Create a subtitle packet using reflection
     * 
     * @param subtitle The subtitle text
     * @return The packet object
     */
    private Object createSubtitlePacket(String subtitle) throws Exception {
        Class<?> packetClass = getNMSClass("PacketPlayOutTitle");
        Class<?> enumClass = getNMSClass("PacketPlayOutTitle$EnumTitleAction");
        
        return packetClass.getConstructor(enumClass, getNMSClass("IChatBaseComponent")).newInstance(
            enumClass.getField("SUBTITLE").get(null),
            createChatComponent(subtitle)
        );
    }
    
    /**
     * Create a chat component using reflection
     * 
     * @param text The text
     * @return The chat component
     */
    private Object createChatComponent(String text) throws Exception {
        Class<?> chatComponentClass = getNMSClass("IChatBaseComponent$ChatSerializer");
        return chatComponentClass.getMethod("a", String.class).invoke(null, "{\"text\":\"" + text + "\"}");
    }
    
    /**
     * Get NMS class using reflection
     * 
     * @param className The class name
     * @return The class
     */
    private Class<?> getNMSClass(String className) {
        try {
            return Class.forName("net.minecraft.server." + getServerVersion() + "." + className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    /**
     * Get server version
     * 
     * @return The server version
     */
    private String getServerVersion() {
        return plugin.getServer().getClass().getPackage().getName().split("\\.")[3];
    }

    /**
     * Helper om auto-drop task te starten/stoppen
     */
    public void startAutoDropTask(Player player) {
        stopAutoDropTask(player);
        BukkitRunnable task = new BukkitRunnable() {
            private Location lastLocation = player.getLocation().clone();
            private int stillTicks = 0;
            private int moveTicks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !plugin.getBasketballManager().hasBasketballInInventory(player)) {
                    stopAutoDropTask(player);
                    cancel();
                    return;
                }
                Location current = player.getLocation();
                if (current.distanceSquared(lastLocation) < 0.01) {
                    stillTicks++;
                    moveTicks = 0;
                } else {
                    moveTicks++;
                    stillTicks = 0;
                }
                lastLocation = current.clone();
                if (stillTicks >= 60) { // 3 seconden stilstaan
                    // Check of speler binnen region is
                    BasketballRegion region = getBasketballRegionForPlayer(player);
                    if (region != null) {
                        plugin.getBasketballManager().dropBasketballOnGround(player.getLocation());
                        plugin.getBasketballManager().removeBasketballFromInventory(player);
                    }
                    stopAutoDropTask(player);
                    cancel();
                } else if (moveTicks >= 100) { // 5 seconden lopen
                    // Check of speler binnen region is
                    BasketballRegion region = getBasketballRegionForPlayer(player);
                    if (region != null) {
                        plugin.getBasketballManager().dropBasketballOnGround(player.getLocation());
                        plugin.getBasketballManager().removeBasketballFromInventory(player);
                    }
                    stopAutoDropTask(player);
                    cancel();
                }
            }
        };
        autoDropTasks.put(player, task);
        task.runTaskTimer(plugin, 1L, 1L);
    }
    public void stopAutoDropTask(Player player) {
        BukkitRunnable task = autoDropTasks.remove(player);
        if (task != null) task.cancel();
    }
} 