package net.civex4.nobilityitems;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * A custom item
 * 
 * @author KingVictoria
 */
public class NobilityItem {
    private String internalName;
    private String displayName;
    private Material material;
    private int customModelData;
    private List<String> lore;
    private boolean hasLore;
    private boolean hasCustomModelData;

    NobilityItem(String id, String displayName, Material material, List<String> lore, int model) {
        this.internalName = id;
        this.displayName = displayName;
        this.material = material;
        this.lore = lore;
        this.customModelData = model;

        hasLore = lore == null ? false : true;
        hasCustomModelData = customModelData != -1;
    }

    public boolean hasLore() {
        return hasLore;
    }

    public boolean hasCustomModelData() {
        return hasCustomModelData;
    }

    public String getInternalName() {
        return internalName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    /**
     * Gets the ItemStack represented by this NobilityItem
     * 
     * @param amount int amount of the ItemStack
     * 
     * @return ItemStack
     */
    public ItemStack getItemStack(int amount) {
        ItemStack item = new ItemStack(material);
        item.setAmount(amount);
        ItemMeta meta = NobilityItems.getInstance().getServer().getItemFactory().getItemMeta(material);
        meta.setDisplayName(displayName);
        if (customModelData > -1)
            meta.setCustomModelData(customModelData);
        if (lore != null)
            meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Determines whether some other Object is 'equal' to this one.
     * Will return true if the other Object is a NobilityItem with
     * the same internalName OR if it is an ItemStack that fits the
     * one described by this NobilityItem.
     * 
     * @param o Object
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof NobilityItem) {
            if (((NobilityItem) o).internalName.equals(internalName)) {
                return true;
            }
        } else if (o instanceof ItemStack && ((ItemStack) o).hasItemMeta()) {
            ItemStack item = (ItemStack) o;
            ItemMeta meta = item.getItemMeta();

            if (!meta.hasDisplayName()) {
                return false;
            }

            if (item.getType() == material && meta.getDisplayName().equals(displayName)) {
                if (meta.hasLore() == (lore != null)) {
                    if (!meta.hasLore() || meta.getLore().equals(lore)) {
                        if (customModelData < 0 || meta.getCustomModelData() == customModelData) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}