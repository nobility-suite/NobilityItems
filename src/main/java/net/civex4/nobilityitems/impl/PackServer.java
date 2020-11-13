package net.civex4.nobilityitems.impl;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.hash.Hashing;
import com.sun.net.httpserver.HttpServer;
import net.civex4.nobilityitems.NobilityItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.logging.Level;

public class PackServer {
    private static HttpServer server;
    public static final boolean debugPacks = Boolean.getBoolean("nobilityitems.debugPacks");

    public static void start(File dataFolder) {
        try {
            server = HttpServer.create(new InetSocketAddress(Bukkit.getPort() + 1), 0);
            server.createContext("/pack", exchange -> {
                byte[] bytes = Files.readAllBytes(dataFolder.toPath().resolve("pack.zip"));
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream out = exchange.getResponseBody();
                out.write(bytes);
                out.close();
            });
            server.setExecutor(null);
            Thread serverThread = new Thread(server::start);
            serverThread.setName("HTTP Pack Debug Server Thread");
            serverThread.setDaemon(true);
            serverThread.start();
            Bukkit.getLogger().log(Level.INFO, "Starting HTTP server on port " + (Bukkit.getPort() + 1));
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "Unable to start HTTP Pack Debug Server", e);
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public static void sendPack(File dataFolder, Player target) {
        if (server == null) {
            return;
        }

        String sha1;
        try {
            //noinspection UnstableApiUsage
            sha1 = com.google.common.io.Files.hash(new File(dataFolder, "pack.zip"), Hashing.sha1()).toString();
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to compute sha1 of pack.zip", e);
            return;
        }

        Bukkit.getLogger().log(Level.INFO, "Sending resource pack with sha1: " + sha1);

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.RESOURCE_PACK_SEND);
        packet.getStrings()
                .write(0, "http://127.0.0.1:" + (Bukkit.getPort() + 1) + "/pack/" + sha1)
                .write(1, sha1);

        try {
            Protocol.protocolManager.sendServerPacket(target, packet);
        } catch (InvocationTargetException e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to send resource pack packet", e);
        }
    }
}
