package us.tastybento.bskyblock.api.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.BSBLocale;
import us.tastybento.bskyblock.database.managers.IslandsManager;
import us.tastybento.bskyblock.database.managers.PlayersManager;
import us.tastybento.bskyblock.util.Util;

/**
 *
 * @author Poslovitch
 */
public abstract class AbstractCommand implements CommandExecutor, TabCompleter {

    private BSkyBlock plugin;

    private final Map<String, ArgumentHandler> argumentsMap;
    private final Map<String, String> aliasesMap;

    public final String label;
    public final String[] aliases;
    public boolean isPlayer;
    public boolean inTeam;
    public UUID teamLeaderUUID;
    public Set<UUID> teamMembers;
    public Player player;
    public UUID playerUUID;

    private final boolean help;
    private static final int MAX_PER_PAGE = 7;

    private static final boolean DEBUG = false;

    protected AbstractCommand(BSkyBlock plugin, String label, String[] aliases, boolean help) {
        this.plugin = plugin;
        this.argumentsMap = new LinkedHashMap<>();
        this.aliasesMap = new HashMap<>();
        this.label = label;
        this.aliases = aliases;
        this.help = help;
        this.teamMembers = new HashSet<>();

        // Register the help argument if needed
        if (help) {
            addArgument(new String[]{"help", "?"}, new ArgumentHandler() {
                @Override
                public CanUseResp canUse(CommandSender sender) {
                    return new CanUseResp(true); // If the player has access to this command, he can get help
                }

                @Override
                public void execute(CommandSender sender, String[] args) {
                    Util.sendMessage(sender, plugin.getLocale(sender).get("help.header"));
                    for(String arg : argumentsMap.keySet()){
                        ArgumentHandler handler = getHandler(arg);
                        if (handler.canUse(sender).isAllowed()) Util.sendMessage(sender, handler.getShortDescription(sender));
                    }
                    Util.sendMessage(sender, plugin.getLocale(sender).get("help.end"));
                }

                @Override
                public Set<String> tabComplete(CommandSender sender, String[] args) {
                    return null; // No tab options for this one
                }

                @Override
                public String[] usage(CommandSender sender) {
                    return new String[] {"", ""};
                }
            });
        }

        // Register the other arguments
        setup();
    }

    /**
     *
     */
    public abstract class ArgumentHandler {
        public abstract CanUseResp canUse(CommandSender sender);
        public abstract void execute(CommandSender sender, String[] args);
        public abstract Set<String> tabComplete(CommandSender sender, String[] args);
        public abstract String[] usage(CommandSender sender);

        public String getShortDescription(CommandSender sender) {
            String msg = plugin.getLocale(sender).get("help.syntax");
            msg = msg.replace("[label]", (aliases[0] != null) ? aliases[0] : label);

            String command = "";
            for(Map.Entry<String, ArgumentHandler> entry : argumentsMap.entrySet()) {
                if (entry.getValue().equals(this)) {
                    command = entry.getKey();
                    break;
                }
            }

            String cmds = command;
            for(String alias : getAliases(command)) {
                cmds += plugin.getLocale(sender).get("help.syntax-alias-separator") + alias;
            }

            msg = msg.replace("[command]", cmds);

            String[] usage = argumentsMap.get(command).usage(sender);
            if (usage == null) usage = new String[2];

            msg = msg.replace("[args]", (usage[0] != null) ? usage[0] : "")
                    .replace("[info]", (usage[1] != null) ? usage[1] : "");

            return msg;
        }
    }

    public abstract void setup();

    public abstract CanUseResp canUse(CommandSender sender);
    public abstract void execute(CommandSender sender, String[] args);

    public void addArgument(String[] names, ArgumentHandler handler) {
        // TODO add some security checks to avoid duplicates
        argumentsMap.put(names[0], handler);
        for (int i = 1 ; i < names.length ; i++) {
            aliasesMap.put(names[0], names[i]);
        }
    }

    public ArgumentHandler getHandler(String argument) {
        if (isAlias(argument)) return argumentsMap.get(getParent(argument));
        else return argumentsMap.get(argument);
    }

    public void setHandler(String argument, ArgumentHandler handler) {
        if (argumentsMap.containsKey(argument)) argumentsMap.put(argument, handler);
    }

    public boolean isAlias(String argument) {
        return aliasesMap.containsValue(argument);
    }

    public void addAliases(String parent, String... aliases) {
        if (argumentsMap.containsKey(parent)) {
            for (String alias : aliases) {
                if (!aliasesMap.containsKey(alias) && !aliasesMap.containsValue(alias)) aliasesMap.put(parent, alias);
            }
        }
    }

    public void removeAliases(String... aliases) {
        for (String alias : aliases) {
            if (aliasesMap.containsValue(alias)) aliasesMap.remove(getParent(alias));
        }
    }

    public String getParent(String alias) {
        if (isAlias(alias)) {
            for(String parent : aliasesMap.keySet()) {
                if (aliasesMap.get(parent).equals(alias)) return parent;
            }
            return null;
        }
        else return alias;
    }

    public Set<String> getAliases(String argument) {
        Set<String> aliases = new HashSet<>();

        for (Map.Entry<String, String> entry : aliasesMap.entrySet()) {
            if (entry.getKey().equals(argument)) aliases.add(entry.getValue());
        }

        return aliases;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        checkForPlayer(sender);
        CanUseResp canUse = this.canUse(sender);
        if (canUse.isAllowed()) {
            if(args.length >= 1) {
                ArgumentHandler handler = getHandler(args[0]); // Store the handler to save some calculations
                if (handler != null && handler.canUse(sender).isAllowed()) {
                    handler.execute(sender, clean(Arrays.copyOfRange(args, 1, args.length)));
                } else if (handler != null && !handler.canUse(sender).isAllowed() && !handler.canUse(sender).getErrorResponse().isEmpty()) {
                    Util.sendMessage(sender, handler.canUse(sender).errorResponse);
                } else if (help) {
                    if (argumentsMap.containsKey("help")) {
                        argumentsMap.get("help").execute(sender, clean(Arrays.copyOfRange(args, 1, args.length)));
                    }
                } else {
                    // Unknown handler
                    this.execute(sender, args);
                }
            } else {
                // No args
                this.execute(sender, args);
            }
        } else {
            // Sender cannot use this command - tell them why
            Util.sendMessage(sender, canUse.errorResponse);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args){
        List<String> options = new ArrayList<>();
        checkForPlayer(sender);
        String lastArg = (args.length != 0 ? args[args.length - 1] : "");
        if (canUse(sender).isAllowed()) {
            if (args.length <= 1) {
                // Go through every argument, check if player can use it and if so, add it in tab options
                for(String argument : argumentsMap.keySet()) {
                    if (getHandler(argument).canUse(sender).isAllowed()) options.add(argument);
                }
            } else {
                // If player can execute the argument, get its tab-completer options
                ArgumentHandler handler = getHandler(args[0]);
                if (handler != null && handler.canUse(sender).isAllowed()) {
                    // We remove the 1st arg - and remove any blank args caused by hitting space before the tab
                    Set<String> tabOptions = handler.tabComplete(sender, clean(Arrays.copyOfRange(args, 1, args.length)));
                    if (tabOptions != null) options.addAll(tabOptions);
                }
            }
        }
        return Util.tabLimit(options, lastArg);
    }

    private static String[] clean(final String[] v) {
        List<String> list = new ArrayList<>(Arrays.asList(v));
        list.removeAll(Collections.singleton(""));
        return list.toArray(new String[list.size()]);
    }

    /**
     * Sets some variables and flags if this is a player
     * @param sender
     */
    private void checkForPlayer(CommandSender sender) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: checkForPlayer");
        // Check if the command sender is a player or not
        if (sender instanceof Player) {
            isPlayer = true;
            player = (Player)sender;
            playerUUID = player.getUniqueId();
        } else {
            isPlayer = false;
        }
        // Check if the player is in a team or not and if so, grab the team leader's UUID
        if (plugin.getPlayers().inTeam(playerUUID)) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: player in team");
            inTeam = true;
            teamLeaderUUID = plugin.getIslands().getTeamLeader(playerUUID);
            if (DEBUG)
                plugin.getLogger().info("DEBUG: team leader UUID = " + teamLeaderUUID);
            teamMembers = plugin.getIslands().getMembers(teamLeaderUUID);
            if (DEBUG) {
                plugin.getLogger().info("DEBUG: teammembers = ");
                for (UUID member: teamMembers) {
                    plugin.getLogger().info("DEBUG: " + member);
                }
            }
        } else {
            inTeam = false;
        }

    }

    /**
     * Response class for the canUse check
     * @author tastybento
     *
     */
    public class CanUseResp {
        private boolean allowed;
        private String errorResponse; // May be shown if required

        /**
         * Cannot use situation
         * @param errorResponse - error response
         */
        public CanUseResp(String errorResponse) {
            this.allowed = false;
            this.errorResponse = errorResponse;
        }

        /**
         * Can or cannot use situation, no error response.
         * @param b
         */
        public CanUseResp(boolean b) {
            this.allowed = b;
            this.errorResponse = "";
        }
        /**
         * @return the allowed
         */
        public boolean isAllowed() {
            return allowed;
        }
        /**
         * @param allowed the allowed to set
         */
        public void setAllowed(boolean allowed) {
            this.allowed = allowed;
        }
        /**
         * @return the errorResponse
         */
        public String getErrorResponse() {
            return errorResponse;
        }
        /**
         * @param errorResponse the errorResponse to set
         */
        public void setErrorResponse(String errorResponse) {
            this.errorResponse = errorResponse;
        }
    }

    // These methods below just neaten up the code in the commands so "plugin." isn't always used
    /**
     * @return PlayersManager
     */
    protected PlayersManager getPlayers() {
        return plugin.getPlayers();
    }
    /**
     * @return IslandsManager
     */
    protected IslandsManager getIslands() {
        return plugin.getIslands();
    }
    /**
     * @param sender
     * @return Locale for sender
     */
    protected BSBLocale getLocale(CommandSender sender) {
        return plugin.getLocale(sender);
    }
    /**
     * @param uuid
     * @return Locale for UUID
     */
    protected BSBLocale getLocale(UUID uuid) {
        return plugin.getLocale(uuid);
    }

}
