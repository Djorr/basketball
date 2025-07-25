package nl.djorr.basketball.utils;

import nl.djorr.basketball.BasketballPlugin;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * Utility class for basketball texture implementation
 * 
 * @author Djorr
 */
public class BasketballTextureUtil {
    
    private static final UUID BASKETBALL_UUID = UUID.fromString("9a869760-a4ae-49ac-9598-e136ce74ba73");
    private static final String BASKETBALL_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWRmODQ3MTVhNjRkYzQ1NTg2ZjdhNjA3OWY4ZTQ5YTk0NzdjMGZlOTY1ODliNGNmZDcxY2JhMzIyNTRhYzgifX19";
    
    /**
     * Create a basketball skull item with the correct texture using NBT structure
     * 
     * @param displayName The display name for the basketball
     * @return The basketball skull item
     */
    public static ItemStack createBasketballSkullItem(String displayName) {
        // Maak een itemstack van een player skull
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();

        // Stel de displaynaam in met color translate
        skullMeta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));

        // Apply basketball texture using NBT structure
        applyBasketballTextureNBT(skullMeta);

        skull.setItemMeta(skullMeta);
        return skull;
    }
    
    /**
     * Apply basketball texture to a SkullMeta using NBT structure
     * 
     * @param skullMeta The SkullMeta to apply texture to
     */
    public static void applyBasketballTextureNBT(SkullMeta skullMeta) {
        try {
            // Create GameProfile with the exact NBT structure
            com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(BASKETBALL_UUID, null);
            
            // Add texture property exactly as in the command
            com.mojang.authlib.properties.Property textureProperty = new com.mojang.authlib.properties.Property("textures", BASKETBALL_TEXTURE);
            profile.getProperties().put("textures", textureProperty);

            // Use reflection to inject the GameProfile into the skull
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
            
        } catch (Exception e) {
            BasketballPlugin plugin = BasketballPlugin.getInstance();
            if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Could not apply basketball texture NBT: " + e.getMessage());
            }
        }
    }
    
    /**
     * Apply basketball texture to a block skull using NBT structure
     * 
     * @param skull The block skull to apply texture to
     */
    public static void applyBasketballTextureNBT(Skull skull) {
        try {
            // First set the skull type to PLAYER
            skull.setSkullType(org.bukkit.SkullType.PLAYER);
            
            // Create GameProfile with the exact NBT structure
            com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(BASKETBALL_UUID, null);
            
            // Add texture property exactly as in the command
            com.mojang.authlib.properties.Property textureProperty = new com.mojang.authlib.properties.Property("textures", BASKETBALL_TEXTURE);
            profile.getProperties().put("textures", textureProperty);

            // Use reflection to inject the GameProfile into the skull
            Field profileField = skull.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skull, profile);
            
            // Update the block to apply changes
            skull.update();
            
            // Debug logging
            BasketballPlugin plugin = BasketballPlugin.getInstance();
            if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Applied basketball texture to block skull - UUID: " + BASKETBALL_UUID);
            }
            
        } catch (Exception e) {
            BasketballPlugin plugin = BasketballPlugin.getInstance();
            if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Could not apply basketball texture NBT to block: " + e.getMessage());
            }
        }
    }
    
    /**
     * Apply basketball texture to a material skull using NBT structure
     * 
     * @param materialSkull The material skull to apply texture to
     */
    public static void applyBasketballTextureNBT(org.bukkit.material.Skull materialSkull) {
        try {
            // For material skulls in 1.12.2, we need to use the data value
            // Set the data to indicate it's a player skull with basketball texture
            materialSkull.setData((byte) 3); // Player skull data value
            
            // Debug logging
            BasketballPlugin plugin = BasketballPlugin.getInstance();
            if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Applied basketball texture to material skull - Data: 3 (Player skull)");
            }
            
        } catch (Exception e) {
            BasketballPlugin plugin = BasketballPlugin.getInstance();
            if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Could not apply basketball texture NBT to material skull: " + e.getMessage());
            }
        }
    }
    
    /**
     * Check if a skull has basketball texture
     * 
     * @param skullMeta The SkullMeta to check
     * @return True if it has basketball texture
     */
    public static boolean isBasketballSkull(SkullMeta skullMeta) {
        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            com.mojang.authlib.GameProfile profile = (com.mojang.authlib.GameProfile) profileField.get(skullMeta);
            
            if (profile != null && profile.getId() != null) {
                return profile.getId().equals(BASKETBALL_UUID);
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
        return false;
    }
    
    /**
     * Check if a block skull has basketball texture
     * 
     * @param skull The block skull to check
     * @return True if it has basketball texture
     */
    public static boolean isBasketballSkull(Skull skull) {
        try {
            Field profileField = skull.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            com.mojang.authlib.GameProfile profile = (com.mojang.authlib.GameProfile) profileField.get(skull);
            
            if (profile != null && profile.getId() != null) {
                return profile.getId().equals(BASKETBALL_UUID);
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
        return false;
    }
    
    /**
     * Check if a material skull has basketball texture
     * 
     * @param materialSkull The material skull to check
     * @return True if it has basketball texture
     */
    public static boolean isBasketballSkull(org.bukkit.material.Skull materialSkull) {
        try {
            // Check if the material skull is a player skull (data value 3)
            if (materialSkull.getData() == 3) {
                // For material skulls, we can't easily check the texture
                // So we'll assume any player skull in a basketball region is a basketball
                return true;
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
        return false;
    }
    
    /**
     * Get the basketball UUID
     * 
     * @return The basketball UUID
     */
    public static UUID getBasketballUUID() {
        return BASKETBALL_UUID;
    }
    
    /**
     * Get the basketball texture
     * 
     * @return The basketball texture
     */
    public static String getBasketballTexture() {
        return BASKETBALL_TEXTURE;
    }
    
    // Legacy methods for backward compatibility
    public static void applyBasketballTexture(SkullMeta skullMeta) {
        applyBasketballTextureNBT(skullMeta);
    }
    
    public static void applyBasketballTexture(Skull skull) {
        applyBasketballTextureNBT(skull);
    }
    
    public static void applyBasketballTexture(org.bukkit.material.Skull materialSkull) {
        applyBasketballTextureNBT(materialSkull);
    }
} 