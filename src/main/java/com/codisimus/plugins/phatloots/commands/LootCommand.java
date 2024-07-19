package com.codisimus.plugins.phatloots.commands;

import com.codisimus.plugins.phatloots.PhatLoot;
import com.codisimus.plugins.phatloots.PhatLoots;
import com.codisimus.plugins.phatloots.PhatLootsAPI;
import com.codisimus.plugins.phatloots.PhatLootsConfig;
import com.codisimus.plugins.phatloots.commands.CommandHandler.CodCommand;
import com.codisimus.plugins.phatloots.gui.InventoryListener;
import com.codisimus.plugins.phatloots.loot.CommandLoot;
import com.codisimus.plugins.phatloots.loot.Experience;
import com.codisimus.plugins.phatloots.loot.LootBundle;
import com.codisimus.plugins.phatloots.loot.LootCollection;
import com.codisimus.plugins.phatloots.loot.Money;
import com.codisimus.plugins.phatloots.util.PhatLootsUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Executes Player Commands
 *
 * @author Codisimus
 */
public class LootCommand {
    public static boolean setUnlockable; //True if linked Chests should be set as unlockable by ChestLock

    @CodCommand(
        command = "make",
        weight = 10,
        aliases = { "create" },
        usage = {
            "§2<command> <Name>§b Create PhatLoot with given name"
        },
        permission = "phatloots.make"
    )
    public boolean make(CommandSender sender, String name) {
        //Cancel if the PhatLoot already exists
        if (PhatLoots.hasPhatLoot(name)) {
            sender.sendMessage("§4A PhatLoot named §6" + name + "§4 already exists.");
        } else {
            PhatLoots.addPhatLoot(new PhatLoot(name));
            sender.sendMessage("§5PhatLoot §6" + name + "§5 made!");
        }
        return true;
    }

    @CodCommand(
        command = "delete",
        weight = 20,
        usage = {
            "§2<command> <Name>§b Delete PhatLoot"
        },
        permission = "phatloots.delete"
    )
    public boolean delete(CommandSender sender, PhatLoot phatLoot) {
        PhatLoots.removePhatLoot(phatLoot);
        sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 was deleted!");
        return true;
    }

    @CodCommand(
        command = "rename",
        weight = 21,
        usage = {
            "§2<command> <PhatLoot> <Collection> <NewName>§b Rename a PhatLoot collection"
        },
        permission = "phatloots.make"
    )
    public boolean rename(CommandSender sender, PhatLoot phatLoot, String name, String newName) {
        LootCollection coll = phatLoot.findCollection(name);
        if (coll == null) {
            return false;
        }
        coll.name = newName;
        phatLoot.save();
        sender.sendMessage("§5Collection renamed to §6" + name);
        return true;
    }

    @CodCommand(
        command = "link",
        subcommand = "hand",
        weight = 30,
        usage = {
            "§2<command> <Name>§b Link Item in hand with PhatLoot"
        },
        permission = "phatloots.link"
    )
    public boolean linkHand(Player player, PhatLoot phatLoot) {
        //Cancel if the player is not holding an item
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        PhatLootsAPI.link(item, phatLoot);
        player.sendMessage("§6" + PhatLootsUtil.getItemName(item) + "§5 has been linked to PhatLoot §6" + phatLoot.name);
        return true;
    }

    @CodCommand(
        command = "link",
        weight = 30.1,
        usage = {
            "§2<command> <Name>§b Link target Block with PhatLoot"
        },
        permission = "phatloots.link"
    )
    public boolean link(Player player, PhatLoot phatLoot) {
        //Cancel if the player is not targeting a correct Block
        Block block  = player.getTargetBlock(null, 10);
        String blockName = PhatLootsUtil.getBlockName(block);
        if (!PhatLootsUtil.isLinkableType(block)) {
            player.sendMessage("§6" + blockName + "§4 is not a linkable type.");
            return true;
        }

        switch (block.getType()) {
        case CHEST:
            Chest chest = (Chest) block.getState();
            Inventory inventory = chest.getInventory();

            //Linked the left side if it is a DoubleChest
            if (inventory instanceof DoubleChestInventory) {
                chest = (Chest) ((DoubleChestInventory) inventory).getLeftSide().getHolder();
                block = chest.getBlock();
            }
            //Fall through
        case ENDER_CHEST:
            //Removed pending ChestLock supporting UUIDs
            ////Make the Chest unlockable if ChestLock is enabled
            //if (setUnlockable && Bukkit.getPluginManager().isPluginEnabled("ChestLock")) {
            //    Safe safe = ChestLock.findSafe(block);
            //    if (safe == null) {
            //        safe = new Safe(player.getName(), block);
            //        safe.lockable = false;
            //        safe.locked = false;
            //
            //        ChestLock.addSafe(safe);
            //    }
            //}
            break;

        default:
            break;
        }

        phatLoot.addChest(block);
        player.sendMessage("§5Target §6" + blockName + "§5 has been linked to PhatLoot §6" + phatLoot.name);
        phatLoot.saveChests();
        return true;
    }

    @CodCommand(command = "spill",
        weight = 35,
        usage = {
            "§2<command> <PhatLoot> <x> <y> <z> <world>§b Spill loot at a specified location"
        },
        permission = "phatloots.spill"
    )
    public boolean spill(CommandSender sender, PhatLoot phatLoot, int x, int y, int z, World world) {
        Location loc = new Location(world, x, y, z);
        LootBundle lootBundle = phatLoot.rollForLoot();
        for (ItemStack item : lootBundle.getItemList()) {
            if (item.getType() == Material.AIR)
                continue;

            world.dropItemNaturally(loc, item);
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            for (CommandLoot command : lootBundle.getCommandList()) {
                command.execute(player);
            }
        }

        // Check experience so orbs without any experience value don't spawn
        if (lootBundle.getExp() > 0) {
            ExperienceOrb orb = (ExperienceOrb) world.spawnEntity(loc, EntityType.EXPERIENCE_ORB);
            orb.setExperience(lootBundle.getExp());
        }

        return true;
    }

    @CodCommand(
            command = "unlink",
            subcommand = "hand",
            weight = 40,
            usage = {
                    "§2<command> <Name>§b Unlink Item in hand with PhatLoot"
            },
            permission = "phatloots.unlink"
    )
    public boolean unlinkHand(Player player, PhatLoot phatLoot) {
        //Cancel if the player is not holding an item
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        if (PhatLootsConfig.persistentDataContainerLinks) {
            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer itemTagContainer = meta.getPersistentDataContainer();
            itemTagContainer.remove(PhatLoot.LINK_TAG);
            item.setItemMeta(meta);
        } else {
            ItemMeta meta = item.getItemMeta();
            List<String> lore = Optional.ofNullable(meta).map(ItemMeta::getLore).orElse(new ArrayList<>());
            List<String> toRemove = new ArrayList<>();
            for (String line : lore) {
                String finalLine = "";
                for (String str : PhatLootsConfig.lootBagKeys) {
                    if (line.startsWith(str)) {
                        finalLine = str;
                        break;
                    }
                }

                if (finalLine.isEmpty())
                    continue;

                PhatLoot handLoot = PhatLoots.getPhatLoot(line.substring(finalLine.length()));
                if (handLoot == null || !handLoot.getName().equals(phatLoot.getName()))
                    continue;

                toRemove.add(line);
            }
            toRemove.forEach(lore::remove);
            lore.removeAll(toRemove);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        player.sendMessage("§6" + PhatLootsUtil.getItemName(item) + "§5 has been unlinked from PhatLoot " + ChatColor.GOLD + phatLoot.getName());
        return true;
    }

    @CodCommand(
        command = "unlink",
        weight = 40.1,
        usage = {
            "§2<command> [Name]§b Unlink target Block from PhatLoot",
            "§7If Name is not specified then all PhatLoots linked to the target Block will be affected"
        },
        permission = "phatloots.unlink"
    )
    public boolean unlink(Player player, PhatLoot phatLoot) {
        Block block = player.getTargetBlock(null, 10);
        phatLoot.removeChest(block);
        player.sendMessage("§5Target §6" + PhatLootsUtil.getBlockName(block) + "§5 has been unlinked from PhatLoot §6" + phatLoot.name);
        phatLoot.saveChests();
        return true;
    }

    @CodCommand(command = "unlink", weight = 40.2)
    public boolean unlink(Player player) {
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
            unlink(player, phatLoot);
        }
        return true;
    }

    @CodCommand(
        command = "time",
        weight = 50,
        usage = {
            "§2<command> [Name] <Days> <Hrs> <Mins> <Secs>§b Set cooldown time for PhatLoot",
            "§2<command> [Name] never§b Set PhatLoot to only be lootable once per chest",
            "§7If Name is not specified then all PhatLoots linked to the target Block will be affected"
        },
        permission = "phatloots.time"
    )
    public boolean time(CommandSender sender, PhatLoot phatLoot, int days, int hours, int minutes, int seconds) {
        phatLoot.days = days;
        phatLoot.hours = hours;
        phatLoot.minutes = minutes;
        phatLoot.seconds = seconds;
        sender.sendMessage("§5Reset time for PhatLoot §6" + phatLoot.name
                + "§5 has been set to §6" + days + " days, "
                + hours + " hours, " + minutes + " minutes, and "
                + seconds + " seconds");
        phatLoot.save();
        return true;
    }
    @CodCommand(command = "time", weight = 50.1)
    public boolean time(Player player, int days, int hours, int minutes, int seconds) {
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
            time(player, phatLoot, days, hours, minutes, seconds);
        }
        return true;
    }
    @CodCommand(command = "time", weight = 50.2)
    public boolean time(CommandSender sender, PhatLoot phatLoot, String string) {
        if (string.equals("never")) {
            phatLoot.days = -1;
            phatLoot.hours = -1;
            phatLoot.minutes = -1;
            phatLoot.seconds = -1;
            sender.sendMessage("§5PhatLoot §6" + phatLoot.name
                    + "§5 has been set to §6never§5 reset");
            phatLoot.save();
            return true;
        } else {
            return false;
        }
    }
    @CodCommand(command = "time", weight = 50.3)
    public boolean time(Player player, String string) {
        if (string.equals("never")) {
            for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
                time(player, phatLoot, string);
            }
            return true;
        } else {
            return false;
        }
    }

    @CodCommand(
        command = "global",
        weight = 60,
        usage = {
            "§2<command> [Name] <true|false>§b Set PhatLoot to global or individual",
            "§7If Name is not specified then all PhatLoots linked to the target Block will be affected"
        },
        permission = "phatloots.global"
    )
    public boolean global(CommandSender sender, PhatLoot phatLoot, boolean global) {
        phatLoot.global = global;
        phatLoot.reset(null);
        sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 has been set to §6"
                + (global ? "global" : "individual") + "§5 reset");
        phatLoot.save();
        return true;
    }
    @CodCommand(command = "global", weight = 60.1)
    public boolean global(Player player, boolean global) {
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
            global(player, phatLoot, global);
        }
        return true;
    }

    @CodCommand(
        command = "autoloot",
        weight = 70,
        usage = {
            "§2<command> [Name] <true|false>§b Set if Items are automatically looted",
            "§7If Name is not specified then all PhatLoots linked to the target Block will be affected"
        },
        permission = "phatloots.autoloot"
    )
    public boolean autoloot(CommandSender sender, PhatLoot phatLoot, boolean autoLoot) {
        phatLoot.autoLoot = autoLoot;
        sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 has been set to "
                + (autoLoot ? "automatically add Loot to the looters inventory." : "open the chest inventory for the looter."));
        phatLoot.save();
        return true;
    }
    @CodCommand(command = "autoloot", weight = 70.1)
    public boolean autoloot(Player player, boolean autoLoot) {
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
            autoloot(player, phatLoot, autoLoot);
        }
        return true;
    }

    @CodCommand(
        command = "break",
        weight = 80,
        usage = {
            "§2<command> [Name] <true|false>§b Set if global Chests are broken after looting",
            "§7If Name is not specified then all PhatLoots linked to the target Block will be affected"
        },
        permission = "phatloots.break"
    )
    public boolean breakAndRespawn(CommandSender sender, PhatLoot phatLoot, boolean breakAndRespawn) {
        if (breakAndRespawn && !phatLoot.global) {
            phatLoot.global = true;
            phatLoot.reset(null);
            sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 has been set to §6global§5 reset");
        }
        phatLoot.breakAndRespawn = breakAndRespawn;
        sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 has been set to "
                + (breakAndRespawn
                   ? "automatically break global chests when they are looted and have them respawn."
                   : "keep chests present after looting."));
        phatLoot.save();
        return true;
    }
    @CodCommand(command = "break", weight = 80.1)
    public boolean breakAndRespawn(Player player, boolean breakAndRespawn) {
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
            breakAndRespawn(player, phatLoot, breakAndRespawn);
        }
        return true;
    }

    @CodCommand(
        command = "round",
        weight = 90,
        usage = {
            "§2<command> [Name] <true|false>§b Set if cooldown times should round down (ex. Daily/Hourly loots)",
            "§7If Name is not specified then all PhatLoots linked to the target Block will be affected"
        },
        permission = "phatloots.round"
    )
    public boolean round(CommandSender sender, PhatLoot phatLoot, boolean round) {
        phatLoot.round = round;
        sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 has been set to §6"
                + (round ? "" : "not ") + "round down time");
        phatLoot.save();
        return true;
    }
    @CodCommand(command = "round", weight = 90.1)
    public boolean round(Player player, boolean round) {
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
            round(player, phatLoot, round);
        }
        return true;
    }

    @CodCommand(
        command = "money",
        weight = 100,
        usage = {
            "§2<command> [Name] <Amount>§b Set money range to be looted",
            "§7If Name is not specified then all PhatLoots linked to the target Block will be affected",
            "§6Amount may be a number §4(100)§6 or range §4(100-500)"
        },
        permission = "phatloots.money"
    )
    public boolean money(CommandSender sender, PhatLoot phatLoot, int lower, int upper) {
        phatLoot.lootList.removeIf(loot -> loot instanceof Money);
        phatLoot.addLoot(new Money(lower, upper));
        sender.sendMessage("§5Money for PhatLoot §6"
                + phatLoot.name + "§5 set to "
                + (lower == upper
                   ? "§6"
                   : "a range from §6" + lower + "§5 to §6")
                + upper);
        phatLoot.save();
        return true;
    }

    @CodCommand(command = "money", weight = 100.1)
    public boolean money(Player player, int lower, int upper) {
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
            money(player, phatLoot, lower, upper);
        }
        return true;
    }
    @CodCommand(command = "money", weight = 100.2)
    public boolean money(Player player, PhatLoot phatLoot, int amount) {
        return money(player, phatLoot, amount, amount);
    }
    @CodCommand(command = "money", weight = 100.3)
    public boolean money(Player player, int amount) {
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
            money(player, phatLoot, amount, amount);
        }
        return true;
    }
    @CodCommand(command = "money", weight = 100.4)
    public boolean money(CommandSender sender, PhatLoot phatLoot, String range) {
        String[] bounds = range.split("-");
        int lower, upper;
        try {
            lower = Integer.parseInt(bounds[0]);
            upper = Integer.parseInt(bounds[1]);
        } catch (Exception ex) {
            return false;
        }
        return money(sender, phatLoot, lower, upper);
    }
    @CodCommand(command = "money", weight = 100.5)
    public boolean money(Player player, String range) {
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
            money(player, phatLoot, range);
        }
        return true;
    }

    @CodCommand(
        command = "cost",
        weight = 110,
        usage = {
            "§2<command> [Name] <Amount>§b Set cost of looting",
            "§7If Name is not specified then all PhatLoots linked to the target Block will be affected",
            "§6Amount may be a number §4(100)§6 or range §4(100-500)"
        },
        permission = "phatloots.cost"
    )
    public boolean cost(CommandSender sender, PhatLoot phatLoot, int lower, int upper) {
        return money(sender, phatLoot, -lower, -upper);
    }
    @CodCommand(command = "cost", weight = 110.1)
    public boolean cost(Player player, int lower, int upper) {
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
            money(player, phatLoot, -lower, -upper);
        }
        return true;
    }
    @CodCommand(command = "cost", weight = 110.2)
    public boolean cost(Player player, PhatLoot phatLoot, int amount) {
        return money(player, phatLoot, -amount, -amount);
    }
    @CodCommand(command = "cost", weight = 110.3)
    public boolean cost(Player player, int amount) {
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
            money(player, phatLoot, -amount, -amount);
        }
        return true;
    }
    @CodCommand(command = "cost", weight = 110.4)
    public boolean cost(CommandSender sender, PhatLoot phatLoot, String range) {
        String[] bounds = range.split("-");
        int lower, upper;
        try {
            lower = Integer.parseInt(bounds[0]);
            upper = Integer.parseInt(bounds[1]);
        } catch (Exception ex) {
            return false;
        }
        return money(sender, phatLoot, -lower, -upper);
    }
    @CodCommand(command = "cost", weight = 110.5)
    public boolean cost(Player player, String range) {
        String[] bounds = range.split("-");
        int lower, upper;
        try {
            lower = Integer.parseInt(bounds[0]);
            upper = Integer.parseInt(bounds[1]);
        } catch (Exception ex) {
            return false;
        }
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
            money(player, phatLoot, -lower, -upper);
        }
        return true;
    }

    @CodCommand(
        command = "exp",
        weight = 120,
        usage = {
            "§2<command> [Name] <Amount>§b Set experience to be gained",
            "§7If Name is not specified then all PhatLoots linked to the target Block will be affected",
            "§6Amount may be a number §4(100)§6 or range §4(100-500)"
        },
        permission = "phatloots.exp"
    )
    public boolean exp(CommandSender sender, PhatLoot phatLoot, int lower, int upper) {
        phatLoot.lootList.removeIf(loot -> loot instanceof Experience);
        phatLoot.addLoot(new Experience(lower, upper));
        sender.sendMessage("§5Experience for PhatLoot §6"
                + phatLoot.name + "§5 set to "
                + (lower == upper
                   ? "§6"
                   : "a range from §6" + lower + "§5 to §6")
                + upper);
        phatLoot.save();
        return true;
    }
    @CodCommand(command = "exp", weight = 120.1)
    public boolean exp(Player player, int lower, int upper) {
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
            exp(player, phatLoot, lower, upper);
        }
        return true;
    }
    @CodCommand(command = "exp", weight = 120.2)
    public boolean exp(CommandSender sender, PhatLoot phatLoot, int amount) {
        return exp(sender, phatLoot, amount, amount);
    }
    @CodCommand(command = "exp", weight = 120.3)
    public boolean exp(Player player, int amount) {
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
            exp(player, phatLoot, amount, amount);
        }
        return true;
    }
    @CodCommand(command = "exp", weight = 120.4)
    public boolean exp(CommandSender sender, PhatLoot phatLoot, String range) {
        String[] bounds = range.split("-");
        int lower, upper;
        try {
            lower = Integer.parseInt(bounds[0]);
            upper = Integer.parseInt(bounds[1]);
        } catch (Exception ex) {
            return false;
        }
        return exp(sender, phatLoot, lower, upper);
    }
    @CodCommand(command = "exp", weight = 120.5)
    public boolean exp(Player player, String range) {
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player)) {
            exp(player, phatLoot, range);
        }
        return true;
    }
    @CodCommand(
        command = "list",
        weight = 130,
        usage = {
            "§2<command>§b List all PhatLoots"
        },
        permission = "phatloots.list"
    )
    public boolean list(CommandSender sender) {
        StringBuilder list = new StringBuilder("§5Current PhatLoots: §6");
        //Concat each PhatLoot
        for (PhatLoot phatLoot : PhatLoots.getPhatLoots()) {
            list.append(phatLoot.name).append(", ");
        }
        sender.sendMessage(list.substring(0, list.length() - 2));
        return true;
    }

    @CodCommand(
        command = "gui",
        aliases = {"edit", "info"},
        weight = 140,
        usage = {
            "§2<command> [Name]§b Open info GUI of PhatLoot",
            "§7If Name is not specified then all PhatLoots linked to the target Block will be affected"
        },
        permission = "phatloots.info"
    )
    public boolean gui(Player player, PhatLoot phatLoot) {
        InventoryListener.viewPhatLoot(player, phatLoot);
        return true;
    }
    @CodCommand(command = "gui", weight = 140.1)
    public boolean gui(CommandSender sender, PhatLoot phatLoot) {
        sender.sendMessage("§2Name:§b " + phatLoot.name
                + " §2Global Reset:§b " + phatLoot.global
                + " §2Round Down:§b " + phatLoot.round);
        sender.sendMessage("§2Reset Time:§b " + phatLoot.days
                + " days, " + phatLoot.hours + " hours, "
                + phatLoot.minutes + " minutes, and "
                + phatLoot.seconds + " seconds.");
        return true;
    }
    @CodCommand(command = "gui", weight = 140.2)
    public boolean gui(Player player) {
        LinkedList<PhatLoot> phatLoots = PhatLootsUtil.getPhatLoots(player);
        switch (phatLoots.size()) {
        case 0:
            return false;
        case 1:
            gui(player, phatLoots.getFirst());
            break;
        default:
            StringBuilder list = new StringBuilder("§5Linked PhatLoots: §6");
            //Concat each PhatLoot
            for (PhatLoot pl : phatLoots) {
                list.append(pl.name).append(", ");
            }
            player.sendMessage(list.substring(0, list.length() - 2));
            break;
        }
        return true;
    }

    @CodCommand(
        command = "give",
        subcommand = "all",
        weight = 150,
        usage = {
            "§2<command> all <PhatLoot> [Title]§b Force all Players to loot a PhatLoot"
        },
        permission = "phatloots.give"
    )
    public boolean give(CommandSender sender, PhatLoot phatLoot) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            phatLoot.rollForLoot(player, phatLoot.name);
        }
        sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 given to §6all Players");
        return true;
    }
    @CodCommand(command = "give", subcommand = "all", weight = 150.1, minArgs = 1)
    public boolean give(CommandSender sender, PhatLoot phatLoot, String[] titleArray) {
        String title = ChatColor.translateAlternateColorCodes('&', PhatLootsUtil.concatArgs(titleArray));
        for (Player player : Bukkit.getOnlinePlayers()) {
            phatLoot.rollForLoot(player, title);
        }
        sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 given to §6all Players");
        return true;
    }

    @CodCommand(
        command = "give",
        weight = 151,
        usage = {
            "§2<command> <Player> <PhatLoot> [Title]§b Force Player to loot a PhatLoot",
            "§2<command> <World> <PhatLoot> [Title]§b Force all Players in a world to loot a PhatLoot"
        },
        permission = "phatloots.give"
    )
    public boolean give(CommandSender sender, Player player, PhatLoot phatLoot) {
        phatLoot.rollForLoot(player, phatLoot.name);
        sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 given to §6" + player.getName());
        return true;
    }
    @CodCommand(command = "give", weight = 151.1, minArgs = 1)
    public boolean give(CommandSender sender, Player player, PhatLoot phatLoot, String[] titleArray) {
        String title = ChatColor.translateAlternateColorCodes('&', PhatLootsUtil.concatArgs(titleArray));
        phatLoot.rollForLoot(player, title);
        sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 given to §6" + player.getName());
        return true;
    }
    @CodCommand(command = "give", weight = 152)
    public boolean give(CommandSender sender, World world, PhatLoot phatLoot) {
        for (Player player : world.getPlayers()) {
            phatLoot.rollForLoot(player, phatLoot.name);
        }
        sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 given to all Players in §6" + world.getName());
        return true;
    }
    @CodCommand(command = "give", weight = 152.1, minArgs = 1)
    public boolean give(CommandSender sender, World world, PhatLoot phatLoot, String[] titleArray) {
        String title = ChatColor.translateAlternateColorCodes('&', PhatLootsUtil.concatArgs(titleArray));
        for (Player player : world.getPlayers()) {
            phatLoot.rollForLoot(player, title);
        }
        sender.sendMessage("§5PhatLoot §6" + phatLoot.name + "§5 given to all Players in §6" + world.getName());
        return true;
    }

    @CodCommand(
        command = "reset",
        weight = 160,
        usage = {
            "§2<command>§b Reset looted times for target Block",
            "§2<command> <Name>§b Reset looted times for PhatLoot",
            "§2<command> all§b Reset looted times for all PhatLoots"
        },
        permission = "phatloots.reset"
    )
    public boolean reset(Player player) {
        Block block = player.getTargetBlock(null, 10);
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player, block)) {
            phatLoot.reset(block);
            player.sendMessage("§5Target §6" + PhatLootsUtil.getBlockName(block) + "§5 has been reset.");
        }
        return true;
    }
    @CodCommand(command = "reset", weight = 160.1)
    public boolean reset(CommandSender sender, PhatLoot phatLoot) {
        //Reset all Chests linked to the PhatLoot
        phatLoot.reset(null);
        sender.sendMessage("§5All Chests in PhatLoot §6" + phatLoot.name + "§5 have been reset.");
        return true;
    }
    @CodCommand(command = "reset", weight = 160.2)
    public boolean reset(CommandSender sender, String string) {
        //Reset all Chests in every PhatLoot if the string provided is 'all'
        if (string.equals("all")) {
            for (PhatLoot phatLoots : PhatLoots.getPhatLoots()) {
                phatLoots.reset(null);
            }
            sender.sendMessage("§5All Chests in each PhatLoot have been reset.");
        }
        return true;
    }

    @CodCommand(
        command = "clean",
        weight = 170,
        usage = {
            "§2<command>§b Clean looted times for target Block",
            "§2<command> <Name>§b Clean looted times for PhatLoot",
            "§2<command> all§b Clean looted times for all PhatLoots"
        },
        permission = "phatloots.clean"
    )
    public boolean clean(Player player) {
        Block block = player.getTargetBlock(null, 10);
        for (PhatLoot phatLoot : PhatLootsUtil.getPhatLoots(player, block)) {
            phatLoot.clean(block);
            player.sendMessage("§5Target §6" + PhatLootsUtil.getBlockName(block) + "§5 has been cleaned.");
        }
        return true;
    }
    @CodCommand(command = "clean", weight = 170.1)
    public boolean clean(CommandSender sender, PhatLoot phatLoot) {
        //Clean all Chests linked to the PhatLoot
        phatLoot.clean(null);
        sender.sendMessage("§5All Chests in PhatLoot §6" + phatLoot.name + "§5 have been cleaned.");
        return true;
    }
    @CodCommand(command = "clean", weight = 170.2)
    public boolean clean(CommandSender sender, String string) {
        //Clean all Chests in every PhatLoot if the string provided is 'all'
        if (string.equals("all")) {
            for (PhatLoot phatLoots : PhatLoots.getPhatLoots()) {
                phatLoots.clean(null);
            }
            sender.sendMessage("§5All Chests in each PhatLoot have been cleaned.");
        }
        return true;
    }

    @CodCommand(
        command = "reload",
        weight = 180,
        aliases = {"rl"},
        usage = {
            "§2<command>§b Reset looted times for target Block",
            "§2<command> <Name>§b Reset looted times for PhatLoot",
            "§2<command> all§b Reset looted times for all PhatLoots"
        },
        permission = "phatloots.reload"
    )
    public boolean reload(CommandSender sender) {
        PhatLoots.rl(sender);
        return true;
    }

    @CodCommand(
            command = "save",
            weight = 190,
            usage = "§2<command> <Name> §b Saves the given loot table",
            permission = "phatloots.save"
    )
    public boolean save(CommandSender sender, PhatLoot phatLoot) {
        phatLoot.saveAll();
        sender.sendMessage(ChatColor.GREEN + "Saved " + ChatColor.GOLD + phatLoot.getName() + ChatColor.GREEN + " successfully!");
        return true;
    }

    @CodCommand(
            command = "saveall",
            weight = 200,
            usage = "§2<command>§b Saves all loot tables.",
            permission = "phatloots.save.all"
    )
    public boolean saveAll(CommandSender sender) {
        PhatLoots.saveAll();
        sender.sendMessage(ChatColor.GREEN + "Saved all loot tables successfully!");
        return true;
    }
}
