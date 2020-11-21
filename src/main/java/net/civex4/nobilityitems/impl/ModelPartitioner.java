package net.civex4.nobilityitems.impl;

import com.comphenix.protocol.utility.MinecraftVersion;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.bukkit.Bukkit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModelPartitioner {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static void partitionModel(Path modelIn, Path modelOut, Path packDir, File dataFolder) throws IOException {
        try (ZipFile clientJar = new ZipFile(downloadClient(new File(dataFolder, "client_cache")))) {
            BlockModel model = GSON.fromJson(Files.newBufferedReader(modelIn), BlockModel.class);
            if (model.parent != null) {
                Set<String> parents = new HashSet<>();
                BlockModel parent = model;
                do {
                    String parentName = parent.parent;
                    String parentNamespace;
                    if (parentName.contains(":")) {
                        String[] namespaceAndName = parentName.split(":", 2);
                        parentNamespace = namespaceAndName[0];
                        parentName = namespaceAndName[1];
                    } else {
                        parentNamespace = "minecraft";
                    }
                    if (!parents.add(parentNamespace + ":" + parent)) {
                        throw new IOException("Model has cyclic inheritance");
                    }
                    parent = GSON.fromJson(new InputStreamReader(getResource(packDir, clientJar, "assets/" + parentNamespace + "/models/" + parentName + ".json"), StandardCharsets.UTF_8), BlockModel.class);
                    if (model.elements == null) {
                        model.elements = parent.elements;
                    }
                    if (parent.textures != null) {
                        if (model.textures == null) {
                            model.textures = new LinkedHashMap<>();
                        }
                        parent.textures.forEach(model.textures::putIfAbsent);
                    }
                } while (parent.parent != null);
            }

            if (model.elements == null) {
                throw new IOException("Models must have either \"parent\" or \"elements\"");
            }

            if (model.textures != null) {
                boolean changed;
                boolean hasUnresolvedTextures;
                do {
                   changed = false;
                   hasUnresolvedTextures = false;
                   for (Map.Entry<String, String> entry : model.textures.entrySet()) {
                       if (entry.getValue().startsWith("#")) {
                           String key = entry.getValue().substring(1);
                           String value = model.textures.get(key);
                           if (value != null && !value.startsWith("#")) {
                               model.textures.put(entry.getKey(), value);
                               changed = true;
                           } else {
                               hasUnresolvedTextures = true;
                           }
                       }
                   }
                } while (changed);
                if (hasUnresolvedTextures) {
                    throw new IOException("Unresolved textures");
                }
            }

            System.out.println(GSON.toJson(model));

            List<BlockModel.Element> newElements = new ArrayList<>();

            for (BlockModel.Element element : model.elements) {
                for (Map.Entry<String, BlockModel.Element.Face> faceEntry : element.faces.entrySet()) {
                    String faceSide = faceEntry.getKey();
                    int xAxis, yAxis;
                    boolean xInverted, yInverted;
                    switch (faceSide) {
                        case "down":
                            xAxis = 0;
                            yAxis = 2;
                            xInverted = false;
                            yInverted = false;
                            break;
                        case "up":
                            xAxis = 0;
                            yAxis = 2;
                            xInverted = false;
                            yInverted = true;
                            break;
                        case "north":
                            xAxis = 0;
                            yAxis = 1;
                            xInverted = true;
                            yInverted = false;
                            break;
                        default:
                        case "south":
                            xAxis = 0;
                            yAxis = 1;
                            xInverted = false;
                            yInverted = false;
                            break;
                        case "west":
                            xAxis = 2;
                            yAxis = 1;
                            xInverted = false;
                            yInverted = false;
                            break;
                        case "east":
                            xAxis = 2;
                            yAxis = 1;
                            xInverted = true;
                            yInverted = false;
                            break;
                    }

                    BlockModel.Element.Face face = faceEntry.getValue();
                    String texture = face.texture;
                    if (!texture.startsWith("#")) {
                        throw new IOException("Texture must start with #");
                    }
                    System.out.println(texture);
                    texture = model.textures.get(texture.substring(1));
                    if (texture == null) {
                        throw new IOException("Texture not found");
                    }
                    String textureName, textureNamespace;
                    if (texture.contains(":")) {
                        String[] namespaceAndName = texture.split(":", 2);
                        textureNamespace = namespaceAndName[0];
                        textureName = namespaceAndName[1];
                    } else {
                        textureNamespace = "minecraft";
                        textureName = texture;
                    }
                    float minU, minV, maxU, maxV;
                    if (face.uv == null) {
                        minU = minV = 0;
                        maxU = maxV = 1;
                    } else {
                        minU = face.uv[0] / 16;
                        minV = face.uv[1] / 16;
                        maxU = face.uv[2] / 16;
                        maxV = face.uv[3] / 16;
                    }
                    TexturePartitioner.Result partitionResult = TexturePartitioner.partitionTexture(
                            getResource(packDir, clientJar, "assets/" + textureNamespace + "/textures/" + textureName + ".png"),
                            texture,
                            minU, minV, maxU, maxV);
                    if (partitionResult != null) {
                        for (TexturePartitioner.Rectangle rectangle : partitionResult.rectangles) {
                            BlockModel.Element.Face newFace = new BlockModel.Element.Face();
                            newFace.uv = new float[] {
                                    (float) rectangle.x1 * 16 / partitionResult.width,
                                    (float) rectangle.y1 * 16 / partitionResult.height,
                                    (float) rectangle.x2 * 16 / partitionResult.width,
                                    (float) rectangle.y2 * 16 / partitionResult.height
                            };
                            newFace.texture = face.texture;
                            newFace.cullface = face.cullface;
                            newFace.rotation = face.rotation;
                            newFace.tintindex = face.tintindex;
                            BlockModel.Element newElement = new BlockModel.Element();
                            newElement.faces = ImmutableMap.of(faceSide, newFace);
                            float xSize = element.to[xAxis] - element.from[xAxis];
                            float ySize = element.to[yAxis] - element.from[yAxis];
                            newElement.from = element.from.clone();
                            newElement.to = element.to.clone();
                            if (xInverted) {
                                newElement.from[xAxis] = element.to[xAxis] - rectangle.x2 * xSize / partitionResult.width;
                                newElement.to[xAxis] = element.to[xAxis] - rectangle.x1 * xSize / partitionResult.width;
                            } else {
                                newElement.from[xAxis] = element.from[xAxis] + rectangle.x1 * xSize / partitionResult.width;
                                newElement.to[xAxis] = element.from[xAxis] + rectangle.x2 * xSize / partitionResult.width;
                            }
                            if (yInverted) {
                                newElement.from[yAxis] = element.to[yAxis] - rectangle.y2 * ySize / partitionResult.height;
                                newElement.to[yAxis] = element.to[yAxis] - rectangle.y1 * ySize / partitionResult.height;
                            } else {
                                newElement.from[yAxis] = element.from[yAxis] + rectangle.y1 * ySize / partitionResult.height;
                                newElement.to[yAxis] = element.from[yAxis] + rectangle.y2 * ySize / partitionResult.height;
                            }
                            newElement.rotation = element.rotation;
                            newElement.shade = element.shade;
                            newElements.add(newElement);
                        }
                    }
                }
            }

            model.elements = newElements;

            try (BufferedWriter writer = Files.newBufferedWriter(modelOut, StandardCharsets.UTF_8)) {
                GSON.toJson(model, writer);
                writer.flush();
            }
        }
    }

    private static InputStream getResource(Path packDir, ZipFile clientJar, String path) throws IOException {
        Path resolved = packDir.resolve(path);
        System.out.println(resolved.toAbsolutePath());
        if (Files.exists(resolved)) {
            return Files.newInputStream(resolved);
        }
        ZipEntry zipEntry = clientJar.getEntry(path);
        if (zipEntry != null) {
            return clientJar.getInputStream(zipEntry);
        }

        throw new FileNotFoundException(path);
    }

    private static File downloadClient(File cacheDir) {
        Versions versions = downloadJson(cacheDir, "https://launchermeta.mojang.com/mc/game/version_manifest.json", "version_manifest.json", Versions.class);
        Version version = null;
        MinecraftVersion mcVersion = MinecraftVersion.getCurrentVersion();
        String mcVersionStr = mcVersion.getBuild() == 0 ? mcVersion.getMajor() + "." + mcVersion.getMinor() : mcVersion.getVersion();
        if (versions != null) {
            for (Versions.Version v : versions.versions) {
                if (mcVersionStr.equals(v.id)) {
                    version = downloadJson(cacheDir, v.url, v.id + ".json", Version.class);
                    break;
                }
            }
        }
        File clientJar = null;
        if (version != null) {
            clientJar = download(cacheDir, version.downloads.client.url, mcVersionStr + ".jar");
        }
        return clientJar;
    }

    // Downloads a json file if un-downloaded, or if it has a json syntax error (corrupted)
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static <T> T downloadJson(File cacheDir, String url, String dest, Class<T> type) {
        File downloadedFile = download(cacheDir, url, dest);
        if (downloadedFile == null) {
            return null;
        }
        try (FileReader reader = new FileReader(downloadedFile)) {
            T ret = GSON.fromJson(reader, type);
            if (ret == null) {
                throw new JsonSyntaxException("Read null");
            }
            return ret;
        } catch (JsonSyntaxException e) {
            downloadedFile.delete();
            // corrupted json, try re-downloading
            downloadedFile = download(cacheDir, url, dest, true);
            if (downloadedFile == null) {
                return null;
            }
            try (FileReader reader = new FileReader(downloadedFile)) {
                return GSON.fromJson(reader, type);
            } catch (IOException e1) {
                Bukkit.getLogger().info("Failed to read file " + dest + " downloaded from " + url);
                return null;
            }
        } catch (IOException e) {
            Bukkit.getLogger().info("Failed to read file " + dest + " downloaded from " + url);
            return null;
        }
    }

    private static File download(File cacheDir, String url, String dest) {
        return download(cacheDir, url, dest, false);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static File download(File cacheDir, String urlStr, String dest, boolean force) {
        URL url;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            Bukkit.getLogger().warning("Invalid URL: " + urlStr);
            return null;
        }

        Bukkit.getLogger().info("Downloading " + cacheDir + "/" + dest);

        File destFile = new File(cacheDir, dest);
        File etagFile = new File(cacheDir, dest + ".etag");

        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (!force && destFile.exists() && etagFile.exists()) {
                //noinspection UnstableApiUsage
                String etag = CharStreams.toString(new FileReader(etagFile));
                connection.setRequestProperty("If-None-Match", etag);
            }

            connection.setRequestProperty("Accept-Encoding", "gzip");

            connection.connect();

            int responseCode = connection.getResponseCode();
            if ((responseCode < 200 || responseCode > 299) && responseCode != HttpURLConnection.HTTP_NOT_MODIFIED) {
                throw new IOException("Got HTTP " + responseCode + " from " + url);
            }

            long lastModified = connection.getHeaderFieldDate("Last-Modified", -1);
            if (!force && destFile.exists() && (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED || lastModified > 0 && destFile.lastModified() >= lastModified))
                return destFile;

            destFile.getParentFile().mkdirs();
            try {
                Files.copy(connection.getInputStream(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                destFile.delete();
                throw e;
            }

            if (lastModified > 0)
                destFile.setLastModified(lastModified);

            String etag = connection.getHeaderField("ETag");
            if (etag != null) {
                //noinspection UnstableApiUsage
                com.google.common.io.Files.asCharSink(etagFile, StandardCharsets.UTF_8).write(etag);
            }
            return destFile;
        } catch (UnknownHostException e) {
            return destFile.exists() ? destFile : null;
        } catch (IOException e) {
            Bukkit.getLogger().severe("Error downloading file " + dest + " from " + url);
            return null;
        }
    }

    @SuppressWarnings({"unused", "MismatchedReadAndWriteOfArray"})
    private static class Versions {
        private Version[] versions;
        private static class Version {
            public String id;
            public String url;
        }
    }

    @SuppressWarnings("unused")
    private static class Version {
        private Downloads downloads;
        private static class Downloads {
            private ClientDownload client;
            private static class ClientDownload {
                private String url;
            }
        }
    }

    @SuppressWarnings({"FieldMayBeFinal", "unused", "MismatchedQueryAndUpdateOfCollection"})
    private static class BlockModel {
        private String parent;
        private boolean ambientocclusion = true;
        private Map<String, Object> display;
        private Map<String, String> textures;
        private List<Element> elements;

        private static class Element {
            private float[] from;
            private float[] to;
            private Map<String, Object> rotation;
            private boolean shade = true;
            private Map<String, Face> faces;

            private static class Face {
                private float[] uv;
                private String texture;
                private String cullface;
                private int rotation;
                private int tintindex;
            }
        }
    }
}
