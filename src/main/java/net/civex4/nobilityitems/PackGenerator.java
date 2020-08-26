package net.civex4.nobilityitems;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

public class PackGenerator {

    private static Map<Material, List<NobilityItem>> items;

    private static File itemModelFolder;
    private static File itemTextureFolder;
    private static InputStream nobilityItemTexture;
    private static Configuration config;

    private static void generateStructure() {
        File dataFolder = NobilityItems.getInstance().getDataFolder();

        NobilityItems.getInstance().saveResource("pack/pack.mcmeta", true);
        NobilityItems.getInstance().saveResource("pack/pack.png", true);

        nobilityItemTexture = NobilityItems.getInstance().getResource("pack/default.png");

        itemModelFolder = new File(dataFolder, "pack/assets/minecraft/models/item");
        itemTextureFolder = new File(dataFolder, "pack/assets/minecraft/textures/item");

        itemModelFolder.mkdirs();
        itemTextureFolder.mkdirs();

        Arrays.asList(itemModelFolder.listFiles()).forEach(file -> file.delete());
        Arrays.asList(itemTextureFolder.listFiles()).forEach(file -> file.delete());
    }

    private static boolean grabItems() {
        items = new HashMap<>();

        for (NobilityItem item : NobilityItems.getItems()) {
            if (item.hasCustomModelData()) {
                if (!items.containsKey(item.getMaterial())) {
                    items.put(item.getMaterial(), new ArrayList<>());
                }

                items.get(item.getMaterial()).add(item);
            }
        }

        return items.keySet().size() > 0;
    }

    private static void overwrite(File file, String string) {
        file.delete();
        try {
            Files.write(Paths.get(file.getPath()), string.getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to write to file: " + file.getName());
            e.printStackTrace();
        }
    }

    public static void generate() {
        if (!grabItems()) {
            return;
        }

        generateStructure();

        InputStreamReader reader = new InputStreamReader(NobilityItems.getInstance().getResource("textures.yml"));
        config = YamlConfiguration.loadConfiguration(reader);

        for (Material type : items.keySet()) {
            if (!config.contains(type.name())) {
                Bukkit.getLogger().severe("Cannot generate for type " + type.name() + "! try a different Material?");
                continue;
            }

            String materialKey = type.name();

            File file = new File(itemModelFolder, materialKey.toLowerCase() + ".json");

            if (file.exists()) {
                file.delete();
            }

            try {
                file.createNewFile();
            } catch (IOException e) {
                Bukkit.getLogger().severe("Unable to create file " + file.getName() + " in models/item folder");
                e.printStackTrace();
                continue;
            }

            String parent = config.getString(materialKey + ".parent");
            String layer0 = config.getString(materialKey + ".layer0");

            String toWrite = "{\n\t\"parent\": \"" + parent + "\",\n\t\"textures\": {\n\t\t\"layer0\": \"" + layer0 + "\"\n\t},\n\t\"overrides\": [";

            for (NobilityItem item : items.get(type)) {
                File itemModelFile = new File(itemModelFolder, item.getInternalName() + ".json");
                String itemModelFileText = "{ \"parent\": \"" + parent
                    + "\", \"textures\": { \"layer0\": \"item/" + item.getInternalName() + "\" } }";
                overwrite(itemModelFile, itemModelFileText);

                try {
                    File itemTextureFile = new File(itemTextureFolder, item.getInternalName() + ".png");
                    itemTextureFile.createNewFile();
                    InputStream inStream = NobilityItems.getInstance().getResource("pack/default.png");
                    byte[] buffer = new byte[inStream.available()];
                    inStream.read(buffer);
                    inStream.close();

                    OutputStream outStream = new FileOutputStream(itemTextureFile);
                    outStream.write(buffer);
                    outStream.close();
                } catch (IOException e) {
                    Bukkit.getLogger().severe("Unable to create file " + item.getInternalName() + ".png in textures/item folder");
                    e.printStackTrace();
                    continue;
                }

                toWrite += "\n\t\t{ \"predicate\": { \"custom_model_data\": " + item.getCustomModelData() 
                    + " }, \"model\": \"item/" + item.getInternalName() + "\" },";
            }

            toWrite = toWrite.substring(0, toWrite.length() - 1);
            toWrite += "\n\t]\n}";
            overwrite(file, toWrite);
        }
    }
    
}