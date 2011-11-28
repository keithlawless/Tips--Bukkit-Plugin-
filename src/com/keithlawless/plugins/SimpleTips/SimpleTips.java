/*
 * SimpleTips (http://github.com/keithlawless/Tips--Bukkit-Plugin-)
 * Copyright (C) 2011 Keith Lawless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.keithlawless.plugins.SimpleTips;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.sun.corba.se.impl.orbutil.graph.Node;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import java.awt.image.ImagingOpException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class SimpleTips extends JavaPlugin implements Runnable {
    private static int MSG_ORDER_SEQ = 0;
    private static int MSG_ORDER_RANDOM = 1;

    private static String version = "SimpleTips v1.0 by keithlawless";
    Logger log = Logger.getLogger("Minecraft");


    // Delay is measured in server ticks, which is 1/20th of a second.
    private Integer firstMsgDelay = 0;
    private Integer nextMsgDelay = 0;
    private boolean groupMsgEnabled = false;
    private List<String> msgs;
    private HashMap<String,List<String>> groupMsgs;
    private int msgCount = 0;
    private int currentMsg = 0;
    private Random random = new Random();
    private File file;
    private YamlConfiguration config;
    private int msgOrder = MSG_ORDER_SEQ;

    public static PermissionHandler permissionHandler;

    public void onDisable() {
        log.info(version+" has been disabled.");
    }

    public void onEnable() {
        load();
        log.info(version+" has been enabled.");
        BukkitScheduler scheduler = this.getServer().getScheduler();
        int result = scheduler.scheduleAsyncRepeatingTask( this, this, firstMsgDelay, nextMsgDelay );
        if( -1 == result ) {
            log.info(version+" Error! Failed to schedule tip display.");
        }
        else {
            log.info(version+" Success! SimpleTips will be displayed on your schedule.");
        }

        setupPermissions();
    }

    public void load() {
        // YAML configuration file.
        File mainDirectory = new File("plugins"+File.separator+"SimpleTips");
        file = new File(mainDirectory.getAbsolutePath()+File.separator+"config.yml");

        if(!file.exists()) {
            try {
                Vector<String> msgs = new Vector<String>();
                msgs.add("Put your messages here!");

                mainDirectory.mkdirs();
                file.createNewFile();
                config = new YamlConfiguration();
                config.set("firstMsgDelay", (30 * 20));
                config.set("nextMsgDelay", (30 * 20));
                config.set("msgOrder", "Sequential");
                config.set("msgList", msgs);
                config.set("groupMsgEnabled", false);
                config.save(file);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                config = YamlConfiguration.loadConfiguration(file);
                firstMsgDelay = config.getInt("firstMsgDelay", 0);
                nextMsgDelay = config.getInt("nextMsgDelay", 0);
                if((config.getString("msgOrder") != null ) && (config.getString("msgOrder").equalsIgnoreCase("Random"))) {
                    msgOrder = MSG_ORDER_RANDOM;
                }
                else {
                    msgOrder = MSG_ORDER_SEQ;
                }

                groupMsgEnabled = config.getBoolean( "groupMsgEnabled", false );

                msgs = config.getStringList("msgList");
                if(msgs != null) {
                    msgCount = msgs.size();
                }
                else {
                    msgCount = 0;
                }

                if(groupMsgEnabled) {
                    groupMsgs = new HashMap<String,List<String>>();

                    ConfigurationSection section = config.getConfigurationSection("groupMsgList");
                    Set<String> keys = section.getKeys(false);
                    if( keys != null ) {
                        for ( String groupName : keys) {
                            groupMsgs.put(groupName.toLowerCase(), section.getStringList(groupName));
                        }
                    }
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void run() {
        if(groupMsgEnabled) {
            groupMessageDisplay();
        }
        else {
            simpleMessageDisplay();
        }
    }

    private void simpleMessageDisplay() {
        if( msgCount > 0 ) {
            String msg = ( msgOrder == MSG_ORDER_RANDOM ? msgs.get( random.nextInt( msgCount )) : msgs.get(currentMsg));
            this.getServer().broadcastMessage(escape_colors( msg ));
            currentMsg++;
            if( currentMsg >= msgCount ) {
                currentMsg = 0;
            }
        }
    }

    private void groupMessageDisplay() {
        Player[] players = this.getServer().getOnlinePlayers();
        for( Player player : players ) {
            String[] groups = permissionHandler.getGroups(player.getWorld().getName(), player.getName());
            for( String group : groups ) {
                List<String> msgList = groupMsgs.get(group.toLowerCase());
                if( msgList != null ) {
                    int c = msgList.size();
                    if( c > 0 ) {
                        String msg = msgList.get( random.nextInt( c ));
                        player.sendMessage(escape_colors(msg));
                    }
                }
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if(!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player)sender;

        if(command.getName().equalsIgnoreCase("tip")) {

            // player entered just /tip by itself - return a random tip
            if( args.length == 0 ) {
                int x = random.nextInt( msgCount );
                player.sendMessage( escape_colors(msgs.get(x)));
                return true;
            }

            if( args.length == 1 ) {
                // player entered /tip list
                if( args[0].equalsIgnoreCase("list")) {

                    if (( SimpleTips.permissionHandler == null ) && (!sender.isOp())) {
                        player.sendMessage( "(SimpleTips) You don't have permission to run that command.");
                    }
                    else if (( SimpleTips.permissionHandler != null ) && (!SimpleTips.permissionHandler.has(player, "tip.list"))) {
                        player.sendMessage( "(SimpleTips) You don't have permission to run that command.");
                    }
                    else {
                        for( int x = 0; x < msgCount; x++ ) {
                            player.sendMessage( "(" + x + ") " + escape_colors(msgs.get(x)));
                        }
                    }

                    return true;
                }
            }

            if( args.length >= 2  ) {
                // player entered /tip add [text]
                if( args[0].equalsIgnoreCase("add")) {

                    if (( SimpleTips.permissionHandler == null ) && (!sender.isOp())) {
                            player.sendMessage( "(SimpleTips) You don't have permission to run that command.");
                    }
                    else if (( SimpleTips.permissionHandler != null ) && (!SimpleTips.permissionHandler.has(player, "tip.add"))) {
                            player.sendMessage( "(SimpleTips) You don't have permission to run that command.");
                    }
                    else {
                        StringBuffer sb = new StringBuffer();
                        for( int x = 1; x < args.length; x++ ) {
                            if( x > 1 ) {
                                sb.append(" ");
                            }
                            sb.append( args[x] );
                        }
                        msgs.add(new String(sb));
                        config.set("msgList", msgs);
                        try {
                            config.save(file);
                            msgCount++;
                            player.sendMessage("(SimpleTips) Tip has been added.");
                        }
                        catch( IOException e ) {
                            player.sendMessage("(SimpleTips) Error while saving configuration.");
                        }
                    }
                    return true;
                }

                //player entered /tip del [num]
                if( args[0].equalsIgnoreCase("del")) {
                    if (( SimpleTips.permissionHandler == null ) && (!sender.isOp())) {
                            player.sendMessage( "(SimpleTips) You don't have permission to run that command.");
                    }
                    else if (( SimpleTips.permissionHandler != null ) && (!SimpleTips.permissionHandler.has(player, "tip.del"))) {
                            player.sendMessage( "(SimpleTips) You don't have permission to run that command.");
                    }
                    else {
                        try {
                            int i = Integer.parseInt(args[1]);
                            msgs.remove(i);
                            config.set("msgList", msgs);
                            try {
                                config.save(file);
                                msgCount--;
                                player.sendMessage("(SimpleTips) Tip has been deleted.");
                            }
                            catch( IOException e ) {
                                player.sendMessage("(SimpleTips) Error while saving configuration.");
                            }
                        }
                        catch(NumberFormatException nfe) {
                            return false;
                        }
                    }
                    return true;
                }

                if( args[0].equalsIgnoreCase("replace")) {
                    if (( SimpleTips.permissionHandler == null ) && (!sender.isOp())) {
                            player.sendMessage( "(SimpleTips) You don't have permission to run that command.");
                    }
                    else if (( SimpleTips.permissionHandler != null ) && (!SimpleTips.permissionHandler.has(player, "tip.replace"))) {
                            player.sendMessage( "(SimpleTips) You don't have permission to run that command.");
                    }
                    else {
                        if( args.length < 3 ) {
                            return false;
                        }
                        Integer msgIndex = 0;
                        try {
                            msgIndex = Integer.parseInt(args[1]);
                            StringBuffer sb = new StringBuffer();
                            for( int x = 2; x < args.length; x++ ) {
                                if( x > 2 ) {
                                    sb.append(" ");
                                }
                                sb.append( args[x] );
                            }
                            msgs.set(msgIndex, new String(sb));
                            config.set("msgList", msgs);
                            try {
                                config.save(file);
                                player.sendMessage("(SimpleTips) Tip has been replaced.");
                            }
                            catch( IOException e ) {
                                player.sendMessage("(SimpleTips) Error while saving configuration.");
                            }
                        }
                        catch(NumberFormatException nfe) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }

        return false;
    }

    private void setupPermissions() {
      Plugin permissionsPlugin = this.getServer().getPluginManager().getPlugin("Permissions");

      if (permissionHandler == null) {
          if (permissionsPlugin != null) {
              permissionHandler = ((Permissions) permissionsPlugin).getHandler();
          } else {
              log.info("Permission system not detected, defaulting to OP");
          }
      }
    }

    private String escape_colors(String input) {
        char[] color_codes = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

        String output = new String(input);
        for( int x = 0; x < color_codes.length; x++ ) {
            output = output.replace( "%"+color_codes[x], "\u00A7"+color_codes[x]);
        }

        return output;
    }
}
