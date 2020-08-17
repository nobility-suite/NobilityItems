package net.civex4.nobilityitems;

import org.bukkit.plugin.java.JavaPlugin;

public class NobilityItems extends JavaPlugin {
    private static NobilityItems instance;

    @Override
    public void onEnable() {
        instance = this;
    }

    public static NobilityItems getInstance() {
        return instance;
    }

}