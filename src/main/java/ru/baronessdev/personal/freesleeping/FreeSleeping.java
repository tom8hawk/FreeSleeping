package ru.baronessdev.personal.freesleeping;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FreeSleeping extends JavaPlugin {
    public static FreeSleeping inst;

    private final List<Player> sleepPlayers = new ArrayList<>();
    private float totalPercent;

    public FreeSleeping() {
        inst = this;
    }
    
    public void onEnable() {
        Config.init();

        Bukkit.getPluginManager().registerEvents(new Listener() {

            @EventHandler
            public void onBedEnter(PlayerBedEnterEvent event) {
                if (nightNow())
                    doSleep(event.getPlayer());
            }

            @EventHandler
            public void onBedLeave(PlayerBedLeaveEvent event) {
                Player player = event.getPlayer();

                if (sleepPlayers.contains(player)) {
                    sleepPlayers.remove(player);

                    if (nightNow())
                        printTexts(TextType.WAKE, Map.of("%player", player.getDisplayName()));
                }
            }

            @EventHandler
            public void onPlayerQuit(PlayerQuitEvent event) {
                Player player = event.getPlayer();

                if (sleepPlayers.contains(player)) {
                    sleepPlayers.remove(player);

                    if (nightNow()) {
                        printTexts(TextType.WAKE, Map.of("%player", player.getDisplayName()));
                        skipNight();
                    }
                }
            }
        }, this);

        CommandExecutor executor = (sender, command, label, args) -> {
            if (sender.hasPermission("freesleeping.reload") || sender.hasPermission("fs.reload")) {
                if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                    Config.init();
                    sender.sendMessage(Config.getMessage("messages.successfully-reload"));
                } else {
                    sender.sendMessage(Config.getMessage("messages.reload-command"));
                }
            } else {
                sender.sendMessage(Config.getMessage("messages.no-rights"));
            }

            return true;
        };

        getCommand("freesleeping").setExecutor(executor);
        getCommand("fs").setExecutor(executor);
    }

    public void onDisable() {

    }

    private boolean nightNow() {
        return Bukkit.getWorld("world").getTime() >= 12000L;
    }

    private void doSleep(Player player) {
        sleepPlayers.add(player);
        printTexts(TextType.SLEEP, Map.of("%player", player.getDisplayName()));

        skipNight();
    }

    private void skipNight() {
        String missed = calculateSleeping();

        if (totalPercent >= (float) Config.getDouble("percent")) {
            sleepPlayers.clear();

            World world = Bukkit.getWorld("world");
            world.setTime(1000L);

            if (world.hasStorm()) {
                world.setWeatherDuration(0);
                world.setThunderDuration(0);
                world.setThundering(false);
            }

            printTexts(TextType.MORNING, Map.of("%missed", missed));
        }
    }

    private String calculateSleeping() {
        int totalSleeping = sleepPlayers.size();
        int totalPlayers = getTotalPlayers();

        totalPercent = (float) totalSleeping / (float) totalPlayers * 100.0F;
        totalPercent = Math.round(totalPercent * 100.0F) / 100.0F;

        return totalSleeping + "/" + totalPlayers + " (" + totalPercent + "%)";
    }

    private int getTotalPlayers() {
        int totalPlayers = 0;
        int miningPlayers = 0;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getEnvironment().equals(World.Environment.NORMAL)) {
                
                if (player.getLocation().getY() < Config.getInt("groundLevel"))
                    miningPlayers++;
                
                totalPlayers++;
            }
        }
        
        return (Config.getBoolean("includeMiners")) ? totalPlayers : (totalPlayers - miningPlayers);
    }

    private void printTexts(TextType type, Map<String, String> replacements) {
        String typePath = "alerts." + type + ".";

        for (TextPosition position : TextPosition.values()) {
            String positionPath = typePath + position;

            if (Config.getBoolean(positionPath + ".enabled")) {
                String message = Config.getMessage(positionPath + ".text");

                for (Map.Entry<String, String> data : replacements.entrySet())
                    message = message.replace(data.getKey(), data.getValue());

                position.broadcastMessage(message);
            }
        }
    }

    private enum TextType {
        MORNING, SLEEP, WAKE
    }

    private enum TextPosition {
        ACTIONBAR {
            @Override
            public void broadcastMessage(String message) {
                BaseComponent text = new ComponentBuilder(message).create()[0];

                for (Player player : Bukkit.getOnlinePlayers()) {
                    Bukkit.getScheduler().runTaskLater(inst, ()
                            -> player.spigot().sendMessage(ChatMessageType.ACTION_BAR, text), 5L);
                }
            }
        },
        CHAT {
            @Override
            public void broadcastMessage(String message) {
                Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
            }
        };

        public abstract void broadcastMessage(String message);
    }
}
