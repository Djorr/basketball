package nl.djorr.basketball.utils;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import nl.djorr.basketball.BasketballPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling basketball items with NBT
 * 
 * @author Djorr
 */
public class ItemUtil {
    
    private static final String BASKETBALL_NBT_KEY = "basketball";
    private static final String BASKETBALL_NBT_VALUE = "true";
    private static final boolean BASKETBALL_NBT_BOOL_VALUE = true;
    
    /**
     * Create a basketball item with NBT
     * 
     * @param plugin The plugin instance
     * @return The basketball item
     */
    public static ItemStack createBasketballItem(BasketballPlugin plugin) {
        Material material = plugin.getConfigManager().getBasketballMaterial();
        byte data = plugin.getConfigManager().getBasketballData();
        String name = plugin.getConfigManager().getBasketballName();
        List<String> lore = plugin.getConfigManager().getBasketballLore();
        
        // Create item using ItemBuilder
        ItemBuilder builder = new ItemBuilder(material, 1)
            .setDurability(data)
            .setName(name)
            .setLore(lore);
        
        // Add NBT tag as boolean true
        ItemStack item = builder.toItemStack();
        item = NBTEditor.set(item, true, BASKETBALL_NBT_KEY);
        
        // Debug logging
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Created basketball item with NBT tag: " + BASKETBALL_NBT_KEY + " = true");
        }
        
        return item;
    }
    
    /**
     * Check if an item is a basketball using NBT
     * 
     * @param item The item to check
     * @return True if it's a basketball
     */
    public static boolean isBasketballItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        // Debug logging
        BasketballPlugin plugin = BasketballPlugin.getInstance();
        if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Checking basketball item: " + item.getType() + ":" + item.getData().getData());
        }
        
        // Check NBT first using contains method
        try {
            if (NBTEditor.contains(item, BASKETBALL_NBT_KEY)) {
                // Get as boolean (since we set it as boolean)
                boolean boolValue = NBTEditor.getBoolean(item, BASKETBALL_NBT_KEY);
                if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("NBT contains basketball key, boolean value: " + boolValue);
                }
                if (boolValue == BASKETBALL_NBT_BOOL_VALUE) {
                    return true;
                }
            } else if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("NBT does not contain basketball key");
            }
        } catch (Exception e) {
            // NBT check failed, fallback to material check
            if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("NBT check failed: " + e.getMessage());
            }
        }
        
        // Fallback to material check
        if (plugin != null) {
            boolean isMaterialMatch = item.getType() == plugin.getConfigManager().getBasketballMaterial() &&
                   item.getData().getData() == plugin.getConfigManager().getBasketballData();
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Material check result: " + isMaterialMatch);
            }
            
            return isMaterialMatch;
        }
        
        return false;
    }
    
    /**
     * Translate color codes in a string
     * 
     * @param text The text to translate
     * @return The translated text
     */
    public static String translateColors(String text) {
        if (text == null) {
            return "";
        }
        
        return text.replace("&", "§");
    }
    
    /**
     * Translate color codes in a list of strings
     * 
     * @param texts The list of texts to translate
     * @return The translated list
     */
    public static List<String> translateColors(List<String> texts) {
        List<String> translated = new ArrayList<>();
        for (String text : texts) {
            translated.add(translateColors(text));
        }
        return translated;
    }
} 