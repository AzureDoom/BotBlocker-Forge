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

@Mod.EventBusSubscriber(modid = BotBlockerMod.MODID, value = Dist.DEDICATED_SERVER)
public class BotBlockerEvents {
    @SubscribeEvent
    public static void onJoin(final EntityJoinLevelEvent event) {
        if (!BotBlockerMod.pluginEnabled) return;
        var playerId = event.getEntity().getUUID();
        if (BotBlockerMod.isPlayerExempt(playerId)) return;

        if (!BotBlockerMod.joinTimes.containsKey(playerId)) {
            BotBlockerMod.joinTimes.put(playerId, System.currentTimeMillis());
        }
    }

    @SubscribeEvent
    public static void onLeave(final EntityLeaveLevelEvent event) {
        if (!BotBlockerMod.pluginEnabled) return;

        var playerId = event.getEntity().getUUID();
        if (BotBlockerMod.joinTimes.containsKey(playerId) && event.getEntity() instanceof ServerPlayer player) {
            var joinTime = BotBlockerMod.joinTimes.get(playerId);
            var timeConnected = (System.currentTimeMillis() - joinTime) / 1000;

            if (timeConnected < BotBlockerMod.timeLimit) {
                var playerName = player.getScoreboardName();
                // Ban the player
                BotBlockerMod.players.put(playerId.toString(), false);
                event.getLevel().getServer().getPlayerList().getBans().add(
                        new UserBanListEntry(player.getGameProfile(), null, playerName, null,
                                BotBlockerMod.MESSAGE_DISCONNECT));
                player.connection.disconnect(Component.literal(BotBlockerMod.MESSAGE_DISCONNECT));
                System.out.println(
                        String.format(BotBlockerMod.MESSAGE_DISCONNECT_CONSOLE, playerName, BotBlockerMod.timeLimit));
            } else {
                // Add the player to players.yml if it is not banned
                BotBlockerMod.players.put(playerId.toString(), true);
                BotBlockerMod.joinTimes.remove(playerId);
            }
            BotBlockerMod.savePlayers();
        }
    }


    @SubscribeEvent
    public static void registerCommands(final RegisterCommandsEvent event) {
        // enable command
        event.getDispatcher().register(Commands.literal(BotBlockerMod.MODID).then(
                Commands.literal(BotBlockerMod.COMMAND_ENABLE).executes(context -> {
                    BotBlockerMod.pluginEnabled = true;
                    context.getSource().sendSystemMessage(Component.nullToEmpty(BotBlockerMod.MESSAGE_ENABLED));
                    BotBlockerMod.config.put("enabled", true);
                    BotBlockerMod.saveConfig();
                    return 1;
                })));
        // disable command
        event.getDispatcher().register(Commands.literal(BotBlockerMod.MODID).then(
                Commands.literal(BotBlockerMod.COMMAND_DISABLE).executes(context -> {
                    BotBlockerMod.pluginEnabled = false;
                    context.getSource().sendSystemMessage(Component.nullToEmpty(BotBlockerMod.MESSAGE_DISABLED));
                    BotBlockerMod.config.put("enabled", false);
                    BotBlockerMod.saveConfig();
                    return 1;
                })));

        // setTimeLimit command
        event.getDispatcher().register(Commands.literal(BotBlockerMod.MODID).then(
                Commands.literal(BotBlockerMod.COMMAND_SET_TIME_LIMIT).then(
                        Commands.argument("seconds", IntegerArgumentType.integer()).executes(context -> {
                            BotBlockerMod.timeLimit = IntegerArgumentType.getInteger(context, "seconds");
                            context.getSource().sendSystemMessage(Component.nullToEmpty(
                                    String.format(BotBlockerMod.MESSAGE_TIME_LIMIT, BotBlockerMod.timeLimit)));
                            BotBlockerMod.config.put("time-limit", BotBlockerMod.timeLimit);
                            BotBlockerMod.saveConfig();
                            return 1;
                        }))));
    }
}
