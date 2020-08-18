package net.civex4.nobilityitems.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.civex4.nobilityitems.NobilityItem;
import net.civex4.nobilityitems.ItemManager;

public class CommandListItems implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Usage of this command is restricted!");
            return true;
        }

        if (args.length != 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /nilist");
            return true;
        }

        for (NobilityItem item : ItemManager.getItems()) {
            sender.sendMessage(item.getInternalName());
        }


        return true;
    }
    
}