package com.codisimus.plugins.phatloots.listeners;

import com.codisimus.plugins.phatloots.Loot;
import com.codisimus.plugins.phatloots.PhatLoots;
import com.codisimus.plugins.phatloots.PhatLootsChest;
import com.codisimus.plugins.phatloots.PhatLootsMain;
import com.codisimus.plugins.phatloots.SaveSystem;
import com.google.common.collect.Sets;
import java.io.File;
import java.util.HashSet;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Executes Player Commands
 * 
 * @author Codisimus
 */
public class commandListener implements CommandExecutor {
    public static enum Action {
        MAKE, LINK, UNLINK, DELETE, TIME, TYPE,
        ADD, REMOVE, MONEY, LIST, INFO, RESET, RL
    }
    public static final HashSet TRANSPARENT = Sets.newHashSet((byte)27, (byte)28,
            (byte)37, (byte)38, (byte)39, (byte)40, (byte)50, (byte)65, (byte)66,
            (byte)69, (byte)70, (byte)72, (byte)75, (byte)76, (byte)78);
    
    /**
     * Listens for PhatLoots commands to execute them
     * 
     * @param sender The CommandSender who may not be a Player
     * @param command The command that was executed
     * @param alias The alias that the sender used
     * @param args The arguments for the command
     * @return true always
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        //Cancel if the command is not from a Player
        if (!(sender instanceof Player)) {
            if (args.length > 0 && args[0].equals("rl"))
                rl(null);
            
            return true;
        }
        
        Player player = (Player)sender;

        //Display help page if the Player did not add any arguments
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        //Execute the correct command
        switch (Action.valueOf(args[0])) {
            case MAKE:
                //Cancel if the Player does not have permission to use the command
                if (!PhatLootsMain.hasPermission(player, "make")) {
                    player.sendMessage("You do not have permission to do that.");
                    return true;
                }
                
                if (args.length == 2)
                    make(player, args[1]);
                else
                    sendHelp(player);
                
                return true;
                
            case LINK:
                //Cancel if the Player does not have permission to use the command
                if (!PhatLootsMain.hasPermission(player, "make")) {
                    player.sendMessage("You do not have permission to do that.");
                    return true;
                }
                
                if (args.length == 2)
                    link(player, args[1]);
                else
                    sendHelp(player);
                
                return true;
                
            case UNLINK:
                //Cancel if the Player does not have permission to use the command
                if (!PhatLootsMain.hasPermission(player, "make")) {
                    player.sendMessage("You do not have permission to do that.");
                    return true;
                }
                
                if (args.length == 1)
                    unlink(player);
                else
                    sendHelp(player);
                
                return true;
                
            case DELETE:
                //Cancel if the Player does not have permission to use the command
                if (!PhatLootsMain.hasPermission(player, "make")) {
                    player.sendMessage("You do not have permission to do that.");
                    return true;
                }
                
                if (args.length == 2)
                    delete(player, args[1]);
                else
                    sendHelp(player);
                
                return true;
                
            case TIME:
                //Cancel if the Player does not have permission to use the command
                if (!PhatLootsMain.hasPermission(player, "make")) {
                    player.sendMessage("You do not have permission to do that.");
                    return true;
                }
                
                switch (args.length) {
                    case 5: //Name is not provided
                        try {
                            time(player, null, Integer.parseInt(args[1]), Integer.parseInt(args[2]),
                                    Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                            return true;
                        }
                        catch (Exception notInt) {
                            sendHelp(player);
                            break;
                        }
                        
                    case 6: //Name is provided
                        try {
                            time(player, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]),
                                    Integer.parseInt(args[4]), Integer.parseInt(args[5]));
                            return true;
                        }
                        catch (Exception notInt) {
                            sendHelp(player);
                            break;
                        }
                        
                    default: break;
                }
                
                sendHelp(player);
                return true;
                
            case TYPE:
                //Cancel if the Player does not have permission to use the command
                if (!PhatLootsMain.hasPermission(player, "make")) {
                    player.sendMessage("You do not have permission to do that.");
                    return true;
                }
                
                boolean global;
                switch (args.length) {
                    case 2: //Name is not provided
                        if (args[1].equals("global"))
                            global = true;
                        else if (args[1].equals("player"))
                            global = false;
                        else
                            break;
                        
                        type(player, null, global);
                        return true;
                        
                    case 3: //Name is provided
                        if (args[2].equals("global"))
                            global = true;
                        else if (args[2].equals("player"))
                            global = false;
                        else
                            break;
                        
                        type(player, args[1], global);
                        return true;
                        
                    default: break;
                }
                
                sendHelp(player);
                return true;
                
            case ADD:
                //Cancel if the Player does not have permission to use the command
                if (!PhatLootsMain.hasPermission(player, "make")) {
                    player.sendMessage("You do not have permission to do that.");
                    return true;
                }
                
                String addName = null; //The name of the PhatLoots
                int addID = 0; //The ID of the Loot collection (0 == IndividualLoots)
                Loot addLoot = null; //The Loot to be added
                
                switch (args.length) {
                    case 4: //All optional fields are missing
                        addLoot = getLoot(player, args[1], "0", args[2], args[3]);
                        break;
                        
                    case 5: //One optional field is present
                        if (args[1].startsWith("coll")) {
                            //ID field is present
                            addLoot = getLoot(player, args[2], "0", args[3], args[4]);
                            
                            try {
                                addID = Integer.parseInt(args[1].substring(4));
                                break;
                            }
                            catch (Exception notInt) {
                                sendHelp(player);
                                return true;
                            }
                        }
                        
                        for (PhatLoots phatLoots: SaveSystem.phatLootsList)
                            if (phatLoots.name.equals(args[1])) {
                                addName = args[1];
                                break;
                            }
                        
                        if (addName != null) {
                            //Name field is present
                            addLoot = getLoot(player, args[2], "0", args[3], args[4]);
                            break;
                        }
                        
                        //Durability field is present
                        addLoot = getLoot(player, args[1], args[2], args[3], args[4]);
                        break;
                        
                    case 6: //One optional field is missing
                        if (args[1].startsWith("coll")) {
                            //ID and Durability fields are present
                            addLoot = getLoot(player, args[2], args[3], args[4], args[5]);
                            
                            try {
                                addID = Integer.parseInt(args[1].substring(4));
                                break;
                            }
                            catch (Exception notInt) {
                                sendHelp(player);
                                return true;
                            }
                        }
                        else if (args[2].startsWith("coll")) {
                            //Name and ID fields are present
                            addLoot = getLoot(player, args[3], "0", args[4], args[5]);
                            
                            addName = args[1];
                            
                            try {
                                addID = Integer.parseInt(args[2].substring(4));
                                break;
                            }
                            catch (Exception notInt) {
                                sendHelp(player);
                                return true;
                            }
                        }
                        else {
                            //Name and Durability fields are present
                            addLoot = getLoot(player, args[2], args[3], args[4], args[5]);
                            
                            addName = args[1];
                        }
                        
                        break;
                        
                    case 7: //All optional fields are present
                        addName = args[1];
                        
                        try {
                            addID = Integer.parseInt(args[2].substring(4));
                        }
                        catch (Exception notInt) {
                            sendHelp(player);
                            return true;
                        }
                        
                        addLoot = getLoot(player, args[3], args[4], args[5], args[6]);
                        break;
                        
                    default: sendHelp(player); return true;
                    
                }
                
                if (addLoot == null)
                    return true;
                
                setLoot(player, addName, true, addID, addLoot);
                return true;
                
            case REMOVE:
                //Cancel if the Player does not have permission to use the command
                if (!PhatLootsMain.hasPermission(player, "make")) {
                    player.sendMessage("You do not have permission to do that.");
                    return true;
                }
                
                String removeName = null; //The name of the PhatLoots
                int removeID = 0; //The ID of the Loot collection (0 == IndividualLoots)
                Loot removeLoot; //The Loot to be removed
                
                switch (args.length) {
                    case 4: //All optional fields are missing
                        removeLoot = getLoot(player, args[1], "0", args[2], args[3]);
                        break;
                        
                    case 5: //One optional field is present
                        if (args[1].startsWith("coll")) {
                            //ID field is present
                            removeLoot = getLoot(player, args[2], "0", args[3], args[4]);
                            
                            try {
                                removeID = Integer.parseInt(args[1].substring(4));
                                break;
                            }
                            catch (Exception notInt) {
                                sendHelp(player);
                                return true;
                            }
                        }
                        
                        for (PhatLoots phatLoots: SaveSystem.phatLootsList)
                            if (phatLoots.name.equals(args[1])) {
                                removeName = args[1];
                                break;
                            }
                        
                        if (removeName != null) {
                            //Name field is present
                            removeLoot = getLoot(player, args[2], "0", args[3], args[4]);
                            break;
                        }
                        
                        //Durability field is present
                        removeLoot = getLoot(player, args[1], args[2], args[3], args[4]);
                        break;
                        
                    case 6: //One optional field is missing
                        if (args[1].startsWith("coll")) {
                            //ID and Durability fields are present
                            removeLoot = getLoot(player, args[2], args[3], args[4], args[5]);
                            
                            try {
                                removeID = Integer.parseInt(args[1].substring(4));
                                break;
                            }
                            catch (Exception notInt) {
                                sendHelp(player);
                                return true;
                            }
                        }
                        else if (args[2].startsWith("coll")) {
                            //Name and ID fields are present
                            removeLoot = getLoot(player, args[3], "0", args[4], args[5]);
                            
                            removeName = args[1];
                            
                            try {
                                removeID = Integer.parseInt(args[2].substring(4));
                                break;
                            }
                            catch (Exception notInt) {
                                sendHelp(player);
                                return true;
                            }
                        }
                        else {
                            //Name and Durability fields are present
                            removeLoot = getLoot(player, args[2], args[3], args[4], args[5]);
                            
                            removeName = args[1];
                        }
                        
                        break;
                        
                    case 7: //All optional fields are present
                        removeName = args[1];
                        
                        try {
                            removeID = Integer.parseInt(args[2].substring(4));
                        }
                        catch (Exception notInt) {
                            sendHelp(player);
                            return true;
                        }
                        
                        removeLoot = getLoot(player, args[3], args[4], args[5], args[6]);
                        break;
                        
                    default: sendHelp(player); return true;
                    
                }
                
                if (removeLoot == null)
                    return true;
                
                setLoot(player, removeName, false, removeID, removeLoot);
                return true;
                
            case MONEY:
                //Cancel if the Player does not have permission to use the command
                if (!PhatLootsMain.hasPermission(player, "make")) {
                    player.sendMessage("You do not have permission to do that.");
                    return true;
                }
                
                switch (args.length) {
                    case 3: //Name is not provided
                        try {
                            setMoney(player, null, Integer.parseInt(args[1]), Integer.parseInt(args[2]));
                            return true;
                        }
                        catch (Exception notInt) {
                            sendHelp(player);
                            break;
                        }
                        
                    case 4: //Name is provided
                        try {
                            setMoney(player, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                            return true;
                        }
                        catch (Exception notInt) {
                            sendHelp(player);
                            break;
                        }
                        
                    default: break;
                }
                
                sendHelp(player);
                return true;
                
            case LIST:
                //Cancel if the Player does not have permission to use the command
                if (!PhatLootsMain.hasPermission(player, "list")) {
                    player.sendMessage("You do not have permission to do that.");
                    return true;
                }
                
                if (args.length == 1)
                    list(player);
                else
                    sendHelp(player);
                
                return true;
                
            case INFO:
                //Cancel if the Player does not have permission to use the command
                if (!PhatLootsMain.hasPermission(player, "info")) {
                    player.sendMessage("You do not have permission to do that.");
                    return true;
                }
                
                switch (args.length) {
                    case 1: info(player, null); return true; //Name is not provided
                    case 2: info(player, args[1]); return true; //Name is provided
                    default: sendHelp(player); return true;
                }
                
            case RESET:
                //Cancel if the Player does not have permission to use the command
                if (!PhatLootsMain.hasPermission(player, "reset")) {
                    player.sendMessage("You do not have permission to do that.");
                    return true;
                }
                
                switch (args.length) {
                    case 1: reset(player, null); return true; //Name is not provided
                    case 2: reset(player, args[1]); return true; //Name is provided
                    default: sendHelp(player); return true;
                }
            
            case RL:
                if (args.length == 1)
                    rl(player);
                else
                    sendHelp(player);
                
                return true;
                
            default: sendHelp(player); return true;
        }
    }
    
    /**
     * Creates a new PhatLoots of the given name
     * 
     * @param player The Player creating the PhatLoots
     * @param name The name of the PhatLoots being created (must not already exist)
     */
    public static void make(Player player, String name) {
        //Cancel if the PhatLoots already exists
        if (SaveSystem.findPhatLoots(name) != null) {
            player.sendMessage("A PhatLoots named "+name+" already exists.");
            return;
        }
        
        SaveSystem.phatLootsList.add(new PhatLoots(name));
        player.sendMessage("PhatLoots "+name+" Made!");
        SaveSystem.save();
    }
    
    /**
     * Links the target Block to the specified PhatLoots
     * 
     * @param player The Player linking the Block they are targeting
     * @param name The name of the PhatLoots the Block will be linked to
     */
    public static void link(Player player, String name) {
        //Cancel if the Player is not targeting a correct Block
        Block block = player.getTargetBlock(TRANSPARENT, 10);
        int id = block.getTypeId();
        if (id != 54 && id != 23) {
            player.sendMessage("You must target a Chest/Dispenser.");
            return;
        }
        
        //Cancel if the Block is already linked to a PhatLoots
        PhatLoots phatLoots = SaveSystem.findPhatLoots(block);
        if (phatLoots != null) {
            player.sendMessage("Target Block is already linked to PhatLoots "+phatLoots.name+".");
            return;
        }
        
        //Cancel if the PhatLoots with the given name does not exist
        phatLoots = SaveSystem.findPhatLoots(name);
        if (phatLoots == null) {
            player.sendMessage("PhatLoots "+name+" does not exsist.");
            return;
        }
        
        phatLoots.chests.add(new PhatLootsChest(block));
        player.sendMessage("Target Block has been linked to PhatLoots "+name+"!");
        SaveSystem.save();
    }
    
    /**
     * Unlinks the target Block from the specified PhatLoots
     * 
     * @param player The Player unlinking the Block they are targeting
     */
    public static void unlink(Player player) {
        //Cancel if the Player is not targeting a correct Block
        Block block = player.getTargetBlock(TRANSPARENT, 10);
        int id = block.getTypeId();
        if (id != 54 && id != 23) {
            player.sendMessage("You must target a Chest/Dispenser.");
            return;
        }
        
        //Cancel if the Block is not linked to a PhatLoots
        PhatLoots phatLoots = SaveSystem.findPhatLoots(block);
        if (phatLoots == null) {
            player.sendMessage("Target Block is not linked to a PhatLoots");
            return;
        }
        
        phatLoots.chests.remove(phatLoots.findChest(block));
        player.sendMessage("Target Block has been unlinked from PhatLoots "+phatLoots.name+"!");
        SaveSystem.save();
    }
    
    /**
     * Deletes the specified PhatLoots
     * If a name is not provided, the PhatLoots of the target Block is deleted
     * 
     * @param player The Player deleting the PhatLoots
     * @param name The name of the PhatLoots to be deleted
     */
    public static void delete(Player player, String name) {
        //Cancel if the PhatLoots was not found
        PhatLoots phatLoots = getPhatLoots(player, name);
        if (phatLoots == null)
            return;
        
        SaveSystem.phatLootsList.remove(phatLoots);
        
        //Delete the .dat file
        File trash = new File("plugins/PhatLoots/"+phatLoots.name+".dat");
        trash.delete();
        
        player.sendMessage("PhatLoots "+phatLoots.name+" was deleted!");
        SaveSystem.save();
    }
    
    /**
     * Modifies the reset time of the specified PhatLoots
     * If a name is not provided, the PhatLoots of the target Block is modified
     * 
     * @param player The Player modifying the PhatLoots
     * @param name The name of the PhatLoots to be modified
     * @param days The amount of days
     * @param hours The amount of hours
     * @param minutes The amount of minutes
     * @param seconds The amount of seconds
     */
    public static void time(Player player, String name, int days, int hours, int minutes, int seconds) {
        //Cancel if the PhatLoots was not found
        PhatLoots phatLoots = getPhatLoots(player, name);
        if (phatLoots == null)
            return;
        
        phatLoots.days = days;
        phatLoots.hours = hours;
        phatLoots.minutes = minutes;
        phatLoots.seconds = seconds;
        player.sendMessage("Reset time for PhatLoots "+phatLoots.name+" has been set to "+days+" days, "
                +hours+" hours, "+minutes+" minutes, and "+seconds+" seconds.");
        
        SaveSystem.save();
    }
    
    /**
     * Modifies the reset type of the specified PhatLoots
     * If a name is not provided, the PhatLoots of the target Block is modified
     * 
     * @param player The Player modifying the PhatLoots
     * @param name The name of the PhatLoots to be modified
     * @param global True if the new reset type is global
     */
    public static void type(Player player, String name, boolean global) {
        //Cancel if the PhatLoots was not found
        PhatLoots phatLoots = getPhatLoots(player, name);
        if (phatLoots == null)
            return;
        
        phatLoots.global = global;
        String type = "player";
        if (global)
            type = "global";
        player.sendMessage("Reset type for PhatLoots "+phatLoots.name+" has been set to "+type+"!");
        
        SaveSystem.save();
    }
    
    /**
     * Adds/Removes a Loot to the specified PhatLoots
     * If a name is not provided, the PhatLoots of the target Block is modified
     * 
     * @param player The Player modifying the PhatLoots
     * @param name The name of the PhatLoots to be modified
     * @param add True the Loot will be added, false if it will be removed
     * @param lootID The id of the Loot, 0 for individual loots
     * @param loot The Loot that will be added/removed
     */
    public static void setLoot(Player player, String name, boolean add, int lootID, Loot loot) {
        //Cancel if the PhatLoots was not found
        PhatLoots phatLoots = getPhatLoots(player, name);
        if (phatLoots == null)
            return;
        
        if (add) { //Add the Loot
            phatLoots.loots[lootID].add(loot);
            if (lootID == 0) //Individual Loot
                player.sendMessage(loot.item.getType().name()+" added as Loot for Phat Loot "+phatLoots.name+"!");
            else //Collective Loot
                player.sendMessage(loot.item.getType().name()+" added as Loot to coll"+lootID+", "
                        +phatLoots.getPercentRemaining(lootID)+"% remaining");
        }
        else { //Subtract the Loot
            phatLoots.loots[lootID].remove(loot);
            if (lootID == 0) //Individual Loot
                player.sendMessage(loot.item.getType().name()+" removed as Loot for Phat Loot "+phatLoots.name+"!");
            else //Collective Loot
                player.sendMessage(loot.item.getType().name()+" removed as Loot to coll"+lootID+", "
                        +phatLoots.getPercentRemaining(lootID)+"% remaining");
        }
        
        SaveSystem.save();
    }
    
    /**
     * Sets the money range of the specified PhatLoots
     * If a name is not provided, the PhatLoots of the target Block is modified
     * 
     * @param player The Player modifying the PhatLoots
     * @param name The name of the PhatLoots to be modified
     * @param low The lower bound of the range
     * @param high The upper bound of the range
     */
    public static void setMoney(Player player, String name, int low, int high) {
        //Cancel if the PhatLoots was not found
        PhatLoots phatLoots = getPhatLoots(player, name);
        if (phatLoots == null)
            return;
        
        //Swap the numbers if they are out of order
        if (high < low) {
            int temp = high;
            high = low;
            low = temp;
        }
        
        phatLoots.rangeLow = low;
        phatLoots.rangeHigh = high;
        player.sendMessage("Money set to a range from "+low+" to "+high);
        
        SaveSystem.save();
    }
    
    /**
     * Displays a list of current PhatLoots
     * 
     * @param player The Player requesting the list
     */
    public static void list(Player player) {
        String list = "Current Phat Loots:  ";
        
        //Concat each PhatLoots
        for (PhatLoots phatLoots: SaveSystem.phatLootsList)
            list = list.concat(phatLoots.name+", ");
        
        player.sendMessage(list.substring(0, list.length() - 2));
    }
    
    /**
     * Displays the info of the specified PhatLoots
     * If a name is not provided, the PhatLoots of the target Block is used
     * 
     * @param player The Player requesting the info
     * @param name The name of the PhatLoots
     */
    public static void info(Player player, String name) {
        //Cancel if the PhatLoots was not found
        PhatLoots phatLoots = getPhatLoots(player, name);
        if (phatLoots == null)
            return;
        
        String type = "player";
        if (phatLoots.global)
            type = "global";
        
        player.sendMessage("§2Name:§b "+phatLoots.name+" §2Reset Type:§b "+type+" §2# of collective loots:§b "+phatLoots.numberCollectiveLoots);
        player.sendMessage("§2Reset Time:§b "+phatLoots.days+" days, "+phatLoots.hours+" hours, "+phatLoots.minutes+" minutes, and "+phatLoots.seconds+" seconds.");
        player.sendMessage("§2Money Range§b: "+phatLoots.rangeLow+"-"+phatLoots.rangeHigh);
        
        //Display Individual Loots if not empty
        String loots = phatLoots.getLoots(0);
        if (!loots.isEmpty())
            player.sendMessage("§2IndividualLoots§b: "+loots);
        
        //Display each Collective Loots that is not empty
        for (int i = 1; i <= 5; i++) {
            loots = phatLoots.getLoots(i);
            if (!loots.isEmpty())
                player.sendMessage("§2Coll"+i+"§b: "+loots);
        }
    }
    
    /**
     * Reset the use times of the specified PhatLoots/PhatLootsChest
     * If a name is not provided, the target PhatLootsChest is reset
     * 
     * @param player The Player reseting the PhatLootsChests
     * @param name The name of the PhatLoots
     */
    public static void reset(Player player, String name) {
        //Reset the target Button if a name was not provided
        if (name == null) {
            //Find the PhatLoots that will be reset using the given name
            Block block = player.getTargetBlock(TRANSPARENT, 10);
            PhatLoots phatLoots = SaveSystem.findPhatLoots(block);
            
            //Cancel if the PhatLoots does not exist
            if (phatLoots == null ) {
                player.sendMessage("Target Block is not linked to a PhatLoots");
                return;
            }
            
            phatLoots.reset(block);
            
            player.sendMessage("Target Block has been reset.");
            return;
        }
        
        //Reset all Buttons in every PhatLoots if the name provided is 'all'
        if (name.equals("all")) {
            for (PhatLoots phatLoots: SaveSystem.phatLootsList)
                phatLoots.reset(null);
            
            player.sendMessage("All Chests in each PhatLoots has been reset.");
            return;
        }
        
        //Find the PhatLoots that will be reset using the given name
        PhatLoots phatLoots = SaveSystem.findPhatLoots(name);

        //Cancel if the PhatLoots does not exist
        if (phatLoots == null ) {
            player.sendMessage("PhatLoots "+name+" does not exsist.");
            return;
        }
        
        //Reset all Buttons linked to the PhatLoots
        phatLoots.reset(null);
        
        player.sendMessage("All Chests in PhatLoots "+name+" have been reset.");
        SaveSystem.save();
    }
    
    /**
     * Reloads PhatLoots data
     * 
     * @param player The Player reloading the data 
     */
    public static void rl(Player player) {
        SaveSystem.phatLootsList.clear();
        SaveSystem.save = true;
        SaveSystem.load();
        PhatLootsMain.pm = PhatLootsMain.server.getPluginManager();
        
        System.out.println("[PhatLoots] reloaded");
        if (player != null)
            player.sendMessage("PhatLoots reloaded");
        return;
    }
    
    /**
     * Displays the PhatLoots Help Page to the given Player
     *
     * @param player The Player needing help
     */
    public static void sendHelp(Player player) {
        player.sendMessage("§e     PhatLoots Help Page:");
        player.sendMessage("§2/loot make [Name]§b Creates PhatLoot");
        player.sendMessage("§2/loot link [Name]§b Links target Chest/Dispenser with PhatLoot");
        player.sendMessage("§2/loot unlink§b Unlinks target Block");
        player.sendMessage("§2/loot delete (Name)§b Deletes PhatLoot and unlinks Block");
        player.sendMessage("§2/loot time (Name) [Days] [Hrs] [Mins] [Secs]§b Sets cooldown time");
        player.sendMessage("§2/loot type (Name) ['global' or 'player']§b Sets cooldown type");
        player.sendMessage("§2/loot ['add' or 'remove'] (Name) ('coll'[1-5]) [Item] (Durability) [Amount] [Percent]"
                + "§b Manage items that may be looted");
        player.sendMessage("§2/loot money (Name) [Low] [High]§b Sets money range to be looted");
        player.sendMessage("§2/loot list§b Lists all PhatLoots");
        player.sendMessage("§2/loot info (Name)§b Lists info of PhatLoot");
        player.sendMessage("§2/loot reset§b Resets activation times for target Block");
        player.sendMessage("§2/loot reset [Name or 'all']§b Resets Block linked to the PhatLoots");
        player.sendMessage("§2/loot rl§b Reloads PhatLoots Plugin");
    }
    
    /**
     * Returns the PhatLoots with the given name
     * If no name is provided the PhatLoots is found using the target Block
     * 
     * @param player The Player target the Block
     * @param name The name of the PhatLoots to be found
     * @return The PhatLoots or null if none was found
     */
    public static PhatLoots getPhatLoots(Player player, String name) {
        PhatLoots phatLoots = null;
        
        if (name == null) {
            //Find the PhatLoots using the target Block
            phatLoots = SaveSystem.findPhatLoots(player.getTargetBlock(TRANSPARENT, 10));
            
            //Cancel if the PhatLoots does not exist
            if (phatLoots == null ) {
                player.sendMessage("Target Block is not linked to a PhatLoots");
                return null;
            }
        }
        else {
            //Find the PhatLoots using the given name
            phatLoots = SaveSystem.findPhatLoots(name);
            
            //Cancel if the PhatLoots does not exist
            if (phatLoots == null ) {
                player.sendMessage("PhatLoots "+name+" does not exsist.");
                return null;
            }
        }
        
        return phatLoots;
    }
    
    /**
     * Checks different aspects of the loot values and returns the combined string
     * 
     * @param player The player who is using the command
     * @param item The String of the item name or ID
     * @param amount The String of the number of items
     * @param percent The String of the percent chance of receiving loot
     */
    public Loot getLoot(Player player, String item, String durability, String amount, String percent) {
        //Return null if the Material id/name is invalid
        int id;
        try {
            id = Integer.parseInt(item);
            if (Material.getMaterial(id) == null) {
                player.sendMessage(item+" is not a valid item id");
                return null;
            }
        }
        catch (Exception notInt) {
            try {
                id = Material.getMaterial(item.toUpperCase()).getId();
            }
            catch (Exception invalid) {
                player.sendMessage(item+" is not a valid item");
                return null;
            }
        }

        //Return null if the probability is invalid
        int probablility;
        try {
            probablility = Integer.parseInt(percent);

            if (probablility < 1 || probablility > 100) {
                player.sendMessage(percent+" is not between 0 and 100");
                return null;
            }
        }
        catch (Exception notInt) {
            player.sendMessage(percent+" is not valid number between 0 and 100");
            return null;
        }

        //Return a newly created Loot using the given Loot data
        return new Loot(id, Short.parseShort(durability), Integer.parseInt(amount), probablility);
    }
}
