package net.civex4.nobilityitems;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.civex4.nobilityitems.impl.ModelPartitioner;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

public class PackGenerator {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static Map<Material, List<NobilityItem>> items;
    private static Map<Material, List<NobilityBlock>> blocks;

    private static Path itemModelFolder;
    private static Path itemTextureFolder;
    private static Path blockstatesFolder;
    private static Path blockModelFolder;
    private static Path blockTextureFolder;
    private static Path blockModelSourceFolder;
    private static Configuration config;

    private static void generateStructure() throws IOException {
        Path dataFolder = NobilityItems.getInstance().getDataFolder().toPath();

        NobilityItems.getInstance().saveResource("pack/pack.mcmeta", true);
        NobilityItems.getInstance().saveResource("pack/pack.png", true);

        itemModelFolder = dataFolder.resolve("pack/assets/minecraft/models/item");
        itemTextureFolder = dataFolder.resolve("pack/assets/minecraft/textures/item");
        blockstatesFolder = dataFolder.resolve("pack/assets/minecraft/blockstates");
        blockModelFolder = dataFolder.resolve("pack/assets/minecraft/models/block");
        blockTextureFolder = dataFolder.resolve("pack/assets/minecraft/textures/block");
        blockModelSourceFolder = dataFolder.resolve("pack/assets/minecraft/block_model_source");

        Files.createDirectories(itemTextureFolder);
        Files.createDirectories(blockTextureFolder);
        Files.createDirectories(itemModelFolder);
        Files.createDirectories(blockModelFolder);
        Files.createDirectories(blockstatesFolder);
        Files.createDirectories(blockModelSourceFolder);

        for (Path child : (Iterable<Path>) Files.list(blockstatesFolder)::iterator) {
            Files.deleteIfExists(child);
        }
        for (Path child : (Iterable<Path>) Files.list(blockModelFolder)::iterator) {
            Files.deleteIfExists(child);
        }
    }

    private static boolean grabItems() {
        items = new HashMap<>();

        for (NobilityItem item : NobilityItems.getItems()) {
            if (item.hasCustomModel()) {
                items.computeIfAbsent(item.getMaterial(), k -> new ArrayList<>()).add(item);
            }
        }

        return items.keySet().size() > 0;
    }

    private static boolean grabBlocks() {
        blocks = new HashMap<>();

        for (NobilityBlock block : NobilityItems.getBlocks()) {
            blocks.computeIfAbsent(block.getBlockData().getMaterial(), k -> new ArrayList<>()).add(block);
        }

        return !blocks.isEmpty();
    }

    private static void overwrite(Path file, String string) {
        try {
            Files.write(file, string.getBytes());
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to write to file: " + file.getFileName());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<String, T> stringToProps(String props) {
        if (props.isEmpty()) {
            return new TreeMap<>();
        }
        return Arrays.stream(props.split(","))
                .map(p -> p.split("="))
                .collect(Collectors.groupingBy(propAndVal -> propAndVal[0], TreeMap::new, Collectors.reducing(null, propAndVal -> (T) propAndVal[1], (a, b) -> a == null ? b : a)));
    }

    private static <T> String propsToString(Map<String, T> props) {
        if (props.isEmpty()) {
            return "";
        }
        return props.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + propValToString(entry.getValue()))
                .collect(Collectors.joining(","));
    }

    private static String propValToString(Object entry) {
        return entry instanceof Enum ? ((Enum<?>) entry).name().toLowerCase(Locale.ROOT) : String.valueOf(entry);
    }

    private static Predicate<Map<String, String>> parseSimpleMultipartCondition(JsonObject condition) {
        Map<String, Set<String>> allowedValues = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : condition.entrySet()) {
            allowedValues.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue().getAsString().split("\\|")));
        }
        return properties -> {
            for (Map.Entry<String, Set<String>> entry : allowedValues.entrySet()) {
                if (!entry.getValue().contains(properties.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> void patchBlockstate(Material material, List<NobilityBlock> blocks) throws IOException {
        String blockName = material.name().toLowerCase(Locale.ROOT);
        InputStream inStream = NobilityItems.getInstance().getResource("blockstates/" + blockName + ".json");
        if (inStream == null) {
            throw new IOException("Unobtainable block \"" + blockName + "\" does not have a blockstate file to patch");
        }

        Map<String, List<T>> allProperties = (Map<String, List<T>>) (Map<?, ?>) UnobtainableBlocks.getAllProperties(material);

        JsonObject blockstate = GSON.fromJson(new InputStreamReader(inStream, StandardCharsets.UTF_8), JsonObject.class);
        if (blockstate.has("variants")) {
            JsonObject variants = blockstate.getAsJsonObject("variants");
            JsonObject newVariants = new JsonObject();

            for (Map.Entry<String, JsonElement> entry : variants.entrySet()) {
                List<Map<String, T>> combinations = new ArrayList<>();
                combinations.add(stringToProps(entry.getKey()));
                for (String prop : allProperties.keySet()) {
                    Map<String, T> combo = combinations.get(0);
                    if (!combo.containsKey(prop)) {
                        int num = combinations.size();
                        for (T val : allProperties.get(prop)) {
                            for (int i = 0; i < num; i++) {
                                Map<String, T> newCombo = new TreeMap<>(combinations.get(i));
                                newCombo.put(prop, val);
                                combinations.add(newCombo);
                            }
                        }
                        combinations.subList(0, num).clear();
                    }
                }

                for (Map<String, T> combo : combinations) {
                    newVariants.add(propsToString(combo), entry.getValue());
                }
            }

            for (NobilityBlock block : blocks) {
                JsonObject model = new JsonObject();
                model.addProperty("model", "minecraft:block/" + block.getInternalName());
                String str = block.getBlockData().getAsString(false);
                newVariants.add(str.substring(str.indexOf('[') + 1, str.length() - 1), model);
            }

            blockstate.add("variants", newVariants);
        } else {
            JsonArray multipart = blockstate.getAsJsonArray("multipart");
            JsonArray newMultipart = new JsonArray();

            List<Map<String, String>> combinations = new ArrayList<>();
            combinations.add(new TreeMap<>());
            for (String prop : allProperties.keySet()) {
                int num = combinations.size();
                for (T val : allProperties.get(prop)) {
                    String strVal = propValToString(val);
                    for (int i = 0; i < num; i++) {
                        Map<String, String> newCombo = new TreeMap<>(combinations.get(i));
                        newCombo.put(prop, strVal);
                        combinations.add(newCombo);
                    }
                }
                combinations.subList(0, num).clear();
            }

            Set<String> customProperties = blocks.stream()
                    .map(block -> block.getBlockData().getAsString(false))
                    .map(str -> str.substring(str.indexOf('[') + 1, str.length() - 1))
                    .collect(Collectors.toSet());

            for (JsonElement elem : multipart) {
                JsonObject part = elem.getAsJsonObject();
                JsonObject when = part.getAsJsonObject("when");
                Predicate<Map<String, String>> condition;
                if (when.size() == 1 && when.has("OR")) {
                    List<Predicate<Map<String, String>>> subConditions = new ArrayList<>();
                    for (JsonElement subCondition : when.getAsJsonArray("OR")) {
                        subConditions.add(parseSimpleMultipartCondition(subCondition.getAsJsonObject()));
                    }
                    condition = properties -> subConditions.stream().anyMatch(subCondition -> subCondition.test(properties));
                } else if (when.size() == 1 && when.has("AND")) {
                    List<Predicate<Map<String, String>>> subConditions = new ArrayList<>();
                    for (JsonElement subCondition : when.getAsJsonArray("AND")) {
                        subConditions.add(parseSimpleMultipartCondition(subCondition.getAsJsonObject()));
                    }
                    condition = properties -> subConditions.stream().allMatch(subCondition -> subCondition.test(properties));
                } else {
                    condition = parseSimpleMultipartCondition(when);
                }

                JsonObject newPart = new JsonObject();
                JsonObject newWhen = new JsonObject();
                JsonArray or = new JsonArray();
                for (Map<String, String> combo : combinations) {
                    if (condition.test(combo) && !customProperties.contains(propsToString(combo))) {
                        JsonObject conditions = new JsonObject();
                        combo.forEach(conditions::addProperty);
                        or.add(conditions);
                    }
                }
                newWhen.add("OR", or);
                newPart.add("when", newWhen);
                newPart.add("apply", part.get("apply"));
                newMultipart.add(newPart);
            }

            for (NobilityBlock block : blocks) {
                JsonObject rule = new JsonObject();

                JsonObject when = new JsonObject();
                String str = block.getBlockData().getAsString(false);
                for (Map.Entry<String, String> entry : PackGenerator.<String>stringToProps(str.substring(str.indexOf('[') + 1, str.length() - 1)).entrySet()) {
                    when.addProperty(entry.getKey(), entry.getValue());
                }
                rule.add("when", when);

                JsonObject apply = new JsonObject();
                apply.addProperty("model", "minecraft:block/" + block.getInternalName());
                rule.add("apply", apply);

                newMultipart.add(rule);
            }

            blockstate.add("multipart", newMultipart);
        }

        overwrite(blockstatesFolder.resolve(blockName + ".json"), GSON.toJson(blockstate));
    }

    private static void makeZip() throws IOException {
        Path p = NobilityItems.getInstance().getDataFolder().toPath().resolve("pack.zip");
        Files.deleteIfExists(p);
        Files.createFile(p);
        Path pp = NobilityItems.getInstance().getDataFolder().toPath().resolve("pack");
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p));
             Stream<Path> paths = Files.walk(pp)) {
            paths
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString().replace(File.separatorChar, '/'));
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    public static void generate() throws IOException {
        if (!grabItems() & !grabBlocks()) {
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
                Path itemModelFile = itemModelFolder.resolve(item.getModel() + ".json");
                if (!Files.exists(itemModelFile)) {
                    ItemModel itemModel = new ItemModel();
                    if (item.hasBlock()) {
                        itemModel.parent = "block/" + item.getInternalName();
                    } else {
                        itemModel.parent = parent;
                        itemModel.textures = new ItemModel.Textures();
                        itemModel.textures.layer0 = "item/" + item.getModel();
                    }
                    overwrite(itemModelFile, GSON.toJson(itemModel));
                }

                Path itemTextureFile = itemTextureFolder.resolve(item.getModel() + ".png");
                if (!Files.exists(itemTextureFile)) {
                    try {
                        InputStream inStream = NobilityItems.getInstance().getResource("pack/default.png");
                        if (inStream == null) {
                            throw new IOException("Could not find pack/default.png");
                        }
                        Files.copy(inStream, itemTextureFile, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        Bukkit.getLogger().severe("Unable to create file " + item.getModel() + ".png in textures/item folder");
                        e.printStackTrace();
                        continue;
                    }
                }

                ItemModel.ModelOverride override = new ItemModel.ModelOverride();
                override.predicate = new ItemModel.ModelOverride.Predicate();
                override.predicate.custom_model_data = item.getCustomModelData();
                override.model = "item/" + item.getModel();
                toWrite.overrides.add(override);
            }

            overwrite(file, GSON.toJson(toWrite));
        }

        for (Material type : blocks.keySet()) {
            List<NobilityBlock> nobilityBlocks = PackGenerator.blocks.get(type);
            patchBlockstate(type, nobilityBlocks);

            for (NobilityBlock block : nobilityBlocks) {
                Path blockModelFile = blockModelSourceFolder.resolve(block.getInternalName() + ".json");
                BlockModelSource model;
                if (!Files.exists(blockModelFile)) {
                    model = new BlockModelSource();
                    model.parent = "block/cube_all";
                    model.textures = ImmutableMap.of("all", "block/" + block.getInternalName());
                    overwrite(blockModelFile, GSON.toJson(model));
                } else {
                    try {
                        model = GSON.fromJson(Files.newBufferedReader(blockModelFile, StandardCharsets.UTF_8), BlockModelSource.class);
                    } catch (JsonSyntaxException e) {
                        Bukkit.getLogger().log(Level.WARNING, "Block model for " + block.getInternalName() + " has a syntax error", e);
                        continue;
                    }
                }
                if (model.textures != null) {
                    for (String texture : model.textures.values()) {
                        if (texture.startsWith("block/")) {
                            texture = texture.substring(6);
                        }
                        Path blockTextureFile = blockTextureFolder.resolve(texture + ".png");
                        if (!Files.exists(blockTextureFile)) {
                            try {
                                InputStream inStream = NobilityItems.getInstance().getResource("pack/default.png");
                                if (inStream == null) {
                                    throw new IOException("Could not find pack/default.png");
                                }
                                Files.copy(inStream, blockTextureFile, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                Bukkit.getLogger().severe("Unable to create file " + block.getInternalName() + ".png in textures/block folder");
                                e.printStackTrace();
                            }
                        }
                    }
                }

                Path generatedModelFile = blockModelFolder.resolve(block.getInternalName() + ".json");
                if (model.nobility_transparent != null && model.nobility_transparent) {
                    ModelPartitioner.partitionModel(blockModelFile, generatedModelFile, NobilityItems.getInstance().getDataFolder().toPath().resolve("pack"), NobilityItems.getInstance().getDataFolder());
                } else {
                    Files.copy(blockModelFile, generatedModelFile);
                }
            }
        }

        makeZip();
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

    @SuppressWarnings("unused")
    private static class BlockModelSource {
        private String parent;
        private Map<String, String> textures;

        private Boolean nobility_transparent;
    }
    
}
