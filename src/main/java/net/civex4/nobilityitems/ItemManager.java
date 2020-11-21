package net.civex4.nobilityitems;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

class ItemManager {
    private static Map<String, NobilityItem> items;
    private static List<NobilityItem> itemList;
    private static Map<String, List<String>> tags;
    private static FileConfiguration tagsConfig;
    private static File file;
    private static FileConfiguration itemsConfig;
    private static List<FileConfiguration> configs;
    private static Map<Material, BiMap<String, Integer>> customModelDatas;
    private static File customModelDataFile;

    static void init(File itemFolder, File tagsFile) {
        Bukkit.getLogger().info("Loading tags...");
        tags = new HashMap<>();

        if (!tagsFile.exists()) {
            NobilityItems.getInstance().saveResource("tags.yml", false);
        }

        try {
            tagsConfig = YamlConfiguration.loadConfiguration(tagsFile);
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().severe("Failed to load tags.yml!");
            e.printStackTrace();
        }

        for (String tag : tagsConfig.getKeys(false)) {
            if (tagsConfig.isString(tag)) {
                List<String> list = new ArrayList<>();
                list.add(tagsConfig.getString(tag));
                tags.put(tag, list);
            } else if (tagsConfig.isList(tag)) {
                tags.put(tag, tagsConfig.getStringList(tag));
            } else {
                Bukkit.getLogger().severe("Unable to load '" + tag + "' tag!");
            }
        }

        Bukkit.getLogger().info("Tags loaded!");

        customModelDatas = new HashMap<>();
        customModelDataFile = new File(NobilityItems.getInstance().getDataFolder(), "customModelData.yml");
        if (customModelDataFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(customModelDataFile);
            config.getKeys(false).forEach(materialKey -> {
                Material material = Material.getMaterial(materialKey);
                if (material != null) {
                    BiMap<String, Integer> materialModels = HashBiMap.create();
                    customModelDatas.put(material, materialModels);
                    ConfigurationSection materialSection = config.getConfigurationSection(materialKey);
                    assert materialSection != null;
                    materialSection.getKeys(false).forEach(model -> materialModels.put(model, materialSection.getInt(model)));
                }
            });
        }

        file = new File(itemFolder, "items.yml");

        if (!file.exists()) {
            NobilityItems.getInstance().saveResource("items/items.yml", false);
        }

        Bukkit.getLogger().info("Loading Items...");

        File[] fileArray = itemFolder.listFiles();
        List<File> files;
        if (fileArray == null) {
            files = new ArrayList<>(0);
        } else {
            files = new ArrayList<>(fileArray.length);
            Collections.addAll(files, fileArray);
        }
        configs = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    Collections.addAll(files, children);
                }
            } else {
                try {
                    if (file.getName().equals("items.yml")) {
                        itemsConfig = YamlConfiguration.loadConfiguration(file);
                    }
                    configs.add(YamlConfiguration.loadConfiguration(file));
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().severe("Failed to load " + file.getName() + "!");
                    e.printStackTrace();
                }
            }
        }

        items = new LinkedHashMap<>();
        itemList = null;
        for (FileConfiguration config : configs) {
            for (String internalName : config.getKeys(false)) {
                ConfigurationSection itemConfig = config.getConfigurationSection(internalName);
                if (itemConfig == null) continue;
                Material material = null;
                List<String> lore = null;

                boolean cont = false;

                String displayName = itemConfig.getString("display_name");
                if (displayName != null) {
                    displayName = displayName.replace('&', '§');
                } else {
                    Bukkit.getLogger().severe(itemConfig.getCurrentPath() + " has no display_name!");
                    cont = true;
                }

                if (itemConfig.isString("material")) {
                    try {
                        material = Material.valueOf(itemConfig.getString("material"));
                    } catch (IllegalArgumentException e) {
                        Bukkit.getLogger().severe(itemConfig.getCurrentPath() + " has invalid material "
                                + itemConfig.getString("material"));
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

                if (itemConfig.isList("tags")) {
                    List<String> tagsList = itemConfig.getStringList("tags");
                    List<String> toAdd = new ArrayList<>();
                    if (lore != null)
                        toAdd.add("");

                    for (String tag : tagsList) {
                        if (tags.containsKey(tag)) {
                            toAdd.addAll(tags.get(tag));
                        } else {
                            Bukkit.getLogger().severe("Invalid tag " + tag + " in " + itemConfig.getCurrentPath());
                            cont = true;
                        }
                    }

                    toAdd.replaceAll(string -> string.replace('&', '§'));

                    if (lore == null) 
                        lore = new ArrayList<>();
                    
                    lore.addAll(toAdd);
                }

                if (cont) {
                    Bukkit.getLogger().severe("Unable to load " + itemConfig.getCurrentPath());
                    continue;
                }

                String model = itemConfig.getString("model");
                if (model != null) {
                    BiMap<String, Integer> modelData = customModelDatas.computeIfAbsent(material, k -> HashBiMap.create());
                    int modelId = modelData.size();
                    while (modelData.containsValue(modelId)) {
                        modelId++;
                    }
                    modelData.put(model, modelId);
                }

                items.put(internalName, new NobilityItem(internalName, displayName, material, lore, model));
            }
        }

        Bukkit.getLogger().info("Items Loaded!");

        save();
    }

    static boolean makeItem(String internalName, ItemStack item) {
        if (items.containsKey(internalName) || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        Material material = item.getType();
        String displayName = meta.getDisplayName();
        List<String> lore = meta.hasLore() ? meta.getLore() : null;
        int model = meta.hasCustomModelData() ? meta.getCustomModelData() : -1;

        ConfigurationSection itemConfig = itemsConfig.createSection(internalName);
        itemConfig.set("display_name", displayName);
        itemConfig.set("material", material.name());
        if (meta.hasLore())
            itemConfig.set("lore", lore);
        if (meta.hasCustomModelData())
            itemConfig.set("model", model);

        BiMap<String, Integer> modelData = customModelDatas.get(material);
        items.put(internalName, new NobilityItem(internalName, displayName, material, lore, modelData == null ? null : modelData.inverse().get(model)));
        itemList = null;

        save();

        return true;
    }

    static void save() {
        try {
            itemsConfig.save(file);

            YamlConfiguration customModelDataConfig = new YamlConfiguration();
            customModelDataConfig.options().header("This is a auto-generated file. Do not edit!");
            for (Map.Entry<Material, BiMap<String, Integer>> materialData : customModelDatas.entrySet()) {
                ConfigurationSection materialSection = customModelDataConfig.createSection(materialData.getKey().name().toLowerCase(Locale.ROOT));
                for (Map.Entry<String, Integer> customModelData : materialData.getValue().entrySet()) {
                    materialSection.set(customModelData.getKey(), customModelData.getValue());
                }
            }
            customModelDataConfig.save(customModelDataFile);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Unable to save config!");
            e.printStackTrace();
        }
    }

    static int getModelData(Material material, String model) {
        Map<String, Integer> modelData = customModelDatas.get(material);
        return modelData == null ? -1 : modelData.getOrDefault(model, -1);
    }

    static List<NobilityItem> getItems() {
        if (itemList == null) {
            itemList = new ArrayList<>(items.values());
        }
        return itemList;
    }

    static NobilityItem getItemByDisplayName(String displayName) {
        for (NobilityItem item : getItems()) {
            if (item.getDisplayName().equals(displayName)) {
                return item;
            }
        }

        return null;
    }

    static NobilityItem getNullableItem(String internalName) {
        return items.get(internalName);
    }

    static NobilityItem getItem(String internalName) {
        NobilityItem item = items.get(internalName);

        if (item == null) {
            throw new IllegalArgumentException(internalName + " is not a valid NobilityItem!");
        }

        return item;
    }

    static NobilityItem getItem(ItemStack itemStack) {
        for (NobilityItem item : getItems()) {
            if (item.equalsStack(itemStack)) {
                return item;
            }
        }

        return null;
    }
}
