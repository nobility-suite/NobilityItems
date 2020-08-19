package net.civex4.nobilityitems;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

public class CommandTabCompleter implements TabCompleter {
    private static final List<String> COMMANDS = new ArrayList<>();

    static {
        COMMANDS.add("list");
        COMMANDS.add("what");
        COMMANDS.add("get");
        COMMANDS.add("create");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> tabs = new ArrayList<>();
        if (!sender.isOp()) {
            return tabs;
        }

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], COMMANDS, tabs);
        } else if (args.length == 2 && args[0].equals("get")) {
            if (args[0].equals("get")) {
                List<String> names = new ArrayList<>();
                for (NobilityItem item : ItemManager.getItems()) {
                    names.add(item.getInternalName());
                }
                StringUtil.copyPartialMatches(args[1], names, tabs);
            }
        }

        return tabs;
    }

}
