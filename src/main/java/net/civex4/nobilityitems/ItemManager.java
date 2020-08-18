package net.civex4.nobilityitems;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemManager {
    private static Map<String, NobilityItem> items;
    private static File file;
    private static FileConfiguration config;

    protected static void init(File configFile) {
        file = configFile;

        if (!configFile.exists()) {
            NobilityItems.getInstance().saveResource("items.yml", false);
        }

        try {
            config = YamlConfiguration.loadConfiguration(configFile);
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().severe("Failed to load NobilityItems config!");
            e.printStackTrace();
            return;
        }

        Bukkit.getLogger().info("Loading Items...");

        items = new HashMap<>();

        for (String internalName : config.getKeys(false)) {
            ConfigurationSection itemConfig = config.getConfigurationSection(internalName);
            String displayName = null;
            Material material = null;
            List<String> lore = null;
            int model = -1;

            boolean cont = false;

            if (itemConfig.isString("display_name")) {
                displayName = itemConfig.getString("display_name").replace('&', '§');
            } else {
                Bukkit.getLogger().severe(itemConfig.getCurrentPath() + " has no display_name!");
                cont = true;
            }

            if (itemConfig.isString("material")) {
                try {
                    material = Material.valueOf(itemConfig.getString("material"));
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().severe(
                            itemConfig.getCurrentPath() + " has invalid material " + itemConfig.getString("material"));
                    cont = true;
                }
            } else {
                Bukkit.getLogger().severe(itemConfig.getCurrentPath() + " has no material!");
                cont = true;
            }

            if (itemConfig.isList("lore")) {
                lore = itemConfig.getStringList("lore");
                lore.replaceAll(string -> string.replace('&', '§'));
            }

            if (itemConfig.isInt("model")) {
                model = itemConfig.getInt("model");
            }

            if (cont) {
                Bukkit.getLogger().severe("Unable to load " + itemConfig.getCurrentPath());
                continue;
            }

            items.put(internalName, new NobilityItem(internalName, displayName, material, lore, model));
        }
    }

    public static boolean makeItem(String internalName, ItemStack item) {
        if (items.keySet().contains(internalName) || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (!meta.hasDisplayName()) {
            return false;
        }

        Material material = item.getType();
        String displayName = meta.getDisplayName();
        List<String> lore = meta.hasLore() ? meta.getLore() : null;
        int model = meta.hasCustomModelData() ? meta.getCustomModelData() : -1;

        ConfigurationSection itemConfig = config.createSection(internalName);
        itemConfig.set("display_name", displayName);
        itemConfig.set("material", material.name());
        if (meta.hasLore())
            itemConfig.set("lore", lore);
        if (meta.hasCustomModelData())
            itemConfig.set("model", model);

        items.put(internalName, new NobilityItem(internalName, displayName, material, lore, model));

        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Unable to save config!");
            e.printStackTrace();
        }

        return true;
    }

    public static List<NobilityItem> getItems() {
        return new ArrayList<NobilityItem>(items.values());
    }

    public static NobilityItem getItem(String internalName) {
        NobilityItem item = items.get(internalName);

        if (item == null) {
            throw new IllegalArgumentException(internalName + " is not a valid NobilityItem!");
        }

        return item;
    }

    public static NobilityItem getItem(ItemStack itemStack) {
        for (NobilityItem item : items.values()) {
            if (item.equals(itemStack)) {
                return item;
            }
        }

        return null;
    }
}