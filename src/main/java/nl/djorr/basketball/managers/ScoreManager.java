package nl.djorr.basketball.managers;

import nl.djorr.basketball.BasketballPlugin;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player scores and displays them
 * 
 * @author Djorr
 */
public class ScoreManager {
    
    private final BasketballPlugin plugin;
    private final Map<UUID, Integer> playerScores;
    
    /**
     * Constructor for ScoreManager
     * 
     * @param plugin The plugin instance
     */
    public ScoreManager(BasketballPlugin plugin) {
        this.plugin = plugin;
        this.playerScores = new HashMap<>();
    }
    
    /**
     * Get a player's score
     * 
     * @param player The player
     * @return The player's score
     */
    public int getScore(Player player) {
        return playerScores.getOrDefault(player.getUniqueId(), 0);
    }
    
    /**
     * Set a player's score
     * 
     * @param player The player
     * @param score The score to set
     */
    public void setScore(Player player, int score) {
        playerScores.put(player.getUniqueId(), score);
        updateScoreDisplay(player);
    }
    
    /**
     * Add points to a player's score
     * 
     * @param player The player
     * @param points The points to add
     */
    public void addScore(Player player, int points) {
        int currentScore = getScore(player);
        int newScore = currentScore + points;
        setScore(player, newScore);
        
        // Show score message
        player.sendMessage(plugin.getConfigManager().getMessageWithPrefix("score"));
        
        // Show title
        showScoreTitle(player, points);
    }
    
    /**
     * Reset a player's score
     * 
     * @param player The player
     */
    public void resetScore(Player player) {
        playerScores.remove(player.getUniqueId());
        updateScoreDisplay(player);
    }
    
    /**
     * Update the score display for a player
     * 
     * @param player The player
     */
    public void updateScoreDisplay(Player player) {
        int score = getScore(player);
        String scoreText = "Score: " + score;
        
        // Send action bar message
        sendActionBar(player, scoreText);
    }
    
    /**
     * Show a score title to a player
     * 
     * @param player The player
     * @param points The points scored
     */
    public void showScoreTitle(Player player, int points) {
        String title = "§6§lSCORE!";
        String subtitle = "§e+" + points + " points!";
        
        int fadeIn = plugin.getConfigManager().getTitleFadeIn();
        int duration = plugin.getConfigManager().getTitleDuration();
        int fadeOut = plugin.getConfigManager().getTitleFadeOut();
        
        // Send title using reflection for 1.12.2 compatibility
        sendTitle(player, title, subtitle, fadeIn, duration, fadeOut);
    }
    
    /**
     * Send a title to a player using reflection
     * 
     * @param player The player
     * @param title The title text
     * @param subtitle The subtitle text
     * @param fadeIn Fade in ticks
     * @param duration Duration ticks
     * @param fadeOut Fade out ticks
     */
    private void sendTitle(Player player, String title, String subtitle, int fadeIn, int duration, int fadeOut) {
        try {
            // Use reflection to send title for 1.12.2 compatibility
            Object chatComponentTitle = createChatComponent(title);
            Object chatComponentSubtitle = createChatComponent(subtitle);
            
            Object packetTitle = createTitlePacket(chatComponentTitle, chatComponentSubtitle, fadeIn, duration, fadeOut);
            
            sendPacket(player, packetTitle);
        } catch (Exception e) {
            // Fallback to action bar if title fails
            sendActionBar(player, title + " " + subtitle);
        }
    }
    
    /**
     * Create a chat component using reflection
     * 
     * @param text The text
     * @return The chat component
     * @throws Exception If reflection fails
     */
    private Object createChatComponent(String text) throws Exception {
        Class<?> chatComponentClass = Class.forName("net.minecraft.server.v1_12_R1.IChatBaseComponent");
        Class<?> chatComponentTextClass = Class.forName("net.minecraft.server.v1_12_R1.ChatComponentText");
        
        Object chatComponent = chatComponentTextClass.getConstructor(String.class).newInstance(text);
        return chatComponent;
    }
    
    /**
     * Create a title packet using reflection
     * 
     * @param title The title component
     * @param subtitle The subtitle component
     * @param fadeIn Fade in ticks
     * @param duration Duration ticks
     * @param fadeOut Fade out ticks
     * @return The packet
     * @throws Exception If reflection fails
     */
    private Object createTitlePacket(Object title, Object subtitle, int fadeIn, int duration, int fadeOut) throws Exception {
        Class<?> packetClass = Class.forName("net.minecraft.server.v1_12_R1.PacketPlayOutTitle");
        Class<?> enumTitleActionClass = Class.forName("net.minecraft.server.v1_12_R1.PacketPlayOutTitle$EnumTitleAction");
        
        // Create times packet
        Object timesPacket = packetClass.getConstructor(enumTitleActionClass, int.class, int.class, int.class)
            .newInstance(enumTitleActionClass.getField("TIMES").get(null), fadeIn, duration, fadeOut);
        
        // Create title packet
        Object titlePacket = packetClass.getConstructor(enumTitleActionClass, Class.forName("net.minecraft.server.v1_12_R1.IChatBaseComponent"))
            .newInstance(enumTitleActionClass.getField("TITLE").get(null), title);
        
        // Create subtitle packet
        Object subtitlePacket = packetClass.getConstructor(enumTitleActionClass, Class.forName("net.minecraft.server.v1_12_R1.IChatBaseComponent"))
            .newInstance(enumTitleActionClass.getField("SUBTITLE").get(null), subtitle);
        
        return new Object[]{timesPacket, titlePacket, subtitlePacket};
    }
    
    /**
     * Send a packet to a player using reflection
     * 
     * @param player The player
     * @param packet The packet to send
     * @throws Exception If reflection fails
     */
    private void sendPacket(Player player, Object packet) throws Exception {
        Object[] packets = (Object[]) packet;
        for (Object p : packets) {
            Object connection = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = connection.getClass().getField("playerConnection").get(connection);
            playerConnection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server.v1_12_R1.Packet"))
                .invoke(playerConnection, p);
        }
    }
    
    /**
     * Send an action bar message to a player
     * 
     * @param player The player
     * @param message The message
     */
    public void sendActionBar(Player player, String message) {
        try {
            Object chatComponent = createChatComponent(message);
            Class<?> packetClass = Class.forName("net.minecraft.server.v1_12_R1.PacketPlayOutChat");
            Object packet = packetClass.getConstructor(Class.forName("net.minecraft.server.v1_12_R1.IChatBaseComponent"), byte.class)
                .newInstance(chatComponent, (byte) 2);
            
            sendPacket(player, packet);
        } catch (Exception e) {
            // Fallback to regular message
            player.sendMessage(message);
        }
    }
    
    /**
     * Get all player scores
     * 
     * @return Map of player UUIDs to scores
     */
    public Map<UUID, Integer> getAllScores() {
        return new HashMap<>(playerScores);
    }
    
    /**
     * Clear all scores
     */
    public void clearAllScores() {
        playerScores.clear();
    }
} 