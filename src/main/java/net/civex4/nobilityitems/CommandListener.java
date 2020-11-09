package net.civex4.nobilityitems;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
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
            sender.sendMessage(ChatColor.YELLOW + "/nobilityitems listblocks "
                + ChatColor.GREEN + "lists all loaded NobilityBlocks");
            sender.sendMessage(ChatColor.YELLOW + "/nobilityitems createblock <name> <unobtainable-block> [has-item] "
                + ChatColor.GREEN + "creates a NobilityBlock");
            sender.sendMessage(ChatColor.YELLOW + "/nobilityitems setblock <x> <y> <z> <name>"
                + ChatColor.GREEN + "sets a NobilityBlock in the world");
            
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

                    displayName.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new Text(components)));
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
    
            if (item.getType().isAir()) {
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
    
            if (item.getType().isAir()) {
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

            try {
                PackGenerator.generate();
                sender.sendMessage(ChatColor.YELLOW + "Pack Generated! Check the NobilityItems config folder.");
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + "Packed failed to generate! Check log for details.");
                e.printStackTrace();
            }

            if (NobilityItems.debugPacks && player != null) {
                PackServer.sendPack(player);
            }

            return true;
        } else if (args[0].equals("listblocks")) {
            if (args.length > 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /nobilityitems listblocks");
                return true;
            }

            sender.sendMessage(ChatColor.DARK_GREEN + "Loaded NobilityBlocks:");

            for (NobilityBlock block : BlockManager.getBlocks()) {
                TextComponent message = new TextComponent(ChatColor.GREEN + block.getInternalName());
                if (block.hasItem()) {
                    message.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new Text("Item: " + ChatColor.YELLOW + block.getItem().getInternalName())));
                }

                message.addExtra(ChatColor.GRAY + " = " + block.getBlockData().getAsString(false));

                sender.spigot().sendMessage(message);
            }

            return true;
        } else if (args[0].equals("createblock")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /nobilityitems createblock <name> <unobtainable-block> [has-item]");
                return true;
            }

            String internalName = args[1];

            BlockData blockData;
            try {
                blockData = Bukkit.createBlockData(args[2]);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Invalid block data " + args[2]);
                return true;
            }
            if (!UnobtainableBlocks.isUnobtainable(blockData)) {
                sender.sendMessage(ChatColor.RED + args[2] + " is not unobtainable");
                return true;
            }

            boolean hasItem = args.length > 3 && Boolean.parseBoolean(args[3]);
            NobilityItem item = null;

            if (hasItem) {
                ItemStack stack = new ItemStack(Material.STRUCTURE_VOID);
                if (sender instanceof Player) {
                    ItemStack heldStack = ((Player) sender).getInventory().getItemInMainHand();
                    if (!heldStack.getType().isAir() && heldStack.hasItemMeta()) {
                        assert heldStack.getItemMeta() != null;
                        stack.setItemMeta(Bukkit.getItemFactory().asMetaFor(heldStack.getItemMeta(), stack));
                    }
                }

                if (!ItemManager.makeItem(internalName, stack)) {
                    sender.sendMessage(ChatColor.RED + "Unable to create item! Is that name already in use?");
                    return true;
                }

                item = ItemManager.getItem(internalName);
            }

            if (!BlockManager.makeBlock(internalName, blockData, item)) {
                sender.sendMessage(ChatColor.RED + "Unable to create block! Is that name already in use?");
                return true;
            }

            sender.sendMessage(ChatColor.YELLOW + "Created " + internalName + " " + (item == null ? "block" : "block and item") + "!");

            return true;
        } else if (args[0].equals("setblock")) {
            if (args.length != 5) {
                sender.sendMessage(ChatColor.RED + "Usage: /nobilityitems setblock <x> <y> <z> <name>");
                return true;
            }

            if (player == null) {
                sender.sendMessage(ChatColor.RED + "/nobilityitems setblock requires a player");
                return true;
            }

            boolean xRelative = args[1].startsWith("~");
            boolean yRelative = args[2].startsWith("~");
            boolean zRelative = args[3].startsWith("~");
            String xStr = xRelative ? args[1].substring(1) : args[1];
            String yStr = xRelative ? args[2].substring(1) : args[2];
            String zStr = xRelative ? args[3].substring(1) : args[3];
            int x, y, z;
            try {
                x = xRelative && xStr.isEmpty() ? 0 : Integer.parseInt(xStr);
                y = yRelative && yStr.isEmpty() ? 0 : Integer.parseInt(yStr);
                z = zRelative && zStr.isEmpty() ? 0 : Integer.parseInt(zStr);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid coordinates");
                return true;
            }
            if (xRelative) x += player.getLocation().getBlockX();
            if (yRelative) y += player.getLocation().getBlockY();
            if (zRelative) z += player.getLocation().getBlockZ();

            String internalName = args[4];
            NobilityBlock block = BlockManager.getNullableBlock(internalName);
            if (block == null) {
                sender.sendMessage(ChatColor.RED + internalName + " is not a valid NobilityBlock! Try /nobilityitems listblocks");
                return true;
            }

            player.getWorld().getBlockAt(x, y, z).setBlockData(block.getBlockData());

            sender.sendMessage(ChatColor.YELLOW + "Placed NobilityBlock " + internalName);

            return true;
        }

        return false;
    }

}
