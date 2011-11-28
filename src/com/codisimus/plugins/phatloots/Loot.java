package com.codisimus.plugins.phatloots;

import org.bukkit.inventory.ItemStack;

/**
 * A Loot is a ItemStack and with a probability of looting
 * 
 * @author Codisimus
 */
public class Loot {
    public ItemStack item;
    public int probability;

    /**
     * Constructs a new Loot with the given Item data
     * 
     * @param id The Material id of the item
     * @param durability The durability of the item
     * @param amount The stack size of the item
     * @param probability The chance of looting the item
     */
    public Loot (int id, short durability, int amount, int probability) {
        item = new ItemStack(id, amount);
        if (durability >= 0)
            item.setDurability(durability);
        
        this.probability = probability;
    }

    /**
     * Returns the String representation of this Loot
     * The format of the returned String is MaterialID'Durability'Amount'Probability
     * 
     * @return The String representation of this Loot
     */
    @Override
    public String toString() {
        return item.getTypeId()+"'"+item.getDurability()+"'"+item.getAmount()+"'"+probability;
    }
}