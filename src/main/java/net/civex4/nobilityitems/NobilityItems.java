package net.civex4.nobilityitems;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.bukkit.block.data.BlockData;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class NobilityItems extends JavaPlugin {
    private static NobilityItems instance;

    @Override
    public void onEnable() {
        instance = this;

        PluginCommand nobilityCommand = getCommand("nobilityitems");
        assert nobilityCommand != null;
        nobilityCommand.setExecutor(new CommandListener());
        nobilityCommand.setTabCompleter(new CommandTabCompleter());

        ItemManager.init(new File(getDataFolder(), "items"), new File(getDataFolder(), "tags.yml"));
        BlockManager.init();
    }

    protected static NobilityItems getInstance() {
        return instance;
    }

    /**
     * Gets a NobilityItem from its DisplayName (what the
     * ItemStack itself is called). If multiple items have
     * the same display name this will just get the first
     * one that matches.
     * 
     * @param displayName The display name
     * @return NobilityItem, null if no DisplayName matches
     */
    public static NobilityItem getItemByDisplayName(String displayName) {
        return ItemManager.getItemByDisplayName(displayName);
    }

    /**
     * Gets a NobilityItem from its internal name (the one used
     * in the items.yml config section header). Throws an
     * IllegalArgumentException if the internal name does not
     * align with any NobilityItem!
     * 
     * @param internalName String
     * @return NobilityItem
     */
    public static NobilityItem getItemByName(String internalName) {
        return ItemManager.getItem(internalName);
    }

    /**
     * Same as {@link #getItemByName(String)}, but returns null
     * instead of throwing an exception if there was no such NobilityItem.
     */
    public static NobilityItem getNullableItemByName(String internalName) {
        return ItemManager.getNullableItem(internalName);
    }

    /**
     * Attempts to get a NobilityItem from an ItemStack.
     * 
     * @param item ItemStack
     * @return NobilityItem, null if no valid NobilityItems for this ItemStack
     */
    public static NobilityItem getItem(ItemStack item) {
        return ItemManager.getItem(item);
    }

    /**
     * Returns a List of all loaded NobilityItems
     * 
     * @return List<NobilityItem>
     */
    public static List<NobilityItem> getItems() {
        return Collections.unmodifiableList(ItemManager.getItems());
    }

    /**
     * Gets a NobilityBlock from its internal name (the one used
     * in the blocks.yml config section header). Throws an
     * IllegalArgumentException if the internal name does not
     * align with any NobilityBlock!
     *
     * @param internalName String
     * @return NobilityItem
     */
    public static NobilityBlock getBlockByName(String internalName) {
        return BlockManager.getBlock(internalName);
    }

    /**
     * Same as {@link #getBlockByName(String)}, but returns null
     * instead of throwing an exception if there was no such NobilityBlock.
     */
    public static NobilityBlock getNullableBlockByName(String internalName) {
        return BlockManager.getNullableBlock(internalName);
    }

    /**
     * Attempts to get a NobilityBlock from a BlockData.
     *
     * @param block BlockData
     * @return NobilityBlock, null if no valid NobilityBlocks for this BlockData
     */
    public static NobilityBlock getBlock(BlockData block) {
        return BlockManager.getBlock(block);
    }

    /**
     * Returns a List of all loaded NobilityBlocks
     *
     * @return List<NobilityBlock>
     */
    public static List<NobilityBlock> getBlocks() {
        return Collections.unmodifiableList(BlockManager.getBlocks());
    }
}
