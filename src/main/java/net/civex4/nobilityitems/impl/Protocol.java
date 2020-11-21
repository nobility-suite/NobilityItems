package net.civex4.nobilityitems.impl;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.plugin.Plugin;

public class Protocol {
    public static ProtocolManager protocolManager;

    public static void init(Plugin plugin) {
        protocolManager = ProtocolLibrary.getProtocolManager();
    }
}
