package com.azuredoom.botblocker;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.DEDICATED_SERVER)
public class BotBlockerEvents {
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
        event.getDispatcher().register(Commands.literal(Constants.MOD_ID).then(
                Commands.literal(Constants.COMMAND_ENABLE).executes(context -> {
                    Constants.pluginEnabled = true;
                    context.getSource().sendSuccess(Component.literal(Constants.MESSAGE_ENABLED), true);
                    Constants.config.put("enabled", true);
                    BotBlocker.saveConfig();
                    return 1;
                })));
        // disable command
        event.getDispatcher().register(Commands.literal(Constants.MOD_ID).then(
                Commands.literal(Constants.COMMAND_DISABLE).executes(context -> {
                    Constants.pluginEnabled = false;
                    context.getSource().sendSuccess(Component.literal(Constants.MESSAGE_DISABLED), true);
                    Constants.config.put("enabled", false);
                    BotBlocker.saveConfig();
                    return 1;
                })));

        // setTimeLimit command
        event.getDispatcher().register(Commands.literal(Constants.MOD_ID).then(
                Commands.literal(Constants.COMMAND_SET_TIME_LIMIT).then(
                        Commands.argument("seconds", IntegerArgumentType.integer()).executes(context -> {
                            Constants.timeLimit = IntegerArgumentType.getInteger(context, "seconds");
                            context.getSource().sendSuccess(Component.literal(
                                    String.format(Constants.MESSAGE_TIME_LIMIT, Constants.timeLimit)), true);
                            Constants.config.put("time-limit", Constants.timeLimit);
                            BotBlocker.saveConfig();
                            return 1;
                        }))));
    }
}
