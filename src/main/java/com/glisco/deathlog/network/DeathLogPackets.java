package com.glisco.deathlog.network;

import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.client.DeathInfo;
import com.glisco.deathlog.client.DeathLogClient;
import com.glisco.deathlog.client.gui.DeathLogScreen;
import com.glisco.deathlog.server.DeathLogServer;
import com.glisco.deathlog.storage.BaseDeathLogStorage;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.format.edm.EdmDeserializer;
import io.wispforest.endec.format.edm.EdmSerializer;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.serialization.RegistriesAttribute;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.UUID;

public class DeathLogPackets {

    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(Identifier.of("deathlog", "channel"));

    public static void init() {
        CHANNEL.builder().register(DeathInfo.ENDEC, DeathInfo.class);

        CHANNEL.registerClientboundDeferred(OpenScreen.class, OpenScreen.ENDEC);
        CHANNEL.registerClientboundDeferred(DeathInfoData.class);

        CHANNEL.registerServerbound(InfoRequest.class, (message, access) -> {
            if (!DeathLogServer.hasPermission(access.player(), "deathlog.view")) {
                BaseDeathLogStorage.LOGGER.warn("Received unauthorized info request from {}", access.player().getName().getString());
                return;
            }

            CHANNEL.serverHandle(access.player()).send(new DeathInfoData(
                    message.index,
                    DeathLogCommon.getStorage().getDeathInfoList(message.profile).get(message.index)
            ));
        });

        CHANNEL.registerServerbound(RestoreRequest.class, (message, access) -> {
            if (!DeathLogServer.hasPermission(access.player(), "deathlog.restore")) {
                BaseDeathLogStorage.LOGGER.warn("Received unauthorized restore packet from {}", access.player().getName().getString());
                return;
            }

            var targetPlayer = access.runtime().getPlayerManager().getPlayer(message.profile);
            if (targetPlayer == null) {
                BaseDeathLogStorage.LOGGER.warn("Received restore packet for invalid player");
                return;
            }

            final var infoList = DeathLogCommon.getStorage().getDeathInfoList(message.profile);
            if (message.index > infoList.size() - 1) {
                BaseDeathLogStorage.LOGGER.warn("Received restore packet with invalid index from '{}'", access.player().getName().getString());
                return;
            }

            var info = DeathInfo.ENDEC.decodeFully(
                    SerializationContext.attributes(RegistriesAttribute.of(access.runtime().getRegistryManager())),
                    EdmDeserializer::of,
                    DeathInfo.ENDEC.encodeFully(
                            SerializationContext.attributes(RegistriesAttribute.of(DeathLogCommon.getStorage().registries())),
                            EdmSerializer::of,
                            infoList.get(message.index)
                    )
            );

            info.restore(targetPlayer);
        });

        CHANNEL.registerServerbound(DeletionRequest.class, (message, access) -> {
            if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) return;

            if (!DeathLogServer.hasPermission(access.player(), "deathlog.delete")) {
                BaseDeathLogStorage.LOGGER.warn("Received unauthorized delete packet from {}", access.player().getName().getString());
                return;
            }

            DeathLogServer.getStorage().delete(DeathLogServer.getStorage().getDeathInfoList(message.profile).get(message.index), message.profile);
        });
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        CHANNEL.registerClientbound(OpenScreen.class, OpenScreen.ENDEC, (message, access) -> {
            var storage = new RemoteDeathLogStorage(message.partialInfos, message.profile);
            DeathLogClient.openScreen(storage, message.canRestore);
        });

        CHANNEL.registerClientbound(DeathInfoData.class, (message, access) -> {
            if (!(access.runtime().currentScreen instanceof DeathLogScreen screen)) {
                BaseDeathLogStorage.LOGGER.warn("Received invalid death info packet");
                return;
            }

            screen.updateInfo(message.info, message.infoIdx);
        });
    }

    public record DeletionRequest(UUID profile, int index) {}

    public record RestoreRequest(UUID profile, int index) {}

    public record InfoRequest(UUID profile, int index) {}

    public record DeathInfoData(int infoIdx, DeathInfo info) {}

    public record OpenScreen(UUID profile, boolean canRestore, List<DeathInfo> partialInfos) {
        public static final StructEndec<OpenScreen> ENDEC = StructEndecBuilder.of(
                BuiltInEndecs.UUID.fieldOf("profile", OpenScreen::profile),
                Endec.BOOLEAN.fieldOf("can_restore", OpenScreen::canRestore),
                DeathInfo.PARTIAL_ENDEC.listOf().fieldOf("partial_infos", OpenScreen::partialInfos),
                OpenScreen::new
        );
    }
}
