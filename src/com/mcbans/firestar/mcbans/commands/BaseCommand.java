package com.mcbans.firestar.mcbans.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mcbans.firestar.mcbans.BukkitInterface;
import com.mcbans.firestar.mcbans.Settings;
import com.mcbans.firestar.mcbans.exception.CommandException;

public abstract class BaseCommand {
    // Set this class
    protected BukkitInterface plugin;
    protected Settings config;
    protected CommandSender sender;
    protected String command;

    // Needs init
    protected List<String> args = new ArrayList<String>();
    protected String senderName;
    protected Player player;
    protected boolean isPlayer;

    // Set this class if banning (needs init)
    protected String target = "";
    protected String targetIP = "";

    // Set extend class constructor (Command property)
    protected String name;
    protected int argLength = 0;
    protected String usage;
    protected boolean bePlayer = false;
    protected boolean banning = false;

    public boolean run(final BukkitInterface plugin, final CommandSender sender, final String cmd, final String[] preArgs) {
        if (name == null){
            plugin.broadcastPlayer(sender, "&cThis command not loaded properly!");
            return true;
        }

        // init command
        init();

        this.plugin = plugin;
        this.config = plugin.settings;
        this.sender = sender;
        this.command = cmd;

        // Sort args

        for (String arg : preArgs)
            args.add(arg);

        // Check args size
        if (argLength > args.size()){
            //sendUsage();
            plugin.broadcastPlayer(sender, ChatColor.DARK_RED + plugin.language.getFormat("formatError"));
            return true;
        }

        // Check sender is player
        if (bePlayer && !(sender instanceof Player)){
            plugin.broadcastPlayer(sender, "&cThis command cannot run from Console!");
            return true;
        }
        if (sender instanceof Player){
            player = (Player)sender;
            senderName = player.getName();
            isPlayer = true;
        }

        // Check permission
        if (!permission(sender)){
            plugin.broadcastPlayer(sender, plugin.language.getFormat("permissionDenied"));
            //plugin.log(senderName + " has tried the command [" + command + "]!"); // maybe not needs command logger. Craftbukkit added this.
            //plugin.broadcastPlayer(sender, "&cYou don't have permission to use this!");
            return true;
        }

        // set banning information
        if (banning && args.size() > 0){
            // target = args.remove(0); // Don't touch args here
            target = args.get(0).trim();
            // get targetIP if available
            final Player targetPlayer = Bukkit.getPlayerExact(target);
            if (targetPlayer != null && targetPlayer.isOnline()){
                targetIP = targetPlayer.getAddress().getAddress().getHostAddress();
            }
        }

        // Exec
        try {
            execute();
        }
        catch (CommandException ex) {
            Throwable error = ex;
            while (error instanceof Exception){
                plugin.broadcastPlayer(sender, error.getMessage());
                error = error.getCause();
            }
        }

        return true;
    }

    /**
     * Initialize command
     */
    private void init(){
        this.args.clear();
        this.player = null;
        this.isPlayer = false;
        this.senderName = "Console";

        this.target = "";
        this.targetIP = "";
    }

    /**
     * Execute command
     */
    public abstract void execute() throws CommandException;

    /**
     * TabComplete
     */
    protected List<String> tabComplete(final BukkitInterface plugin, final CommandSender sender, final String cmd, final String[] preArgs){
        return null;
    }

    /**
     * Check sender has command permission
     * @return true if sender has permission
     */
    public abstract boolean permission(final CommandSender sender);

    /**
     * Send command usage
     */
    public void sendUsage(){
        // TODO: change this
        //plugin.broadcastPlayer(sender, "&c/"+this.command+" "+name+" "+usage);
    }
}
