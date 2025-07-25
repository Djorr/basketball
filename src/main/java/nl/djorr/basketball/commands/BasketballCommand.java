package nl.djorr.basketball.commands;

import nl.djorr.basketball.BasketballPlugin;
import nl.djorr.basketball.objects.BasketballRegion;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Command executor for basketball commands
 * 
 * @author Djorr
 */
public class BasketballCommand implements CommandExecutor {
    
    private final BasketballPlugin plugin;
    
    public BasketballCommand(BasketballPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "help":
                showHelp(sender);
                return true;
                
            case "region":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /basketbal region <create|delete|list> [name]");
                    return true;
                }
                handleRegionCommand(sender, args);
                return true;
                
            case "leaderboard":
            case "lb":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /basketbal leaderboard <create|delete|list> [name] [page]");
                    return true;
                }
                handleLeaderboardCommand(sender, args);
                return true;
                
            default:
                showHelp(sender);
                return true;
        }
    }
    
    /**
     * Handle region subcommands
     */
    private void handleRegionCommand(CommandSender sender, String[] args) {
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "create":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /basketbal region create <name>");
                    return;
                }
                handleRegionCreate(sender, args[2]);
                break;
                
            case "delete":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /basketbal region delete <name>");
                    return;
                }
                handleRegionDelete(sender, args[2]);
                break;
                
            case "list":
                int page = 1;
                if (args.length > 2) {
                    try {
                        page = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid page number!");
                        return;
                    }
                }
                handleRegionList(sender, page);
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "Unknown region subcommand. Use: create, delete, or list");
                break;
        }
    }
    
    /**
     * Handle leaderboard subcommands
     */
    private void handleLeaderboardCommand(CommandSender sender, String[] args) {
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "create":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /basketbal leaderboard create <region_name>");
                    return;
                }
                handleLeaderboardCreate(sender, args[2]);
                break;
                
            case "delete":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /basketbal leaderboard delete <region_name>");
                    return;
                }
                handleLeaderboardDelete(sender, args[2]);
                break;
                
            case "list":
                int page = 1;
                if (args.length > 2) {
                    try {
                        page = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid page number!");
                        return;
                    }
                }
                handleLeaderboardList(sender, page);
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "Unknown leaderboard subcommand. Use: create, delete, or list");
                break;
        }
    }
    
    /**
     * Handle region creation
     */
    private void handleRegionCreate(CommandSender sender, String regionName) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return;
        }
        
        Player player = (Player) sender;
        
        // Check if region already exists
        if (plugin.getBasketballManager().getRegion(regionName) != null) {
            player.sendMessage(ChatColor.RED + "Region '" + regionName + "' already exists!");
            return;
        }
        
        // Create new region at player location
        Location playerLocation = player.getLocation();
        BasketballRegion newRegion = new BasketballRegion(regionName, playerLocation, playerLocation);
        
        // Register region
        plugin.getBasketballManager().registerRegion(regionName, newRegion);
        
        player.sendMessage(ChatColor.GREEN + "Basketball region '" + regionName + "' created successfully!");
        player.sendMessage(ChatColor.YELLOW + "Location: " + formatLocation(playerLocation));
        player.sendMessage(ChatColor.YELLOW + "Use /basketbal leaderboard create " + regionName + " to create a leaderboard!");
    }
    
    /**
     * Handle region deletion
     */
    private void handleRegionDelete(CommandSender sender, String regionName) {
        // Check if region exists
        BasketballRegion region = plugin.getBasketballManager().getRegion(regionName);
        if (region == null) {
            sender.sendMessage(ChatColor.RED + "Region '" + regionName + "' not found!");
            return;
        }
        
        // Remove hologram
        plugin.getHologramManager().removeRegionHologram(regionName);
        
        // Remove region
        plugin.getBasketballManager().unregisterRegion(regionName);
        
        sender.sendMessage(ChatColor.GREEN + "Basketball region '" + regionName + "' deleted successfully!");
    }
    
    /**
     * Handle region listing with pagination
     */
    private void handleRegionList(CommandSender sender, int page) {
        Map<String, BasketballRegion> regions = plugin.getBasketballManager().getRegions();
        
        if (regions.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No basketball regions found.");
            return;
        }
        
        // Convert to list for pagination
        List<Map.Entry<String, BasketballRegion>> regionList = new ArrayList<>(regions.entrySet());
        
        int regionsPerPage = 5;
        int totalPages = (int) Math.ceil((double) regionList.size() / regionsPerPage);
        
        if (page < 1 || page > totalPages) {
            sender.sendMessage(ChatColor.RED + "Invalid page number! Available pages: 1-" + totalPages);
            return;
        }
        
        // Calculate start and end indices
        int startIndex = (page - 1) * regionsPerPage;
        int endIndex = Math.min(startIndex + regionsPerPage, regionList.size());
        
        // Display header
        sender.sendMessage(ChatColor.GOLD + "╔══════════════════════════════════════╗");
        sender.sendMessage(ChatColor.GOLD + "║" + ChatColor.YELLOW + "        Basketball Regions        " + ChatColor.GOLD + "║");
        sender.sendMessage(ChatColor.GOLD + "║" + ChatColor.GRAY + "        Page " + page + "/" + totalPages + "        " + ChatColor.GOLD + "║");
        sender.sendMessage(ChatColor.GOLD + "╠══════════════════════════════════════╣");
        
        // Display regions for current page
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, BasketballRegion> entry = regionList.get(i);
            String regionName = entry.getKey();
            BasketballRegion region = entry.getValue();
            
            Location center = region.getCenter();
            String locationStr = formatLocation(center);
            
            // Count players in region
            Set<Player> playersInRegion = region.getPlayersInRegion();
            int playerCount = playersInRegion.size();
            
            String line = ChatColor.GOLD + "║" + ChatColor.AQUA + " " + regionName + 
                ChatColor.GRAY + " (" + playerCount + " players)";
            
            // Pad to 36 characters
            while (line.length() < 36) {
                line += " ";
            }
            line += ChatColor.GOLD + "║";
            sender.sendMessage(line);
            
            // Location line
            String locationLine = ChatColor.GOLD + "║" + ChatColor.WHITE + "   " + locationStr;
            while (locationLine.length() < 36) {
                locationLine += " ";
            }
            locationLine += ChatColor.GOLD + "║";
            sender.sendMessage(locationLine);
            
            // Empty line for spacing
            if (i < endIndex - 1) {
                sender.sendMessage(ChatColor.GOLD + "║" + ChatColor.GRAY + "                                    " + ChatColor.GOLD + "║");
            }
        }
        
        // Display footer
        sender.sendMessage(ChatColor.GOLD + "╚══════════════════════════════════════╝");
        
        // Show navigation info
        if (totalPages > 1) {
            sender.sendMessage(ChatColor.YELLOW + "Use /basketbal region list <page> to navigate");
        }
    }
    
    /**
     * Handle leaderboard creation
     */
    private void handleLeaderboardCreate(CommandSender sender, String regionName) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return;
        }
        
        Player player = (Player) sender;
        
        // Check if region exists
        BasketballRegion region = plugin.getBasketballManager().getRegion(regionName);
        if (region == null) {
            player.sendMessage(ChatColor.RED + "Region '" + regionName + "' not found!");
            return;
        }
        
        // Create leaderboard hologram at player location
        Location hologramLocation = player.getLocation().add(0, 3, 0);
        plugin.getHologramManager().createRegionHologram(regionName, hologramLocation);
        
        player.sendMessage(ChatColor.GREEN + "Leaderboard created for region '" + regionName + "'!");
        player.sendMessage(ChatColor.YELLOW + "Location: " + formatLocation(hologramLocation));
    }
    
    /**
     * Handle leaderboard deletion
     */
    private void handleLeaderboardDelete(CommandSender sender, String regionName) {
        // Check if region exists
        BasketballRegion region = plugin.getBasketballManager().getRegion(regionName);
        if (region == null) {
            sender.sendMessage(ChatColor.RED + "Region '" + regionName + "' not found!");
            return;
        }
        
        // Remove leaderboard hologram
        plugin.getHologramManager().removeRegionHologram(regionName);
        
        sender.sendMessage(ChatColor.GREEN + "Leaderboard deleted for region '" + regionName + "'!");
    }
    
    /**
     * Handle leaderboard listing
     */
    private void handleLeaderboardList(CommandSender sender, int page) {
        Map<String, BasketballRegion> regions = plugin.getBasketballManager().getRegions();
        
        if (regions.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No basketball regions found.");
            return;
        }
        
        // Convert to list for pagination
        List<Map.Entry<String, BasketballRegion>> regionList = new ArrayList<>(regions.entrySet());
        
        int regionsPerPage = 5;
        int totalPages = (int) Math.ceil((double) regionList.size() / regionsPerPage);
        
        if (page < 1 || page > totalPages) {
            sender.sendMessage(ChatColor.RED + "Invalid page number! Available pages: 1-" + totalPages);
            return;
        }
        
        // Calculate start and end indices
        int startIndex = (page - 1) * regionsPerPage;
        int endIndex = Math.min(startIndex + regionsPerPage, regionList.size());
        
        // Display header
        sender.sendMessage(ChatColor.GOLD + "╔══════════════════════════════════════╗");
        sender.sendMessage(ChatColor.GOLD + "║" + ChatColor.YELLOW + "        Basketball Leaderboards        " + ChatColor.GOLD + "║");
        sender.sendMessage(ChatColor.GOLD + "║" + ChatColor.GRAY + "        Page " + page + "/" + totalPages + "        " + ChatColor.GOLD + "║");
        sender.sendMessage(ChatColor.GOLD + "╠══════════════════════════════════════╣");
        
        // Display regions for current page
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, BasketballRegion> entry = regionList.get(i);
            String regionName = entry.getKey();
            BasketballRegion region = entry.getValue();
            
            // Check if leaderboard exists for this region
            boolean hasLeaderboard = plugin.getHologramManager().hasRegionHologram(regionName);
            
            String line = ChatColor.GOLD + "║" + ChatColor.AQUA + " " + regionName + 
                (hasLeaderboard ? ChatColor.GREEN + " ✓" : ChatColor.RED + " ✗");
            
            // Pad to 36 characters
            while (line.length() < 36) {
                line += " ";
            }
            line += ChatColor.GOLD + "║";
            sender.sendMessage(line);
        }
        
        // Display footer
        sender.sendMessage(ChatColor.GOLD + "╚══════════════════════════════════════╝");
        
        // Show navigation info
        if (totalPages > 1) {
            sender.sendMessage(ChatColor.YELLOW + "Use /basketbal leaderboard list <page> to navigate");
        }
    }
    
    /**
     * Format location for display
     */
    private String formatLocation(Location location) {
        return "X:" + location.getBlockX() + " Y:" + location.getBlockY() + " Z:" + location.getBlockZ() + 
               " (" + location.getWorld().getName() + ")";
    }
    
    /**
     * Show help message
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "╔══════════════════════════════════════╗");
        sender.sendMessage(ChatColor.GOLD + "║" + ChatColor.YELLOW + "        Basketball Commands        " + ChatColor.GOLD + "║");
        sender.sendMessage(ChatColor.GOLD + "╠══════════════════════════════════════╣");
        sender.sendMessage(ChatColor.GOLD + "║" + ChatColor.WHITE + " /basketbal help" + ChatColor.GOLD + "                    ║");
        sender.sendMessage(ChatColor.GOLD + "║" + ChatColor.WHITE + " /basketbal region create <name>" + ChatColor.GOLD + "      ║");
        sender.sendMessage(ChatColor.GOLD + "║" + ChatColor.WHITE + " /basketbal region delete <name>" + ChatColor.GOLD + "      ║");
        sender.sendMessage(ChatColor.GOLD + "║" + ChatColor.WHITE + " /basketbal region list [page]" + ChatColor.GOLD + "        ║");
        sender.sendMessage(ChatColor.GOLD + "║" + ChatColor.WHITE + " /basketbal leaderboard create <region>" + ChatColor.GOLD + " ║");
        sender.sendMessage(ChatColor.GOLD + "║" + ChatColor.WHITE + " /basketbal leaderboard delete <region>" + ChatColor.GOLD + " ║");
        sender.sendMessage(ChatColor.GOLD + "║" + ChatColor.WHITE + " /basketbal leaderboard list [page]" + ChatColor.GOLD + "   ║");
        sender.sendMessage(ChatColor.GOLD + "╚══════════════════════════════════════╝");
    }
} 