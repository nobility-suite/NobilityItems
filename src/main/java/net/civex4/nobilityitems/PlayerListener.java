package net.civex4.nobilityitems;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (NobilityItems.debugPacks) {
            PackServer.sendPack(e.getPlayer());
        }
    }

}
