package net.civex4.nobilityitems.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.civex4.nobilityitems.NobilityItem;
import net.civex4.nobilityitems.NobilityItems;

public class CommandGetItem implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Usage of this command is restricted!");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command must be used in-game!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /niget <internal_name>");
            return true;
        }

        NobilityItem item;
        
        try {
            item = NobilityItems.getItemByName(args[0]);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid name!");
            return true;
        }

        player.getInventory().addItem(item.getItemStack(1));

        return true;
    }


    
}