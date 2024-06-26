package com.azuredoom.botblocker.neo;

import com.azuredoom.botblocker.CommonClass;
import com.azuredoom.botblocker.Constants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Mod(Constants.MOD_ID)
public class BotBlocker {
    public static Yaml yaml;

    public BotBlocker(IEventBus eventBus) {
        CommonClass.init();
        var options = new DumperOptions();

        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(options);
        Constants.configPath = new File(Constants.PATH_CONFIG).toPath();
        Constants.playersPath = new File(Constants.PATH_PLAYERS).toPath();
        loadConfig();
        loadPlayers();
        Constants.timeLimit = 20;
    }

    @SubscribeEvent
    public static void onJoin(final EntityJoinLevelEvent event) {
        if (!Constants.pluginEnabled) return;
        var playerId = event.getEntity().getUUID();
        if (CommonClass.isPlayerExempt(playerId)) return;

        if (!Constants.joinTimes.containsKey(playerId)) {
            Constants.joinTimes.put(playerId, System.currentTimeMillis());
        }
    }

    @SubscribeEvent
    public static void onLeave(final EntityLeaveLevelEvent event) {
        if (!Constants.pluginEnabled) return;

        var playerId = event.getEntity().getUUID();
        if (Constants.joinTimes.containsKey(playerId) && event.getEntity() instanceof ServerPlayer player) {
            var joinTime = Constants.joinTimes.get(playerId);
            var timeConnected = (System.currentTimeMillis() - joinTime) / 1000;

            if (timeConnected < Constants.timeLimit) {
                var playerName = player.getScoreboardName();
                // Ban the player
                Constants.players.put(playerId.toString(), false);
                event.getLevel().getServer().getPlayerList().getBans().add(
                        new UserBanListEntry(player.getGameProfile(), null, playerName, null,
                                Constants.MESSAGE_DISCONNECT));
                player.connection.disconnect(Component.literal(Constants.MESSAGE_DISCONNECT));
                System.out.printf((Constants.MESSAGE_DISCONNECT_CONSOLE) + "%n", playerName, Constants.timeLimit);
            } else {
                // Add the player to players.yml if it is not banned
                Constants.players.put(playerId.toString(), true);
                Constants.joinTimes.remove(playerId);
            }
            BotBlocker.savePlayers();
        }
    }

    @SubscribeEvent
    public static void registerCommands(final RegisterCommandsEvent event) {
        // enable command
        event.getDispatcher().register(
                Commands.literal(Constants.MOD_ID).then(Commands.literal(Constants.COMMAND_ENABLE).executes(context -> {
                    Constants.pluginEnabled = true;
                    context.getSource().sendSuccess(() -> Component.literal(Constants.MESSAGE_ENABLED), true);
                    Constants.config.put("enabled", true);
                    BotBlocker.saveConfig();
                    return 1;
                })));
        // disable command
        event.getDispatcher().register(Commands.literal(Constants.MOD_ID).then(
                Commands.literal(Constants.COMMAND_DISABLE).executes(context -> {
                    Constants.pluginEnabled = false;
                    context.getSource().sendSuccess(() -> Component.literal(Constants.MESSAGE_DISABLED), true);
                    Constants.config.put("enabled", false);
                    BotBlocker.saveConfig();
                    return 1;
                })));

        // setTimeLimit command
        event.getDispatcher().register(Commands.literal(Constants.MOD_ID).then(
                Commands.literal(Constants.COMMAND_SET_TIME_LIMIT).then(
                        Commands.argument("seconds", IntegerArgumentType.integer()).executes(context -> {
                            Constants.timeLimit = IntegerArgumentType.getInteger(context, "seconds");
                            context.getSource().sendSuccess(() -> Component.literal(
                                    String.format(Constants.MESSAGE_TIME_LIMIT, Constants.timeLimit)), true);
                            Constants.config.put("time-limit", Constants.timeLimit);
                            BotBlocker.saveConfig();
                            return 1;
                        }))));
    }

    public static Map<String, Object> loadConfigs(Path path) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new HashMap<>();
        }

        try (InputStream input = new FileInputStream(path.toFile())) {
            return yaml.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public static void saveConfigs(Map<String, Object> data, Path path) {
        try (Writer writer = new FileWriter(path.toFile())) {
            yaml.dump(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadPlayers() {
        Constants.players = loadConfigs(Constants.playersPath);
    }

    public static void savePlayers() {
        saveConfigs(Constants.players, Constants.playersPath);
    }

    public static void loadConfig() {
        Constants.config = loadConfigs(Constants.configPath);
        if (Constants.config == null) {
            Constants.config = new HashMap<>();
            Constants.config.putIfAbsent("enabled", true);
            Constants.config.putIfAbsent("time-limit", 20);
            saveConfig();
        }
    }

    public static void saveConfig() {
        saveConfigs(Constants.config, Constants.configPath);
    }
}