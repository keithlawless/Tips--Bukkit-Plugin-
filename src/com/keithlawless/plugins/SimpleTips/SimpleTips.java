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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.config.Configuration;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

public class SimpleTips extends JavaPlugin implements Runnable {
    private static String version = "SimpleTips v0.3 by keithlawless";
    Logger log = Logger.getLogger("Minecraft");


    // Delay is measured in server ticks, which is 1/20th of a second.
    private Integer firstMsgDelay = 0;
    private Integer nextMsgDelay = 0;
    private List<String> msgs;
    private int msgCount = 0;
    private int currentMsg = 0;
    private Random random = new Random();

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
        File file = new File(mainDirectory.getAbsolutePath()+File.separator+"config.yml");

        Configuration config;

        if(!file.exists()) {
            try {
                Vector<String> msgs = new Vector<String>();
                msgs.add("Put your messages here!");

                mainDirectory.mkdirs();
                file.createNewFile();
                config = new Configuration(file);
                config.setProperty("firstMsgDelay", (30*20));
                config.setProperty("nextMsgDelay", (30*20));
                config.setProperty("msgList", (List<String>)msgs);
                config.save();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                config = new Configuration(file);
                config.load();
                firstMsgDelay = config.getInt("firstMsgDelay", 0);
                nextMsgDelay = config.getInt("nextMsgDelay", 0);
                msgs = config.getStringList("msgList", new Vector<String>() );
                if(msgs != null) {
                    msgCount = msgs.size();
                }
                else {
                    msgCount = 0;
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void run() {
        if( msgCount > 0 ) {
            this.getServer().broadcastMessage(escape_colors(msgs.get(currentMsg)));
            currentMsg++;
            if( currentMsg >= msgCount ) {
                currentMsg = 0;
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

            if( args.length >= 1 ) {
                // player entered /tip list
                if( args[0].equalsIgnoreCase("list")) {

                    if (!SimpleTips.permissionHandler.has(player, "tip.list")) {
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
        }

        return false;
    }

    private void setupPermissions() {
      Plugin permissionsPlugin = this.getServer().getPluginManager().getPlugin("Permissions");

      if (this.permissionHandler == null) {
          if (permissionsPlugin != null) {
              this.permissionHandler = ((Permissions) permissionsPlugin).getHandler();
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
