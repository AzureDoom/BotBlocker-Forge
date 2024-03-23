package com.azuredoom.botblocker;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.UserBanListEntry;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BotBlocker implements ModInitializer {
    public static Yaml yaml;

    @Override
    public void onInitialize() {
        CommonClass.init();
        Constants.LOGGER.info("BotBlocker mod is loading!");
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(options);

        Constants.configPath = new File(Constants.PATH_CONFIG).toPath();
        Constants.playersPath = new File(Constants.PATH_PLAYERS).toPath();

        loadConfig();
        loadPlayers();

        Constants.timeLimit = 20; // Default to 20 seconds

        Constants.LOGGER.info(String.format(Constants.MESSAGE_MOD_LOADED, Constants.timeLimit));

        // onPlayerJoin: Add the player to the joinTimes map
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!Constants.pluginEnabled) return;

            UUID playerId = handler.player.getUUID();
            if (CommonClass.isPlayerExempt(playerId)) return;

            if (!Constants.joinTimes.containsKey(playerId)) {
                Constants.joinTimes.put(playerId, System.currentTimeMillis());
            }
        });

        // onPlayerQuit: Check if the player is a bot and ban it if it is
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (!Constants.pluginEnabled) return;

            UUID playerId = handler.player.getUUID();
            if (Constants.joinTimes.containsKey(playerId)) {
                long joinTime = Constants.joinTimes.get(playerId);
                long timeConnected = (System.currentTimeMillis() - joinTime) / 1000;

                if (timeConnected < Constants.timeLimit) {
                    String playerName = handler.player.getGameProfile().getName();
                    // Ban the player
                    Constants.players.put(playerId.toString(), false);
                    server.getPlayerList().getBans().add(
                            new UserBanListEntry(handler.player.getGameProfile(), null, playerName, null,
                                    Constants.MESSAGE_DISCONNECT));
                    handler.player.connection.disconnect(Component.literal(Constants.MESSAGE_DISCONNECT));
                    System.out.printf((Constants.MESSAGE_DISCONNECT_CONSOLE) + "%n", playerName, Constants.timeLimit);
                } else {
                    // Add the player to players.yml if it is not banned
                    Constants.players.put(playerId.toString(), true);
                    Constants.joinTimes.remove(playerId);
                }
                savePlayers();
            }
        });

        // onCommand: Register the commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // enable command
            dispatcher.register(Commands.literal(Constants.MOD_ID)
                    .then(Commands.literal(Constants.COMMAND_ENABLE)
                            .executes(context -> {
                                Constants.pluginEnabled = true;
                                context.getSource().sendSuccess(Component.literal(Constants.MESSAGE_ENABLED),
                                        true);
                                Constants.config.put("enabled", true);
                                saveConfig();
                                return 1;
                            })));

            // disable command
            dispatcher.register(Commands.literal(Constants.MOD_ID)
                    .then(Commands.literal(Constants.COMMAND_DISABLE)
                            .executes(context -> {
                                Constants.pluginEnabled = false;
                                context.getSource().sendSuccess(Component.literal(Constants.MESSAGE_DISABLED),
                                        true);
                                Constants.config.put("enabled", false);
                                saveConfig();
                                return 1;
                            })));

            // setTimeLimit command
            dispatcher.register(Commands.literal(Constants.MOD_ID)
                    .then(Commands.literal(Constants.COMMAND_SET_TIME_LIMIT)
                            .then(Commands.argument("seconds", IntegerArgumentType.integer())
                                    .executes(context -> {
                                        Constants.timeLimit = IntegerArgumentType.getInteger(context, "seconds");
                                        context.getSource().sendSuccess(Component.literal(
                                                        String.format(Constants.MESSAGE_TIME_LIMIT, Constants.timeLimit)),
                                                true);
                                        Constants.config.put("time-limit", Constants.timeLimit);
                                        saveConfig();
                                        return 1;
                                    }))));
        });
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

    public static void loadConfig() {
        Constants.config = loadConfigs(Constants.configPath);
        if (Constants.config == null) {
            Constants.config = new HashMap<>();
            Constants.config.putIfAbsent("enabled", true);
            Constants.config.putIfAbsent("time-limit", 20);
            saveConfig();
        }
    }

    public static void loadPlayers() {
        Constants.players = loadConfigs(Constants.playersPath);
    }

    public static void savePlayers() {
        saveConfigs(Constants.players, Constants.playersPath);
    }

    public static void saveConfig() {
        saveConfigs(Constants.config, Constants.configPath);
    }
}
