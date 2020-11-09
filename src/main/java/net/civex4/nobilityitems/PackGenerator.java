package net.civex4.nobilityitems;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

public class PackGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Map<Material, List<NobilityItem>> items;

    private static Path itemModelFolder;
    private static Path itemTextureFolder;
    private static Configuration config;

    private static void generateStructure() throws IOException {
        Path dataFolder = NobilityItems.getInstance().getDataFolder().toPath();

        NobilityItems.getInstance().saveResource("pack/pack.mcmeta", true);
        NobilityItems.getInstance().saveResource("pack/pack.png", true);

        itemModelFolder = dataFolder.resolve("pack/assets/minecraft/models/item");
        itemTextureFolder = dataFolder.resolve("pack/assets/minecraft/textures/item");

        Files.createDirectories(itemModelFolder);
        Files.createDirectories(itemTextureFolder);

        for (Path child : (Iterable<Path>) Files.list(itemModelFolder)::iterator) {
            Files.deleteIfExists(child);
        }
        for (Path child : (Iterable<Path>) Files.list(itemTextureFolder)::iterator) {
            Files.deleteIfExists(child);
        }
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

    private static void overwrite(Path file, String string) {
        try {
            Files.write(file, string.getBytes());
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to write to file: " + file.getFileName());
            e.printStackTrace();
        }
    }

    public static void generate() throws IOException {
        if (!grabItems()) {
            return;
        }

        generateStructure();

        InputStream texturesYml = NobilityItems.getInstance().getResource("textures.yml");
        if (texturesYml == null) {
            throw new IOException("Could not find textures.yml");
        }
        InputStreamReader reader = new InputStreamReader(texturesYml);
        config = YamlConfiguration.loadConfiguration(reader);

        for (Material type : items.keySet()) {
            if (!config.contains(type.name())) {
                Bukkit.getLogger().severe("Cannot generate for type " + type.name() + "! try a different Material?");
                continue;
            }

            String materialKey = type.name();

            Path file = itemModelFolder.resolve(materialKey.toLowerCase() + ".json");

            String parent = config.getString(materialKey + ".parent");

            ItemModel toWrite = new ItemModel();
            toWrite.parent = parent;
            toWrite.textures = new ItemModel.Textures();
            toWrite.textures.layer0 = config.getString(materialKey + ".layer0");
            toWrite.overrides = new ArrayList<>();

            for (NobilityItem item : items.get(type)) {
                Path itemModelFile = itemModelFolder.resolve(item.getInternalName() + ".json");
                ItemModel itemModel = new ItemModel();
                itemModel.parent = parent;
                itemModel.textures = new ItemModel.Textures();
                itemModel.textures.layer0 = "item/" + item.getInternalName();
                overwrite(itemModelFile, GSON.toJson(itemModel));

                Path itemTextureFile = itemTextureFolder.resolve(item.getInternalName() + ".png");
                if (!Files.exists(itemTextureFile)) {
                    try {
                        InputStream inStream = NobilityItems.getInstance().getResource("pack/default.png");
                        if (inStream == null) {
                            throw new IOException("Could not find pack/default.png");
                        }
                        Files.copy(inStream, itemTextureFile, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        Bukkit.getLogger().severe("Unable to create file " + item.getInternalName() + ".png in textures/item folder");
                        e.printStackTrace();
                        continue;
                    }
                }

                ItemModel.ModelOverride override = new ItemModel.ModelOverride();
                override.predicate = new ItemModel.ModelOverride.Predicate();
                override.predicate.custom_model_data = item.getCustomModelData();
                override.model = "item/" + item.getInternalName();
                toWrite.overrides.add(override);
            }

            overwrite(file, GSON.toJson(toWrite));
        }
    }

    @SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
    private static class ItemModel {
        private String parent;
        private Textures textures;
        private List<ModelOverride> overrides;

        private static class Textures {
            private String layer0;
        }

        private static class ModelOverride {
            private Predicate predicate;
            private String model;

            private static class Predicate {
                private int custom_model_data;
            }
        }
    }
    
}
