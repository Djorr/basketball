package nl.djorr.basketball.utils;

import nl.djorr.basketball.BasketballPlugin;
import nl.djorr.basketball.objects.Basketball;
import nl.djorr.basketball.objects.BasketballRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import nl.djorr.basketball.utils.ItemBuilder;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for basketball animations
 * 
 * @author Djorr
 */
public class BasketballAnimation {
    
    private final BasketballPlugin plugin;
    
    public BasketballAnimation(BasketballPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Animate basketball spawning with falling effect
     * 
     * @param finalLocation The final location where the basketball should land
     * @param region The basketball region (for player targeting)
     * @param onComplete Callback when animation is complete
     */
    public void animateBasketballSpawn(Location finalLocation, BasketballRegion region, Runnable onComplete) {
        // Start position (3 blocks above final location)
        Location startLocation = finalLocation.clone().add(0, 3, 0);
        
        // Create temporary basketball entity for animation
        ArmorStand tempBasketball = startLocation.getWorld().spawn(startLocation, ArmorStand.class);
        tempBasketball.setVisible(false);
        tempBasketball.setGravity(false);
        tempBasketball.setSmall(true);
        tempBasketball.setMarker(true);
        
        // Set basketball head using BasketballTextureUtil
        ItemStack basketballHead = BasketballTextureUtil.createBasketballSkullItem(plugin.getConfigManager().getBasketballName());
        tempBasketball.setHelmet(basketballHead);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Created basketball animation with BasketballTextureUtil texture");
        }
        
        // Animation variables
        final double fallDistance = 3.0;
        final int totalTicks = 40; // 2 seconds
        final double bounceHeight = 0.8;
        
        new BukkitRunnable() {
            private int ticks = 0;
            private boolean hasBounced = false;
            private double currentY = startLocation.getY();
            private double velocityY = 0;
            
            @Override
            public void run() {
                ticks++;
                
                // Calculate falling physics
                if (!hasBounced) {
                    // Falling phase
                    velocityY -= 0.1; // Gravity
                    currentY += velocityY;
                    
                    // Check if hit ground
                    if (currentY <= finalLocation.getY()) {
                        currentY = finalLocation.getY();
                        hasBounced = true;
                        velocityY = bounceHeight; // Bounce up
                        
                        // Spawn particles on impact
                        spawnImpactParticles(finalLocation, region);
                        spawnHologramEffect(finalLocation, region, "§6§lBOUNCE!");
                    }
                } else {
                    // Bounce phase
                    velocityY -= 0.1; // Gravity
                    currentY += velocityY;
                    
                    // Check if bounce is complete
                    if (currentY <= finalLocation.getY()) {
                        currentY = finalLocation.getY();
                        
                        // Animation complete
                        tempBasketball.remove();
                        
                        // Spawn final particles
                        spawnLandingParticles(finalLocation, region);
                        spawnHologramEffect(finalLocation, region, "§a§lREADY!");
                        
                        // Call completion callback
                        if (onComplete != null) {
                            onComplete.run();
                        }
                        
                        this.cancel();
                        return;
                    }
                }
                
                // Update basketball position
                Location newLocation = finalLocation.clone();
                newLocation.setY(currentY);
                tempBasketball.teleport(newLocation);
                
                // Spawn falling particles
                if (!hasBounced) {
                    spawnFallingParticles(newLocation, region);
                }
                
                // Cancel if animation takes too long
                if (ticks >= totalTicks) {
                    tempBasketball.remove();
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
    
    /**
     * Spawn falling particles (only to players in region)
     */
    private void spawnFallingParticles(Location location, BasketballRegion region) {
        for (Player player : region.getPlayersInRegion()) {
            player.spawnParticle(
                Particle.SMOKE_NORMAL,
                location.clone().add(0, 0.5, 0),
                3, 0.1, 0.1, 0.1, 0.01
            );
        }
    }
    
    /**
     * Spawn impact particles (only to players in region)
     */
    private void spawnImpactParticles(Location location, BasketballRegion region) {
        for (Player player : region.getPlayersInRegion()) {
            // Impact particles
            player.spawnParticle(
                Particle.EXPLOSION_NORMAL,
                location.clone().add(0, 0.5, 0),
                5, 0.3, 0.1, 0.3, 0.1
            );
            
            // Dust particles
            player.spawnParticle(
                Particle.SMOKE_NORMAL,
                location.clone().add(0, 0.5, 0),
                10, 0.5, 0.1, 0.5, 0.1
            );
        }
    }
    
    /**
     * Spawn landing particles (only to players in region)
     */
    private void spawnLandingParticles(Location location, BasketballRegion region) {
        for (Player player : region.getPlayersInRegion()) {
            // Landing particles
            player.spawnParticle(
                Particle.VILLAGER_HAPPY,
                location.clone().add(0, 1, 0),
                8, 0.3, 0.3, 0.3, 0.1
            );
        }
    }
    
    /**
     * Spawn hologram effect (only to players in region)
     */
    private void spawnHologramEffect(Location location, BasketballRegion region, String text) {
        try {
            // Try to use DecentHolograms if available
            Class.forName("eu.decentsoftware.holograms.api.HologramAPI");
            
            // Create temporary hologram
            Location holoLocation = location.clone().add(0, 2, 0);
            
            // Note: This would require DecentHolograms API integration
            // For now, we'll just use particles to simulate the effect
            for (Player player : region.getPlayersInRegion()) {
                player.spawnParticle(
                    Particle.FIREWORKS_SPARK,
                    holoLocation,
                    15, 0.2, 0.2, 0.2, 0.1
                );
            }
            
        } catch (ClassNotFoundException e) {
            // DecentHolograms not available, use particles only
            for (Player player : region.getPlayersInRegion()) {
                player.spawnParticle(
                    Particle.FIREWORKS_SPARK,
                    location.clone().add(0, 2, 0),
                    15, 0.2, 0.2, 0.2, 0.1
                );
            }
        }
    }
    
    /**
     * Animate basketball scoring with particle effects (only to players in region)
     */
    public void animateBasketballScore(Location location, BasketballRegion region) {
        for (Player player : region.getPlayersInRegion()) {
            // Score particles
            player.spawnParticle(
                Particle.FIREWORKS_SPARK,
                location.clone().add(0, 1, 0),
                20, 0.5, 0.5, 0.5, 0.2
            );
        }
        
        // Score hologram
        spawnHologramEffect(location, region, "§6§lSCORE!");
        
        // Additional effects
        new BukkitRunnable() {
            private int ticks = 0;
            
            @Override
            public void run() {
                ticks++;
                
                // Spawn particles in a circle
                for (int i = 0; i < 8; i++) {
                    double angle = (i * Math.PI * 2) / 8;
                    double x = Math.cos(angle) * 0.5;
                    double z = Math.sin(angle) * 0.5;
                    
                    for (Player player : region.getPlayersInRegion()) {
                        player.spawnParticle(
                            Particle.VILLAGER_HAPPY,
                            location.clone().add(x, 1, z),
                            1, 0, 0, 0, 0
                        );
                    }
                }
                
                if (ticks >= 20) { // 1 second
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
} 