package net.civex4.nobilityitems;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent.Action;

public class CommandListener implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Usage of this command is restricted!");
        }

        String pluginName = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "[" + NobilityItems.getInstance().getName() + "]";

        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.GREEN + "===" + pluginName + ChatColor.GREEN + "===");
            sender.sendMessage(ChatColor.YELLOW + "/nobilityitems " 
                + ChatColor.GREEN + "NobilityItems help");
            sender.sendMessage(ChatColor.YELLOW + "/nobilityitems list " 
                + ChatColor.GREEN + "lists all loaded NobilityItems");
                sender.sendMessage(ChatColor.YELLOW + "/nobilityitems what " 
                + ChatColor.GREEN + "says if the item being held is a NobilityItem");
            sender.sendMessage(ChatColor.YELLOW + "/nobilityitems get <name> (amount) " 
                + ChatColor.GREEN + "gets a specified NobilityItem");
            sender.sendMessage(ChatColor.YELLOW + "/nobilityitems create <name> " 
                + ChatColor.GREEN + "creates a NobilityItem");
            sender.sendMessage(ChatColor.YELLOW + "/nobilityitems generate " 
                + ChatColor.GREEN + "generates a resourcepack for all items with CustomModelData");
            
            return true;
        } else if (args[0].equals("list")) {
            if (args.length > 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /nobilityitems list");
                return true;
            }

            sender.sendMessage(ChatColor.DARK_GREEN + "Loaded Nobilityitems:");
            
            for (NobilityItem item : ItemManager.getItems()) {
                TextComponent message = new TextComponent(ChatColor.GREEN + item.getInternalName() + " ");
                TextComponent displayName = new TextComponent(ChatColor.YELLOW + "(" + item.getDisplayName() + ChatColor.YELLOW + ")");

                if (item.hasLore()) {
                    List<BaseComponent> componentsList = new ArrayList<>();
                    for (int i = 0; i < item.getLore().size(); i++) {
                        if (i != item.getLore().size() - 1) {
                            componentsList.add(new TextComponent(item.getLore().get(i) + "\n"));
                        } else {
                            componentsList.add(new TextComponent(item.getLore().get(i)));
                        }
                    }

                    BaseComponent[] components = new BaseComponent[componentsList.size()];
                    for (int i = 0; i < components.length; i++) {
                        components[i] = componentsList.get(i);
                    }

                    displayName.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, components));
                }

                message.addExtra(displayName);

                sender.spigot().sendMessage(message);
            }

            return true;
        } else if (args[0].equals("what")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /nobilityitems what");
                return true;
            }

            if (player == null) {
                sender.sendMessage(ChatColor.RED + "This command cannot be used from the console!");
                return true;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
    
            if (item == null) {
                sender.sendMessage(ChatColor.RED + "You must have an item in your main hand!");
                return true;
            }

            NobilityItem nItem = ItemManager.getItem(item);

            if (nItem == null) {
                sender.sendMessage(ChatColor.YELLOW + "You are not holding a NobilityItem!");
                return true;
            }

            sender.sendMessage(ChatColor.YELLOW + "You are holding " + ChatColor.GREEN + nItem.getInternalName());
            return true;
        } else if (args[0].equals("get")) {
            if (args.length < 2 || args.length > 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /nobilityitems get <name> (amount)");
                return true;
            }

            if (player == null) {
                sender.sendMessage(ChatColor.RED + "This command cannot be used from the console!");
                return true;
            }

            NobilityItem item;
        
            try {
                item = NobilityItems.getItemByName(args[1]);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + args[1] + " is not a valid NobilityItem! Try /nobilityitems list");
                return true;
            }

            int amount = 1;
            if (args.length == 3) {
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + args[2] + " is not a number!");
                    return true;
                }

                int maxStackSize = item.getItemStack(1).getMaxStackSize();
                if (maxStackSize == -1) {
                    maxStackSize = 1;
                }

                if (amount < 1 || amount > maxStackSize) {
                    sender.sendMessage(ChatColor.RED + args[2] + " is not between 1 and " + maxStackSize + "!");
                    return true;
                }
            }

            player.getInventory().addItem(item.getItemStack(amount));
            if (args.length == 3) {
                sender.sendMessage(ChatColor.YELLOW + "" + amount + "x " + args[1] + " added to your inventory!");
            } else {
                sender.sendMessage(ChatColor.YELLOW + args[1] + " added to your inventory!");
            }
            return true;
        } else if (args[0].equals("create")) {
            if (args.length != 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /nobilityitems create <name>");
                return true;
            }

            if (player == null) {
                sender.sendMessage(ChatColor.RED + "This command cannot be used from the console!");
                return true;
            }
    
            ItemStack item = player.getInventory().getItemInMainHand();
    
            if (item == null) {
                sender.sendMessage(ChatColor.RED + "You must have an item in your main hand!");
                return true;
            }
    
            if(!ItemManager.makeItem(args[1], item)) {
                sender.sendMessage(ChatColor.RED + "Unable to create item! Is that name already in use?");
                return true;
            }
    
            sender.sendMessage(ChatColor.YELLOW + "Created " + args[0] + " item!");
    
            return true;
        } else if (args[0].equals("generate")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /nobilityitems generate");
                return true;
            }

            PackGenerator.generate();
            sender.sendMessage(ChatColor.YELLOW + "Pack Generated! Check the NobilityItems config folder.");
            return true;
        }

        return false;
    }

}