package net.civex4.nobilityitems.impl;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;

public class PlayerListener implements Listener {

    private final File dataFolder;

    public PlayerListener(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (PackServer.debugPacks) {
            PackServer.sendPack(dataFolder, e.getPlayer());
        }
    }

}
