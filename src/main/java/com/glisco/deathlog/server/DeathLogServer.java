package com.glisco.deathlog.server;

import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.client.DeathInfo;
import com.glisco.deathlog.network.DeathLogPackets;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class DeathLogServer implements DedicatedServerModInitializer {

    private static final DynamicCommandExceptionType INVALID_INDEX = new DynamicCommandExceptionType(o -> Text.literal("No DeathInfo found for index " + o));
    private static final DynamicCommandExceptionType NO_PLAYER_FOR_PROFILE = new DynamicCommandExceptionType(o -> Text.literal("Player " + ((GameProfile) o).getName() + " is not online"));
    private static final SimpleCommandExceptionType NO_DEATHS = new SimpleCommandExceptionType(Text.literal("No DeathInfo found"));

    private static ServerDeathLogStorage storage;

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            storage = new ServerDeathLogStorage();
            DeathLogCommon.setStorage(storage);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("deathlog").then(literal("list").requires(hasPermission("deathlog.list"))
                            .then(createProfileArgument().executes(context -> executeList(context, null))
                                    .then(argument("search_term", StringArgumentType.string())
                                            .executes(context -> executeList(context, StringArgumentType.getString(context, "search_term"))))))
                    .then(literal("view").requires(hasPermission("deathlog.view")).then(createProfileArgument().executes(context -> {
                        var player = context.getSource().getPlayer();
                        var profileId = getProfile(context).getId();

                        DeathLogPackets.CHANNEL.serverHandle(player).send(new DeathLogPackets.OpenScreen(
                                profileId,
                                player.getServer().getPlayerManager().getPlayer(profileId) != null,
                                DeathLogServer.getStorage().getDeathInfoList(profileId)
                        ));
                        return 0;
                    }))).then(literal("restore").requires(hasPermission("deathlog.restore")).then(createProfileArgument().then(argument("index", IntegerArgumentType.integer()).executes(context -> {
                        int index = IntegerArgumentType.getInteger(context, "index");
                        return executeRestore(context, index);
                    })).then(literal("latest").executes(DeathLogServer::executeRestoreLatest)))));
        });
    }

    private int executeList(CommandContext<ServerCommandSource> context, @Nullable String filter) throws CommandSyntaxException {
        var profile = getProfile(context);

        var deathInfoList = DeathLogServer.getStorage().getDeathInfoList(profile.getId());
        if (filter != null) deathInfoList = deathInfoList.stream().filter(info -> info.createSearchString().contains(filter.toLowerCase())).toList();

        final var infoListSize = deathInfoList.size();
        for (int i = 0; i < infoListSize; i++) {
            DeathInfo deathInfo = deathInfoList.get(i);
            var leftText = deathInfo.getLeftColumnText().iterator();
            var rightText = deathInfo.getRightColumnText().iterator();

            int idx = i;

            context.getSource().sendFeedback(() -> Text.literal(""), false);
            context.getSource().sendFeedback(() -> Text.literal("§7-- §aBegin §bDeath Info Entry [" + idx + "]§7--"), false);
            while (leftText.hasNext()) {
                context.getSource().sendFeedback(() -> ((MutableText) leftText.next()).append(Text.literal(": ")).append(((MutableText) rightText.next()).formatted(Formatting.WHITE)), false);
            }
            context.getSource().sendFeedback(() -> Text.literal("§7-- §cEnd §bDeath Info Entry [" + idx + "]§7--"), false);
        }

        if (infoListSize > 0) context.getSource().sendFeedback(() -> Text.literal(""), false);
        context.getSource().sendFeedback(() -> Text.literal("Queried §b" + infoListSize + "§r death info entries for player ").append("§b" + profile.getName()), false);

        return infoListSize;
    }

    private static Predicate<ServerCommandSource> hasPermission(String node) {
        return DeathLogCommon.usePermissions() ? Permissions.require(node, 4) : serverCommandSource -> serverCommandSource.hasPermissionLevel(4);
    }

    public static boolean hasPermission(ServerPlayerEntity player, String node) {
        return DeathLogCommon.usePermissions() ? Permissions.check(player, node, 4) : player.hasPermissionLevel(4);
    }

    private static int executeRestoreLatest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        restore(context, deathInfos -> deathInfos.size() - 1, index -> NO_DEATHS.create());
        return 0;
    }

    private static int executeRestore(CommandContext<ServerCommandSource> context, int index) throws CommandSyntaxException {
        restore(context, deathInfos -> index, INVALID_INDEX::create);
        return 0;
    }

    private static void restore(CommandContext<ServerCommandSource> context, Function<List<DeathInfo>, Integer> indexProvider, Function<Integer, CommandSyntaxException> exceptionProvider) throws CommandSyntaxException {
        final var targetProfile = getProfile(context);
        final var deathInfoList = DeathLogServer.getStorage().getDeathInfoList(targetProfile.getId());

        final var targetPlayer = context.getSource().getServer().getPlayerManager().getPlayer(targetProfile.getId());
        if (targetPlayer == null) throw NO_PLAYER_FOR_PROFILE.create(targetProfile);

        final int index = indexProvider.apply(deathInfoList);
        if (deathInfoList.isEmpty() || index > deathInfoList.size() - 1) throw exceptionProvider.apply(index);

        deathInfoList.get(index).restore(targetPlayer);
    }

    private static GameProfile getProfile(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var profileArgument = GameProfileArgumentType.getProfileArgument(context, "player");
        return profileArgument.iterator().next();
    }

    private static RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> createProfileArgument() {
        return argument("player", GameProfileArgumentType.gameProfile()).suggests((context, builder) -> {
            PlayerManager playerManager = context.getSource().getServer().getPlayerManager();
            return CommandSource.suggestMatching(playerManager.getPlayerList().stream().map((player) -> player.getGameProfile().getName()), builder);
        });
    }

    public static ServerDeathLogStorage getStorage() {
        return storage;
    }
}
