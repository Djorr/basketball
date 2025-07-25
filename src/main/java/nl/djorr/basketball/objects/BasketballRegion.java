package nl.djorr.basketball.objects;

import nl.djorr.basketball.BasketballPlugin;
import nl.djorr.basketball.utils.BasketballTextureUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldedit.Vector;
import nl.djorr.basketball.utils.ItemUtil;
import nl.djorr.basketball.utils.BasketballAnimation;
import org.bukkit.ChatColor;

/**
 * Represents a basketball region/court
 * 
 * @author Djorr
 */
public class BasketballRegion {
    
    private final String regionName;
    private final Location center;
    private Location leftHoop;
    private Location rightHoop;
    private Location leftBackboard;
    private Location rightBackboard;
    private final Location spawnLocation;
    private final Set<Player> playersInRegion;
    private Basketball currentBasketball;
    private Player basketballOwner; // Track who threw the basketball
    private boolean isAnimating = false;
    private int[] cachedRegionBounds = null;
    private long lastBoundsCheck = 0;
    private final Map<Player, Integer> playerScores; // Track scores per player
    private boolean gameWon = false; // Track if someone has won
    private final Map<Player, Integer> playerWins; // Track wins per player
    private final Map<UUID, Integer> playerWinsByUUID; // Track wins by UUID for persistence
    
    /**
     * Constructor for BasketballRegion
     * 
     * @param regionName The name of the region
     * @param center The center location of the region
     * @param spawnLocation The basketball spawn location
     */
    public BasketballRegion(String regionName, Location center, Location spawnLocation) {
        this.regionName = regionName;
        this.center = center;
        this.spawnLocation = spawnLocation;
        this.playersInRegion = new HashSet<>();
        this.currentBasketball = null;
        this.playerScores = new java.util.HashMap<>();
        this.playerWins = new java.util.HashMap<>();
        this.playerWinsByUUID = new java.util.HashMap<>();
        
        // Automatically find hoops and backboards
        findHoopsAndBackboards();
    }
    
    /**
     * Constructor for BasketballRegion (legacy support)
     * 
     * @param regionName The name of the region
     * @param center The center location of the region
     * @param leftHoop The left hoop location
     * @param rightHoop The right hoop location
     * @param leftBackboard The left backboard location
     * @param rightBackboard The right backboard location
     * @param spawnLocation The basketball spawn location
     */
    public BasketballRegion(String regionName, Location center, Location leftHoop, Location rightHoop,
                          Location leftBackboard, Location rightBackboard, Location spawnLocation) {
        this.regionName = regionName;
        this.center = center;
        this.leftHoop = leftHoop;
        this.rightHoop = rightHoop;
        this.leftBackboard = leftBackboard;
        this.rightBackboard = rightBackboard;
        this.spawnLocation = spawnLocation;
        this.playersInRegion = new HashSet<>();
        this.currentBasketball = null;
        this.playerScores = new java.util.HashMap<>();
        this.playerWins = new java.util.HashMap<>();
        this.playerWinsByUUID = new java.util.HashMap<>();
    }
    
    /**
     * Get the region name
     * 
     * @return The region name
     */
    public String getRegionName() {
        return regionName;
    }
    
    /**
     * Get the center location
     * 
     * @return The center location
     */
    public Location getCenter() {
        return center;
    }
    
    /**
     * Get the left hoop location
     * 
     * @return The left hoop location
     */
    public Location getLeftHoop() {
        return leftHoop;
    }
    
    /**
     * Set the left hoop location
     * 
     * @param leftHoop The new left hoop location
     */
    public void setLeftHoop(Location leftHoop) {
        this.leftHoop = leftHoop;
    }
    
    /**
     * Get the right hoop location
     * 
     * @return The right hoop location
     */
    public Location getRightHoop() {
        return rightHoop;
    }
    
    /**
     * Set the right hoop location
     * 
     * @param rightHoop The new right hoop location
     */
    public void setRightHoop(Location rightHoop) {
        this.rightHoop = rightHoop;
    }
    
    /**
     * Get the left backboard location
     * 
     * @return The left backboard location
     */
    public Location getLeftBackboard() {
        return leftBackboard;
    }
    
    /**
     * Set the left backboard location
     * 
     * @param leftBackboard The new left backboard location
     */
    public void setLeftBackboard(Location leftBackboard) {
        this.leftBackboard = leftBackboard;
    }
    
    /**
     * Get the right backboard location
     * 
     * @return The right backboard location
     */
    public Location getRightBackboard() {
        return rightBackboard;
    }
    
    /**
     * Set the right backboard location
     * 
     * @param rightBackboard The new right backboard location
     */
    public void setRightBackboard(Location rightBackboard) {
        this.rightBackboard = rightBackboard;
    }
    
    /**
     * Get the spawn location
     * 
     * @return The spawn location
     */
    public Location getSpawnLocation() {
        return spawnLocation;
    }
    
    /**
     * Get all players in the region
     * 
     * @return Set of players in the region
     */
    public Set<Player> getPlayersInRegion() {
        return playersInRegion;
    }
    
    /**
     * Add a player to the region
     * 
     * @param player The player to add
     */
    public void addPlayer(Player player) {
        playersInRegion.add(player);
        playerScores.put(player, 0); // Initialize score for new player
        
        // Debug logging
        BasketballPlugin plugin = BasketballPlugin.getInstance();
        if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Player " + player.getName() + " entered region " + regionName + 
                " (Player count: " + playersInRegion.size() + ", Current basketball: " + (currentBasketball != null) + 
                ", Has basketball in inventory: " + hasBasketballInInventory() + ")");
        }
        
        // NO AUTOMATIC BASKETBALL SPAWNING - Basketballs moeten handmatig gespawnd worden
        // of via Q drop functionaliteit
    }
    
    /**
     * Remove a player from the region
     * 
     * @param player The player to remove
     */
    public void removePlayer(Player player) {
        playersInRegion.remove(player);
        playerScores.remove(player); // Remove player's score
        
        // Debug logging
        BasketballPlugin plugin = BasketballPlugin.getInstance();
        if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Player " + player.getName() + " left region " + regionName + 
                " (Player count: " + playersInRegion.size() + ", Current basketball: " + (currentBasketball != null) + ")");
        }
        
        // Verwijder ALLE basketballen uit inventory
        int removedCount = 0;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && ItemUtil.isBasketballItem(item)) {
                player.getInventory().setItem(i, null);
                removedCount++;
            }
        }
        player.updateInventory();

        // Als dit de laatste speler is, bal uit inventory, entity despawn, auto-drop timer stoppen
        if (playersInRegion.isEmpty() && plugin != null) {
            // Verwijder ALTIJD alle basketballen uit inventory
            plugin.getBasketballManager().removeBasketballFromInventory(player);
            // Verwijder alle basketball entities in deze region
            if (currentBasketball != null) {
                plugin.getBasketballManager().removeBasketball(currentBasketball);
                setCurrentBasketball(null);
            }
            // Stop auto-drop timer
            nl.djorr.basketball.listeners.BasketballListener.instance.stopAutoDropTask(player);
            
            // Verwijder alle basketball skull blokken in de region
            removeAllBasketballBlocks();
        }
    }
    
    /**
     * Check if a player is in the region
     * 
     * @param player The player to check
     * @return True if the player is in the region
     */
    public boolean hasPlayer(Player player) {
        return playersInRegion.contains(player);
    }
    
    /**
     * Get the current basketball
     * 
     * @return The current basketball or null
     */
    public Basketball getCurrentBasketball() {
        return currentBasketball;
    }
    
    /**
     * Set the current basketball
     * 
     * @param basketball The basketball to set
     */
    public void setCurrentBasketball(Basketball basketball) {
        this.currentBasketball = basketball;
    }
    
    /**
     * Get the basketball owner (who threw it)
     * 
     * @return The basketball owner or null
     */
    public Player getBasketballOwner() {
        return basketballOwner;
    }
    
    /**
     * Set the basketball owner
     * 
     * @param owner The player who threw the basketball
     */
    public void setBasketballOwner(Player owner) {
        this.basketballOwner = owner;
    }
    
    /**
     * Check if a player is the basketball owner
     * 
     * @param player The player to check
     * @return True if the player is the owner
     */
    public boolean isBasketballOwner(Player player) {
        return basketballOwner != null && basketballOwner.equals(player);
    }
    
    /**
     * Add a point to a player's score
     * 
     * @param player The player who scored
     * @return True if the player won (reached 10 points)
     */
    public boolean addScore(Player player) {
        if (gameWon) {
            return false; // Game already won
        }
        
        int currentScore = playerScores.getOrDefault(player, 0);
        int newScore = currentScore + 1;
        playerScores.put(player, newScore);
        
        BasketballPlugin plugin = BasketballPlugin.getInstance();
        if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Player " + player.getName() + " scored! New score: " + newScore + "/10");
        }
        
        // Check if player won
        if (newScore >= 10) {
            gameWon = true;
            
            // Update wins/losses
            int currentWins = playerWins.getOrDefault(player, 0);
            playerWins.put(player, currentWins + 1);
            
            if (plugin != null) {
                // Send title to winner
                sendWinnerTitle(player);
                
                // Announce winner in chat
                for (Player p : playersInRegion) {
                    p.sendMessage("§6§l╔══════════════════════════════════════╗");
                    p.sendMessage("§6§l║" + "§e§l🏆 WINNER! 🏆" + "§6§l                    ║");
                    p.sendMessage("§6§l║" + "§f" + player.getName() + " §ehas won!" + "§6§l                    ║");
                    p.sendMessage("§6§l║" + "§aScore: §f" + newScore + "§a/10" + "§6§l                    ║");
                    p.sendMessage("§6§l╚══════════════════════════════════════╝");
                }
                
                // Reset game after 5 seconds
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    resetGame();
                    for (Player p : playersInRegion) {
                        p.sendMessage("§a§lGame reset! New game starting...");
                    }
                }, 100L); // 5 seconds
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * Add score to a player
     * 
     * @param player The player
     * @param points The points to add
     */
    public void addScore(Player player, int points) {
        int currentScore = playerScores.getOrDefault(player, 0);
        int newScore = currentScore + points;
        playerScores.put(player, newScore);
        
        // Notify all players in the region about the score
        notifyScoring(player, points, newScore);
        
        // Check for game win
        if (newScore >= 10 && !gameWon) {
            gameWon = true;
            announceWinner(player);
            resetGame();
        }
        
        // Update holograms automatically
        BasketballPlugin plugin = BasketballPlugin.getInstance();
        if (plugin != null) {
            plugin.getHologramManager().updateAllRegionHolograms();
        }
    }
    
    /**
     * Notify all players in the region about a scoring event
     * 
     * @param scorer The player who scored
     * @param points The points scored
     * @param totalScore The total score after scoring
     */
    private void notifyScoring(Player scorer, int points, int totalScore) {
        String chatMessage = ChatColor.GREEN + "🏀 " + ChatColor.YELLOW + scorer.getName() + 
            ChatColor.GREEN + " heeft gescoord! (" + ChatColor.GOLD + totalScore + 
            ChatColor.GREEN + " punten)";
        
        String titleMessage = ChatColor.GREEN + "🏀 SCORE! 🏀";
        String subtitleMessage = ChatColor.YELLOW + scorer.getName() + ChatColor.GREEN + " - " + 
            ChatColor.GOLD + totalScore + ChatColor.GREEN + " punten";
        
        // Send to all players in the region
        for (Player player : playersInRegion) {
            // Chat message
            player.sendMessage(chatMessage);
            
            // Title and subtitle
            sendTitle(player, titleMessage, subtitleMessage);
        }
        
        // Also send to the scorer specifically
        if (!playersInRegion.contains(scorer)) {
            scorer.sendMessage(chatMessage);
            sendTitle(scorer, titleMessage, subtitleMessage);
        }
    }
    
    /**
     * Send a title to a player using reflection (compatible with 1.12.2)
     * 
     * @param player The player to send the title to
     * @param title The title text
     * @param subtitle The subtitle text
     */
    private void sendTitle(Player player, String title, String subtitle) {
        try {
            // Get the player's connection
            Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = craftPlayer.getClass().getField("playerConnection").get(craftPlayer);
            
            // Create title packets
            Object[] packets = createTitlePacket(title, subtitle);
            
            // Send both packets
            for (Object packet : packets) {
                connection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(connection, packet);
            }
            
        } catch (Exception e) {
            // Fallback to chat message if title fails
            player.sendMessage(title);
            if (!subtitle.isEmpty()) {
                player.sendMessage(subtitle);
            }
        }
    }
    
    /**
     * Create title packets using reflection
     * 
     * @param title The title text
     * @param subtitle The subtitle text
     * @return Array of title packets
     */
    private Object[] createTitlePacket(String title, String subtitle) throws Exception {
        // Get NMS classes
        Class<?> packetClass = getNMSClass("PacketPlayOutTitle");
        Class<?> enumClass = getNMSClass("PacketPlayOutTitle$EnumTitleAction");
        Class<?> chatComponentClass = getNMSClass("IChatBaseComponent");
        
        // Create title component
        Object titleComponent = chatComponentClass.getMethod("a", String.class).invoke(null, title);
        Object subtitleComponent = chatComponentClass.getMethod("a", String.class).invoke(null, subtitle);
        
        // Create title packet
        Object titlePacket = packetClass.getConstructor(enumClass, chatComponentClass).newInstance(
            enumClass.getField("TITLE").get(null), titleComponent);
        
        // Create subtitle packet
        Object subtitlePacket = packetClass.getConstructor(enumClass, chatComponentClass).newInstance(
            enumClass.getField("SUBTITLE").get(null), subtitleComponent);
        
        // Return both packets
        return new Object[]{titlePacket, subtitlePacket};
    }
    
    /**
     * Announce the winner
     * 
     * @param winner The winning player
     */
    private void announceWinner(Player winner) {
        // Add win to player
        int currentWins = playerWins.getOrDefault(winner, 0);
        playerWins.put(winner, currentWins + 1);
        
        // Broadcast winner message
        String message = ChatColor.GOLD + "🏆 " + ChatColor.YELLOW + winner.getName() + 
            ChatColor.GREEN + " heeft gewonnen met 10 punten! 🏆";
        winner.getWorld().getPlayers().forEach(p -> p.sendMessage(message));
        
        // Send title to winner
        sendWinnerTitle(winner);
        
        // Update holograms automatically
        BasketballPlugin plugin = BasketballPlugin.getInstance();
        if (plugin != null) {
            plugin.getHologramManager().updateAllRegionHolograms();
        }
    }
    
    /**
     * Get a player's score
     * 
     * @param player The player
     * @return The player's score
     */
    public int getPlayerScore(Player player) {
        return playerScores.getOrDefault(player, 0);
    }
    
    /**
     * Get all player scores
     * 
     * @return Map of player scores
     */
    public Map<Player, Integer> getPlayerScores() {
        return new java.util.HashMap<>(playerScores);
    }
    
    /**
     * Check if the game has been won
     * 
     * @return True if someone has won
     */
    public boolean isGameWon() {
        return gameWon;
    }
    
    /**
     * Reset the game (clear scores and game won status)
     */
    public void resetGame() {
        playerScores.clear();
        gameWon = false;
        
        // Initialize scores for current players
        for (Player player : playersInRegion) {
            playerScores.put(player, 0);
        }
        
        BasketballPlugin plugin = BasketballPlugin.getInstance();
        if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Basketball game reset for region " + regionName);
        }
    }
    
    /**
     * Spawn a basketball in the region
     */
    public void spawnBasketball() {
        BasketballPlugin plugin = BasketballPlugin.getInstance();
        
        if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("spawnBasketball() called - Current: " + (currentBasketball != null) + 
                ", Animating: " + isAnimating + ", Has in inventory: " + hasBasketballInInventory() + 
                ", Player count: " + playersInRegion.size());
        }
        
        // Check if there's already a basketball entity or if any player has one in inventory
        if (currentBasketball != null || isAnimating || hasBasketballInInventory()) {
            if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Cannot spawn basketball - Current: " + (currentBasketball != null) + 
                    ", Animating: " + isAnimating + ", Has in inventory: " + hasBasketballInInventory());
            }
            return;
        }
        
        if (plugin != null) {
            // Use spawn location instead of center (bedrock)
            Location basketballLocation = spawnLocation.clone();
            basketballLocation.setY(basketballLocation.getBlockY() + 1); // Spawn 1 block above ground
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Spawning basketball at spawn location: " + basketballLocation.getBlockX() + "," + 
                    basketballLocation.getBlockY() + "," + basketballLocation.getBlockZ());
            }
            
            // Set animating flag
            isAnimating = true;
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Starting basketball spawn animation...");
            }
            
            // Use animation to spawn basketball
            BasketballAnimation animation = new BasketballAnimation(plugin);
            animation.animateBasketballSpawn(basketballLocation, this, () -> {
                // Create the basketball as a block on the ground after animation
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Animation complete, creating basketball block...");
                }
                
                // Plaats de bal als blok op de grond (spawn location)
                Location groundLocation = spawnLocation.clone();
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
                            if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                                plugin.getLogger().warning("Could not apply material skull texture: " + e.getMessage());
                            }
                        }
                        
                        if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("Basketball skull block spawned with GameProfile texture");
                        }
                    }
                    
                    // Particle effect
                    groundLocation.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, groundLocation.clone().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
                    groundLocation.getWorld().spawnParticle(org.bukkit.Particle.CRIT, groundLocation.clone().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
                    // Geluid
                    groundLocation.getWorld().playSound(groundLocation, org.bukkit.Sound.ENTITY_SLIME_SQUISH, 0.8f, 1.2f);
                    
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Basketball spawned as block at " + groundLocation);
                    }
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
                                if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                                    plugin.getLogger().warning("Could not apply material skull texture: " + e.getMessage());
                                }
                            }
                            
                            if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                                plugin.getLogger().info("Basketball skull block spawned (1 block up) with GameProfile texture");
                            }
                        }
                        
                        // Particle effect
                        groundLocation.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, groundLocation.clone().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
                        groundLocation.getWorld().spawnParticle(org.bukkit.Particle.CRIT, groundLocation.clone().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
                        // Geluid
                        groundLocation.getWorld().playSound(groundLocation, org.bukkit.Sound.ENTITY_SLIME_SQUISH, 0.8f, 1.2f);
                        
                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("Basketball spawned as block (1 block up) at " + groundLocation);
                        }
                    }
                }
                
                isAnimating = false;
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Basketball spawned successfully as block");
                }
            });
        }
    }
    
    /**
     * Remove the basketball from the region
     */
    public void removeBasketball() {
        if (currentBasketball != null) {
            BasketballPlugin plugin = BasketballPlugin.getInstance();
            if (plugin != null) {
                plugin.getBasketballManager().removeBasketball(currentBasketball);
            }
            currentBasketball = null;
        }
        
        // Stop any ongoing animations
        isAnimating = false;
    }
    
    /**
     * Check if the region is currently animating
     * 
     * @return True if animating
     */
    public boolean isAnimating() {
        return isAnimating;
    }
    
    /**
     * Stop any ongoing animations
     */
    public void stopAnimations() {
        isAnimating = false;
    }
    
    /**
     * Check if any player in the region has a basketball in their inventory
     * 
     * @return True if any player has a basketball
     */
    public boolean hasBasketballInInventory() {
        BasketballPlugin plugin = BasketballPlugin.getInstance();
        
        for (Player player : playersInRegion) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && ItemUtil.isBasketballItem(item)) {
                    if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Found basketball in " + player.getName() + "'s inventory");
                    }
                    return true;
                }
            }
        }
        
        if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("No basketball found in any player's inventory");
        }
        return false;
    }
    
    /**
     * Remove ALL basketball items from ALL players in the region
     */
    public void removeAllBasketballsFromInventory() {
        BasketballPlugin plugin = BasketballPlugin.getInstance();
        
        for (Player player : playersInRegion) {
            int removedCount = 0;
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && ItemUtil.isBasketballItem(item)) {
                    player.getInventory().setItem(i, null);
                    removedCount++;
                }
            }
            
            if (removedCount > 0) {
                player.updateInventory();
                if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Removed " + removedCount + " basketball(s) from " + player.getName() + "'s inventory");
                }
            }
        }
    }
    
    /**
     * Check if a location is near a hoop
     * 
     * @param location The location to check
     * @return True if near a hoop
     */
    public boolean isNearHoop(Location location) {
        double detectionRadius = BasketballPlugin.getInstance().getConfigManager().getHoopDetectionRadius();
        
        return location.distance(leftHoop) <= detectionRadius || 
               location.distance(rightHoop) <= detectionRadius;
    }
    
    /**
     * Get the nearest hoop location
     * 
     * @param location The location to check from
     * @return The nearest hoop location
     */
    public Location getNearestHoop(Location location) {
        double leftDistance = location.distance(leftHoop);
        double rightDistance = location.distance(rightHoop);
        
        return leftDistance <= rightDistance ? leftHoop : rightHoop;
    }
    
    /**
     * Check if a location is near a backboard
     * 
     * @param location The location to check
     * @return True if near a backboard
     */
    public boolean isNearBackboard(Location location) {
        double detectionRadius = BasketballPlugin.getInstance().getConfigManager().getHoopDetectionRadius();
        
        return location.distance(leftBackboard) <= detectionRadius || 
               location.distance(rightBackboard) <= detectionRadius;
    }
    
    /**
     * Get the number of players in the region
     * 
     * @return The number of players
     */
    public int getPlayerCount() {
        return playersInRegion.size();
    }
    
    /**
     * Get the bounds of the WorldGuard region
     * 
     * @return Array with [minX, maxX, minY, maxY, minZ, maxZ] or null
     */
    public int[] getRegionBounds() {
        // Cache bounds for 5 seconds to reduce lag
        long currentTime = System.currentTimeMillis();
        if (cachedRegionBounds != null && (currentTime - lastBoundsCheck) < 5000) {
            return cachedRegionBounds;
        }
        
        try {
            WorldGuardPlugin worldGuard = WorldGuardPlugin.inst();
            Object regionManager = worldGuard.getRegionManager(center.getWorld());
            ProtectedRegion region = (ProtectedRegion) regionManager.getClass().getMethod("getRegion", String.class).invoke(regionManager, regionName);
            if (region == null) {
                // Return a default boundary around center to prevent errors
                cachedRegionBounds = new int[]{
                    center.getBlockX() - 20, center.getBlockX() + 20,
                    center.getBlockY() - 5, center.getBlockY() + 10,
                    center.getBlockZ() - 20, center.getBlockZ() + 20
                };
                lastBoundsCheck = currentTime;
                return cachedRegionBounds;
            }
            Vector minPoint = region.getMinimumPoint();
            Vector maxPoint = region.getMaximumPoint();
            int minX = minPoint.getBlockX();
            int minY = minPoint.getBlockY();
            int minZ = minPoint.getBlockZ();
            int maxX = maxPoint.getBlockX();
            int maxY = maxPoint.getBlockY();
            int maxZ = maxPoint.getBlockZ();
            cachedRegionBounds = new int[]{minX, maxX, minY, maxY, minZ, maxZ};
            lastBoundsCheck = currentTime;
            return cachedRegionBounds;
        } catch (Exception e) {
            // Return a default boundary around center to prevent errors
            cachedRegionBounds = new int[]{
                center.getBlockX() - 20, center.getBlockX() + 20,
                center.getBlockY() - 5, center.getBlockY() + 10,
                center.getBlockZ() - 20, center.getBlockZ() + 20
            };
            lastBoundsCheck = currentTime;
            return cachedRegionBounds;
        }
    }
    
    /**
     * Fallback method to find hoops and backboards around center
     */
    private void findHoopsAndBackboardsFallback() {
        // Search for hoppers (hoops) in a 20 block radius around center
        int searchRadius = 20;
        java.util.List<Location> hoppers = new java.util.ArrayList<>();
        
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -5; y <= 10; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    Location loc = center.clone().add(x, y, z);
                    if (loc.getBlock().getType() == org.bukkit.Material.HOPPER) {
                        hoppers.add(loc);
                    }
                }
            }
        }
        
        // Find backboards (stained glass) near hoppers
        java.util.List<Location> backboards = new java.util.ArrayList<>();
        for (Location hopper : hoppers) {
            // Search for backboards near hoppers
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        Location loc = hopper.clone().add(x, y, z);
                        if (loc.getBlock().getType() == org.bukkit.Material.STAINED_GLASS) {
                            backboards.add(loc);
                        }
                    }
                }
            }
        }
        
        // Set hoops and backboards
        if (hoppers.size() >= 2) {
            this.leftHoop = hoppers.get(0);
            this.rightHoop = hoppers.get(1);
        } else if (hoppers.size() == 1) {
            this.leftHoop = hoppers.get(0);
            this.rightHoop = hoppers.get(0);
        } else {
            // Default positions if no hoppers found
            this.leftHoop = center.clone().add(-10, 3, 0);
            this.rightHoop = center.clone().add(10, 3, 0);
        }
        
        if (backboards.size() >= 2) {
            this.leftBackboard = backboards.get(0);
            this.rightBackboard = backboards.get(1);
        } else if (backboards.size() == 1) {
            this.leftBackboard = backboards.get(0);
            this.rightBackboard = backboards.get(0);
        } else {
            // Default positions if no backboards found
            this.leftBackboard = center.clone().add(-10, 3, -1);
            this.rightBackboard = center.clone().add(10, 3, -1);
        }
    }
    
    /**
     * Automatically find hoops and backboards in the region
     */
    private void findHoopsAndBackboards() {
        // Get region bounds to search within
        int[] bounds = getRegionBounds();
        if (bounds == null) {
            // Fallback to center-based search
            findHoopsAndBackboardsFallback();
            return;
        }
        
        int minX = bounds[0], maxX = bounds[1];
        int minY = bounds[2], maxY = bounds[3];
        int minZ = bounds[4], maxZ = bounds[5];
        
        // Search for hoppers (hoops) within region bounds
        java.util.List<Location> hoppers = new java.util.ArrayList<>();
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new org.bukkit.Location(center.getWorld(), x, y, z);
                    if (loc.getBlock().getType() == org.bukkit.Material.HOPPER) {
                        hoppers.add(loc);
                    }
                }
            }
        }
        
        // Find backboards (stained glass) near hoppers within region bounds
        java.util.List<Location> backboards = new java.util.ArrayList<>();
        for (Location hopper : hoppers) {
            // Search for backboards near hoppers
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        Location loc = hopper.clone().add(x, y, z);
                        // Check if location is within region bounds
                        if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                            loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
                            loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {
                            if (loc.getBlock().getType() == org.bukkit.Material.STAINED_GLASS) {
                                backboards.add(loc);
                            }
                        }
                    }
                }
            }
        }
        
        // Set hoops and backboards
        if (hoppers.size() >= 2) {
            this.leftHoop = hoppers.get(0);
            this.rightHoop = hoppers.get(1);
        } else if (hoppers.size() == 1) {
            this.leftHoop = hoppers.get(0);
            this.rightHoop = hoppers.get(0);
        } else {
            // Default positions if no hoppers found
            this.leftHoop = center.clone().add(-10, 3, 0);
            this.rightHoop = center.clone().add(10, 3, 0);
        }
        
        if (backboards.size() >= 2) {
            this.leftBackboard = backboards.get(0);
            this.rightBackboard = backboards.get(1);
        } else if (backboards.size() == 1) {
            this.leftBackboard = backboards.get(0);
            this.rightBackboard = backboards.get(0);
        } else {
            // Default positions if no backboards found
            this.leftBackboard = center.clone().add(-10, 3, -1);
            this.rightBackboard = center.clone().add(10, 3, -1);
        }
    }

    /**
     * Check if a location is within this basketball region
     * 
     * @param location The location to check
     * @return True if the location is within the region
     */
    public boolean containsLocation(Location location) {
        if (location == null || !location.getWorld().equals(center.getWorld())) {
            return false;
        }
        
        // Get region bounds
        int[] bounds = getRegionBounds();
        if (bounds == null) {
            return false;
        }
        
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        return x >= bounds[0] && x <= bounds[1] &&
               y >= bounds[2] && y <= bounds[3] &&
               z >= bounds[4] && z <= bounds[5];
    }

    /**
     * Remove all basketball skull blocks in this region
     */
    public void removeAllBasketballBlocks() {
        BasketballPlugin plugin = BasketballPlugin.getInstance();
        if (plugin == null) return;
        
        int removedBlocks = 0;
        int[] bounds = getRegionBounds();
        
        if (bounds != null) {
            for (int x = bounds[0]; x <= bounds[1]; x++) {
                for (int y = bounds[2]; y <= bounds[3]; y++) {
                    for (int z = bounds[4]; z <= bounds[5]; z++) {
                        Location loc = new Location(center.getWorld(), x, y, z);
                        if (loc.getBlock().getType() == org.bukkit.Material.SKULL) {
                            // Check if it's a basketball skull using BasketballTextureUtil
                            org.bukkit.block.BlockState state = loc.getBlock().getState();
                            if (state instanceof org.bukkit.block.Skull) {
                                org.bukkit.block.Skull skull = (org.bukkit.block.Skull) state;
                                if (BasketballTextureUtil.isBasketballSkull(skull)) {
                                    // This is a basketball skull block, remove it
                                    loc.getBlock().setType(org.bukkit.Material.AIR);
                                    removedBlocks++;
                                    
                                    if (plugin.getConfigManager().isDebugEnabled()) {
                                        plugin.getLogger().info("Removed basketball skull block at " + loc + " (last player left region)");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Removed " + removedBlocks + " basketball skull blocks from region " + regionName + " (last player left)");
        }
    }

    /**
     * Send winner title to player
     * 
     * @param player The winning player
     */
    private void sendWinnerTitle(Player player) {
        try {
            // Send title packet
            Object timesPacket = createTimesPacket(10, 60, 10); // 0.5s fade in, 3s stay, 0.5s fade out
            Object titlePacket = createTitlePacket("§e§l🏆 WINNER! 🏆", "");
            Object subtitlePacket = createSubtitlePacket("§f" + player.getName() + " §ehas won!");
            
            sendPacket(player, timesPacket);
            sendPacket(player, titlePacket);
            sendPacket(player, subtitlePacket);
            
        } catch (Exception e) {
            BasketballPlugin plugin = BasketballPlugin.getInstance();
            if (plugin != null && plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("Could not send winner title: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get player wins
     * 
     * @param player The player
     * @return Number of wins
     */
    public int getPlayerWins(Player player) {
        return playerWins.getOrDefault(player, 0);
    }
    
    /**
     * Get player losses
     * 
     * @param player The player
     * @return Number of losses
     */
    public int getPlayerLosses(Player player) {
        return 0; // No player losses tracking
    }
    
    /**
     * Get all player wins
     * 
     * @return Map of player wins
     */
    public Map<Player, Integer> getPlayerWins() {
        return new java.util.HashMap<>(playerWins);
    }
    
    /**
     * Get all player losses
     * 
     * @return Map of player losses
     */
    public Map<Player, Integer> getPlayerLosses() {
        return new java.util.HashMap<>(); // No player losses tracking
    }
    
    /**
     * Set player wins from UUID (for data loading)
     * 
     * @param playerUUID The player UUID
     * @param wins The number of wins
     */
    public void setPlayerWinsFromUUID(UUID playerUUID, int wins) {
        playerWinsByUUID.put(playerUUID, wins);
    }
    
    /**
     * Update player wins from UUID when player joins
     * 
     * @param player The player who joined
     */
    public void updatePlayerWinsFromUUID(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (playerWinsByUUID.containsKey(playerUUID)) {
            int wins = playerWinsByUUID.get(playerUUID);
            playerWins.put(player, wins);
            playerWinsByUUID.remove(playerUUID);
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
        BasketballPlugin plugin = BasketballPlugin.getInstance();
        if (plugin != null) {
            return plugin.getServer().getClass().getPackage().getName().split("\\.")[3];
        }
        return "v1_12_R1"; // Fallback
    }
} 