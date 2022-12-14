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

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FreeSleeping extends JavaPlugin {
    public static FreeSleeping inst;
    private final Set<UUID> sleepPlayers = new HashSet<>();

    public FreeSleeping() {
        inst = this;
    }

    @Override
    public void onEnable() {
        Config.init();

        Bukkit.getPluginManager().registerEvents(new Listener() {

            @EventHandler
            public void onBedEnter(PlayerBedEnterEvent e) {
                if (!e.isCancelled() && nightNow()) {
                    Player player = e.getPlayer();

                    sleepPlayers.add(player.getUniqueId());
                    printTexts(TextType.SLEEP, player);

                    skipNight();
                }
            }

            @EventHandler
            public void onBedLeave(PlayerBedLeaveEvent e) {
                if (!e.isCancelled()) {
                    Player player = e.getPlayer();
                    UUID uuid = player.getUniqueId();

                    if (sleepPlayers.contains(uuid)) {
                        sleepPlayers.remove(uuid);

                        if (nightNow())
                            printTexts(TextType.WAKE, player);
                    }
                }
            }

            @EventHandler
            public void onPlayerQuit(PlayerQuitEvent e) {
                Player player = e.getPlayer();
                UUID uuid = player.getUniqueId();

                if (sleepPlayers.contains(uuid)) {
                    sleepPlayers.remove(uuid);

                    if (nightNow()) {
                        printTexts(TextType.WAKE, player);
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

    private boolean nightNow() {
        return Bukkit.getWorlds().get(0).getTime() >= 12000L;
    }

    private void skipNight() {
        int sleeping = sleepPlayers.size();
        int needed = calculateNeeded();

        if (sleeping >= needed) {
            World world = Bukkit.getWorlds().get(0);
            world.setTime(1000L);

            if (world.hasStorm()) {
                world.setWeatherDuration(0);
                world.setThunderDuration(0);
                world.setThundering(false);
            }

            printTexts(TextType.MORNING, null);
            sleepPlayers.clear();
        }
    }

    private int calculateNeeded() {
        int players = getTotalPlayers();

        if (players > 0)
            return Math.round(players / 100f * (float) Config.getDouble("percent"));

        return 1;
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

    private void printTexts(TextType type, @Nullable Player player) {
        String typePath = "alerts." + type + ".";

        String sleeping = String.valueOf(sleepPlayers.size());
        String needed = String.valueOf(calculateNeeded());

        for (TextPosition position : TextPosition.values()) {
            String positionPath = typePath + position;

            if (Config.getBoolean(positionPath + ".enabled")) {
                String message = Config.getMessage(positionPath + ".text")
                        .replace("%sleeping", sleeping).replace("%needed", needed);

                if (player != null)
                    message = message.replace("%player", player.getDisplayName());

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
