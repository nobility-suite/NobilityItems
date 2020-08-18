package net.civex4.nobilityitems;

import java.io.File;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class NobilityItems extends JavaPlugin {
    private static NobilityItems instance;

    @Override
    public void onEnable() {
        instance = this;

        ItemManager.init(new File(getDataFolder(), "items.yml"));
    }

    protected static NobilityItems getInstance() {
        return instance;
    }

    /**
     * Gets a NobilityItem from its internal name (the one used
     * in the items.yml config section header)
     * 
     * @param internalName String
     * @return
     */
    public static NobilityItem getItem(String internalName) {
        return ItemManager.getItem(internalName);
    }

    /**
     * Attempts to get a NobilityItem from an ItemStack. Returns
     * null if there are no valid NobilityItems
     * 
     * @param item ItemStack
     * @return
     */
    public static NobilityItem getItem(ItemStack item) {
        return ItemManager.getItem(item);
    }

}