package nl.djorr.basketball.utils;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemBuilder {
    private ItemStack is;

    /**
     * Create a new ItemBuilderUtil from scratch.
     *
     * @param m The material to create the ItemBuilderUtil with.
     */
    public ItemBuilder(Material m) {
        this(m, 1);
    }

    /**
     * Create a new ItemBuilderUtil over an existing itemstack.
     *
     * @param is The itemstack to create the ItemBuilderUtil over.
     */
    public ItemBuilder(ItemStack is) {
        this.is = is;
    }

    /**
     * Create a new ItemBuilderUtil from scratch.
     *
     * @param m      The material of the item.
     * @param amount The amount of the item.
     */
    public ItemBuilder(Material m, int amount) {
        is = new ItemStack(m, amount);
    }

    /**
     * Clone the ItemBuilderUtil into a new one.
     *
     * @return The cloned instance.
     */
    public ItemBuilder clone() {
        return new ItemBuilder(is);
    }

    public ItemBuilder setNBT(String key, Object value) {
        is = NBTEditor.set(is, value, key);
        return this;
    }

    /**
     * Adds a glowing effect to the item without showing enchantments.
     */
    public ItemBuilder addGlow() {
        ItemMeta im = is.getItemMeta();
        im.addEnchant(Enchantment.LUCK, 1, true); // Voeg een enchant toe voor glow
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS); // Verberg de enchant voor spelers
        is.setItemMeta(im);
        return this;
    }

    public ItemBuilder setType(Material material) {
        is.setType(material);
        return this;
    }

    public ItemBuilder makeUnbreakable(boolean unbreakable) {
        is = NBTEditor.set(is, unbreakable ? (byte) 1 : (byte) 0, "Unbreakable");
        return this;
    }

    /**
     * Change the durability of the item.
     *
     * @param dur The durability to set it to.
     */
    public ItemBuilder setDurability(short dur) {
        is.setDurability(dur);
        return this;
    }

    /**
     * Set the displayname of the item.
     *
     * @param name The name to change it to.
     */
    public ItemBuilder setName(String name) {
        ItemMeta im = is.getItemMeta();
        im.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        is.setItemMeta(im);
        return this;
    }

    /**
     * Add an unsafe enchantment.
     *
     * @param ench  The enchantment to add.
     * @param level The level to put the enchant on.
     */
    public ItemBuilder addUnsafeEnchantment(Enchantment ench, int level) {
        is.addUnsafeEnchantment(ench, level);
        return this;
    }

    /**
     * Remove a certain enchant from the item.
     *
     * @param ench The enchantment to remove
     */
    public ItemBuilder removeEnchantment(Enchantment ench) {
        is.removeEnchantment(ench);
        return this;
    }

    /**
     * Add an enchant to the item.
     *
     * @param ench  The enchantment to add
     * @param level The level
     */
    public ItemBuilder addEnchant(Enchantment ench, int level) {
        ItemMeta im = is.getItemMeta();
        im.addEnchant(ench, level, true);
        is.setItemMeta(im);
        return this;
    }

    /**
     * Add multiple enchants at once.
     *
     * @param enchantments The enchants to add.
     */
    public ItemBuilder addEnchantments(Map<Enchantment, Integer> enchantments) {
        is.addUnsafeEnchantments(enchantments);
        return this;
    }

    /**
     * Sets infinity durability on the item by setting the durability to
     * Short.MAX_VALUE.
     */
    public ItemBuilder setInfinityDurability() {
        is.setDurability(Short.MAX_VALUE);
        return this;
    }

    /**
     * Re-sets the lore.
     *
     * @param lore The lore to set it to.
     */
    public ItemBuilder setLore(String... lore) {
        ItemMeta im = is.getItemMeta();
        List<String> colored = new ArrayList<>();
        for (String line : lore) {
            colored.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        im.setLore(colored);
        is.setItemMeta(im);
        return this;
    }

    /**
     * Re-sets the lore.
     *
     * @param lore The lore to set it to.
     */
    public ItemBuilder setLore(List<String> lore) {
        ItemMeta im = is.getItemMeta();
        List<String> colored = new ArrayList<>();
        for (String line : lore) {
            colored.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        im.setLore(colored);
        is.setItemMeta(im);
        return this;
    }

    /**
     * Re-sets the lore.
     *
     * @param lore The lore to set it to.
     */
    public ItemBuilder lore(List<String> lore) {
        ItemMeta im = is.getItemMeta();
        im.setLore(lore);
        is.setItemMeta(im);
        return this;
    }

    public ItemBuilder removeLoreLine(String line) {
        ItemMeta im = is.getItemMeta();
        List<String> lore = new ArrayList<>(im.getLore());
        if (!lore.contains(line))
            return this;
        lore.remove(line);
        im.setLore(lore);
        is.setItemMeta(im);
        return this;
    }

    /**
     * Remove a lore line.
     *
     * @param index The index of the lore line to remove.
     */
    public ItemBuilder removeLoreLine(int index) {
        ItemMeta im = is.getItemMeta();
        List<String> lore = new ArrayList<>(im.getLore());
        if (index < 0 || index > lore.size())
            return this;
        lore.remove(index);
        im.setLore(lore);
        is.setItemMeta(im);
        return this;
    }

    /**
     * Add a lore line.
     *
     * @param line The lore line to add.
     */
    public ItemBuilder addLoreLine(String line) {
        ItemMeta im = is.getItemMeta();
        List<String> lore = im.hasLore() ? im.getLore() : new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', line));
        im.setLore(lore);
        is.setItemMeta(im);
        return this;
    }

    /**
     * Add a lore line.
     *
     * @param line The lore line to add.
     * @param pos  The index of where to put it.
     */
    public ItemBuilder addLoreLine(String line, int pos) {
        ItemMeta im = is.getItemMeta();
        List<String> lore = im.hasLore() ? im.getLore() : new ArrayList<>();
        lore.add(pos, ChatColor.translateAlternateColorCodes('&', line));
        im.setLore(lore);
        is.setItemMeta(im);
        return this;
    }

    /**
     * Sets the armor color of a leather armor piece. Works only on leather armor
     * pieces.
     *
     * @param color The color to set it to.
     */
    public ItemBuilder setLeatherArmorColor(Color color) {
        try {
            LeatherArmorMeta im = (LeatherArmorMeta) is.getItemMeta();
            im.setColor(color);
            is.setItemMeta(im);
        } catch (ClassCastException ignored) {
        }
        return this;
    }

    /**
     * Set the owner of a skull.
     *
     * @param owner The owner of the desired skull.
     */
    public ItemBuilder setSkullOwner(Player owner) {
        SkullMeta im = (SkullMeta) is.getItemMeta();
        im.setOwningPlayer(owner);
        is.setItemMeta(im);
        return this;
    }

    /**
     * Set the owner of a skull.
     *
     * @param owner The owner of the desired skull.
     */
    public ItemBuilder setSkullOwner(OfflinePlayer owner) {
        SkullMeta im = (SkullMeta) is.getItemMeta();
        im.setOwningPlayer(owner);
        is.setItemMeta(im);
        return this;
    }

    /**
     * Set the owner of a skull.
     *
     * @param owner The owner of the desired skull.
     */
    public ItemBuilder setSkullOwner(String owner) {
        SkullMeta im = (SkullMeta) is.getItemMeta();
        im.setOwner(owner);
        is.setItemMeta(im);
        return this;
    }

    /**
     * Set a custom skull texture and UUID (for player heads)
     * @param uuid The UUID as string
     * @param texture The base64 texture string
     * @return this
     */
    public ItemBuilder setSkullTexture(String uuid, String texture) {
        if (is.getType() != Material.SKULL_ITEM) return this;
        try {
            SkullMeta meta = (SkullMeta) is.getItemMeta();
            // Use NBTEditor to set SkullOwner with Properties
            is = NBTEditor.set(is, uuid, "SkullOwner", "Id");
            is = NBTEditor.set(is, texture, "SkullOwner", "Properties", "textures", 0, "Value");
            is.setItemMeta(meta);
        } catch (Exception ignored) {}
        return this;
    }

    /**
     * Set the flags of an item.
     *
     * @param itemFlag The itemFlag you want to add.
     */
    public ItemBuilder setItemFlag(ItemFlag itemFlag) {
        ItemMeta im = is.getItemMeta();
        im.addItemFlags(itemFlag);
        is.setItemMeta(im);
        return this;
    }

    /**
     * Set the flags of an item.
     *
     * @param itemFlag The itemFlag you want to add.
     */
    public ItemBuilder setItemFlag(ItemFlag[] itemFlag) {
        ItemMeta im = is.getItemMeta();
        im.addItemFlags(itemFlag);
        is.setItemMeta(im);
        return this;
    }

    /**
     * Retrieves the itemstack from the ItemBuilderUtil.
     *
     * @return The itemstack created/modified by the ItemBuilderUtil instance.
     */
    public ItemStack toItemStack() {
        return is;
    }
}