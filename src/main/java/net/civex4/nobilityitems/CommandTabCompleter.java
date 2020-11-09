package net.civex4.nobilityitems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class CommandTabCompleter implements TabCompleter {
    private static final List<String> COMMANDS = new ArrayList<>();

    static {
        COMMANDS.add("list");
        COMMANDS.add("what");
        COMMANDS.add("get");
        COMMANDS.add("create");
        COMMANDS.add("generate");
        COMMANDS.add("listblocks");
        COMMANDS.add("createblock");
        COMMANDS.add("setblock");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> tabs = new ArrayList<>();
        if (!sender.isOp()) {
            return tabs;
        }

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], COMMANDS, tabs);
        } else {
            if (args[0].equals("get")) {
                if (args.length == 2) {
                    List<String> names = new ArrayList<>();
                    for (NobilityItem item : ItemManager.getItems()) {
                        names.add(item.getInternalName());
                    }
                    StringUtil.copyPartialMatches(args[1], names, tabs);
                }
            } else if (args[0].equals("createblock")) {
                if (args.length == 3) {
                    List<String> unobtainable = new ArrayList<>();
                    for (BlockData data : UnobtainableBlocks.getUnobtainableBlocks()) {
                        if (BlockManager.getBlock(data) == null) {
                            unobtainable.add(data.getAsString(false));
                        }
                    }
                    StringUtil.copyPartialMatches(args[2], unobtainable, tabs);
                }
            } else if (args[0].equals("setblock")) {
                if (args.length <= 4) {
                    if (sender instanceof Player) {
                        Block target = ((Player) sender).getTargetBlockExact(5);
                        if (target == null) {
                            StringUtil.copyPartialMatches(args[args.length - 1], Collections.singletonList("~"), tabs);
                        } else {
                            int coord;
                            if (args.length == 2) coord = target.getX();
                            else if (args.length == 3) coord = target.getY();
                            else coord = target.getZ();
                            StringUtil.copyPartialMatches(args[args.length - 1], Collections.singletonList(String.valueOf(coord)), tabs);
                        }
                    }
                } else if (args.length == 5) {
                    List<String> names = new ArrayList<>();
                    for (NobilityBlock block : BlockManager.getBlocks()) {
                        names.add(block.getInternalName());
                    }
                    StringUtil.copyPartialMatches(args[4], names, tabs);
                }
            }
        }

        return tabs;
    }

}
