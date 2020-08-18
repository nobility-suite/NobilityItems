package net.civex4.nobilityitems.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.civex4.nobilityitems.ItemManager;

public class CommandCreateItem implements CommandExecutor {

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
            sender.sendMessage(ChatColor.RED + "Usage: /nicreate <internal_name>");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null) {
            sender.sendMessage(ChatColor.RED + "You must have an item in your main hand!");
            return true;
        }

        if(!ItemManager.makeItem(args[0], item)) {
            sender.sendMessage(ChatColor.RED + "Unable to create item! Is that name already in use?");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Created " + args[0] + " item!");

        return true;
    }
    
}