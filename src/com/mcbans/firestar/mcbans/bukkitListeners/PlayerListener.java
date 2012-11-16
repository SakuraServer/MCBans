package com.mcbans.firestar.mcbans.bukkitListeners;

import static com.mcbans.firestar.mcbans.I18n._;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.mcbans.firestar.mcbans.ActionLog;
import com.mcbans.firestar.mcbans.I18n;
import com.mcbans.firestar.mcbans.MCBans;
import com.mcbans.firestar.mcbans.permission.Perms;
import com.mcbans.firestar.mcbans.pluginInterface.Disconnect;
import com.mcbans.firestar.mcbans.util.Util;

public class PlayerListener implements Listener {
    private final MCBans plugin;
    private final ActionLog log;

    public PlayerListener(final MCBans plugin) {
        this.plugin = plugin;
        this.log = plugin.getLog();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerPreLoginEvent(final AsyncPlayerPreLoginEvent event) {
        try {
            int check = 1;
            while (plugin.notSelectedServer) {
                // waiting for server select
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                check++;
                if (check > 5) {
                    log.warning("Can't reach mcbans server! Check passed player: " + event.getName());
                    return;
                }
            }

            // get player information
            final URL urlMCBans = new URL("http://" + plugin.apiServer + "/v2/" + plugin.getConfigs().getApiKey() + "/login/"
                    + URLEncoder.encode(event.getName(), "UTF-8") + "/"
                    + URLEncoder.encode(String.valueOf(event.getAddress().getHostAddress()), "UTF-8"));
            BufferedReader br = null;
            String response = null;
            try{
                br = new BufferedReader(new InputStreamReader(urlMCBans.openStream()));
                response = br.readLine();
            }finally{
                if (br != null) br.close();
            }
            if (response == null){
                log.warning("Null response! (Player: " + event.getName() + ")");
                return;
            }

            plugin.debug("Response: " + response);
            String[] s = response.split(";");
            if (s.length == 6 || s.length == 7) {
                // check banned
                if (s[0].equals("l") || s[0].equals("g") || s[0].equals("t") || s[0].equals("i") || s[0].equals("s")) {
                    event.disallow(Result.KICK_BANNED, s[1]);
                    return;
                }
                // check reputation
                else if (plugin.getConfigs().getMinRep() > Double.valueOf(s[2])) {
                    event.disallow(Result.KICK_BANNED, "Reputation too low!");
                    return;
                }
                // check alternate accounts
                else if (plugin.getConfigs().getMaxAlts() < Integer.valueOf(s[3])) {
                    event.disallow(Result.KICK_BANNED, "You have too many alternate accounts!");
                    return;
                }
                // check passed, put data to playerCache
                else{
                    HashMap<String, String> tmp = new HashMap<String, String>();
                    if(s[0].equals("b")){
                        tmp.put("b", "y");
                    }
                    if(Integer.parseInt(s[3])>0){
                        tmp.put("a", s[3]);
                        tmp.put("al", s[6]);
                    }
                    if(s[4].equals("y")){
                        tmp.put("m", "y");
                    }
                    if(Integer.parseInt(s[5])>0){
                        tmp.put("d", s[5]);
                    }
                    plugin.playerCache.put(event.getName(),tmp);
                }
                plugin.debug(event.getName() + " authenticated with " + s[2] + " rep");
            }else{
                log.warning("Invalid response! Player: " + event.getName() + ", length: " + s.length);
                log.warning("Response: " + response);
            }
        }catch (Exception ex){
            log.warning("Error occurred in AsyncPlayerPreLoginEvent. Please report this!");
            ex.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        String playerName = event.getPlayer().getName();
        HashMap<String,String> pcache = plugin.playerCache.remove(playerName);
        if(pcache == null) return;
        if(pcache.containsKey("b")){
            //MCBans.broadcastPlayer(playerName, ChatColor.DARK_RED + "You have bans on record! ( check http://mcbans.com )" );
            //MCBans.broadcastJoinView( ChatColor.DARK_RED + MCBans.Language.getFormat( "previousBans", playerName ) );

            // Modify
            if (!Perms.has(event.getPlayer(), "ignoreBroadcastLowRep")){
                //MCBans.broadcastAll("プレイヤー'" + ChatColor.DARK_AQUA + PlayerName + ChatColor.WHITE + "'は" + ChatColor.DARK_RED + response.getString("totalBans") + "つのBAN" + ChatColor.WHITE + "を受けています" + ChatColor.AQUA + "(" + response.getString("playerRep") + " REP)" );
                Util.broadcastMessage(ChatColor.RED + _("previousBans", I18n.PLAYER, playerName));
            }else{
                //MCBans.broadcastJoinView( "Player " + ChatColor.DARK_AQUA + PlayerName + ChatColor.WHITE + " has " + ChatColor.DARK_RED + response.getString("totalBans") + " ban(s)" + ChatColor.WHITE + " and " + ChatColor.AQUA + response.getString("playerRep") + " REP" + ChatColor.WHITE + "." );
            	Perms.VIEW_BANS.message(ChatColor.DARK_RED + _("previousBans", I18n.PLAYER, playerName));
            }
            // older codes
            /*
            if (response.getJSONArray("globalBans").length() > 0 && MCBans.Settings.getBoolean("onConnectGlobals")) {
                // Modify
                if (!MCBans.Permissions.isAllow(PlayerName, "ignoreBroadcastLowRep")){
                    MCBans.broadcastAll("プレイヤー'" + ChatColor.DARK_AQUA + PlayerName + ChatColor.WHITE + "'は" + ChatColor.DARK_RED + response.getString("totalBans") + "つのBAN" + ChatColor.WHITE + "を受けています" + ChatColor.AQUA + "(" + response.getString("playerRep") + " REP)" );
                }else{
                    MCBans.broadcastJoinView( "Player " + ChatColor.DARK_AQUA + PlayerName + ChatColor.WHITE + " has " + ChatColor.DARK_RED + response.getString("totalBans") + " ban(s)" + ChatColor.WHITE + " and " + ChatColor.AQUA + response.getString("playerRep") + " REP" + ChatColor.WHITE + "." );
                }
                MCBans.broadcastJoinView("--------------------------");
                if (response.getJSONArray("globalBans").length() > 0) {
                    for (int v = 0; v < response.getJSONArray("globalBans").length(); v++) {
                        out = response.getJSONArray("globalBans").getString(v).split(" .:. ");
                        if (out.length == 2) {
                            MCBans.broadcastJoinView(ChatColor.LIGHT_PURPLE + out[0]);
                            MCBans.broadcastJoinView("\\---\"" + ChatColor.DARK_PURPLE + out[1] + "\"");
                        }
                    }
                }
                MCBans.broadcastJoinView("--------------------------");
            }
            */
        }
        if(pcache.containsKey("d")){
            Util.message(playerName, ChatColor.DARK_RED + pcache.get("d") + " open disputes!");
        }
        if(pcache.containsKey("a")){
            Perms.VIEW_ALTS.message(ChatColor.DARK_PURPLE + _("altAccounts", I18n.PLAYER, playerName, I18n.ALTS, pcache.get("al")));
        }
        if(pcache.containsKey("m")){
            log.info(playerName + " is a MCBans.com Staff member");
            Util.broadcastMessage(ChatColor.AQUA + _("isMCBansMod", I18n.PLAYER, playerName));
            Util.message(playerName, ChatColor.AQUA + _("youAreMCBansStaff"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        Disconnect disconnectHandler = new Disconnect(plugin, event.getPlayer().getName());
        (new Thread(disconnectHandler)).start();
    }
}