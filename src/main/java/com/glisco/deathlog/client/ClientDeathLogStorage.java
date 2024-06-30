package com.glisco.deathlog.client;

import com.glisco.deathlog.client.gui.DeathLogToast;
import com.glisco.deathlog.death_info.SpecialPropertyProvider;
import com.glisco.deathlog.death_info.properties.*;
import com.glisco.deathlog.mixin.MinecraftServerAccessor;
import com.glisco.deathlog.network.DeathLogPackets;
import com.glisco.deathlog.storage.BaseDeathLogStorage;
import com.glisco.deathlog.storage.DeathInfoCreatedCallback;
import com.glisco.deathlog.storage.DirectDeathLogStorage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ClientDeathLogStorage extends BaseDeathLogStorage implements DirectDeathLogStorage {

    private final List<DeathInfo> deathInfos;
    private final File deathLogFile;

    public ClientDeathLogStorage(MinecraftClient client) {
        super(client.world.getRegistryManager());
        var worldSuffix = DigestUtils.sha1Hex(
                client.isInSingleplayer()
                        ? ((MinecraftServerAccessor) client.getServer()).deathlog_getSession().getDirectoryName()
                        : client.getCurrentServerEntry().name
        ).substring(0, 10);

        this.deathLogFile = FabricLoader.getInstance().getGameDir().resolve("deathlog").resolve("deaths_" + worldSuffix + ".dat").toFile();
        this.deathInfos = load(client.world.getRegistryManager(), deathLogFile).join();

        var deathLogDir = FabricLoader.getInstance().getGameDir().resolve("deathlog").toAbsolutePath();

        if (!Files.exists(deathLogDir) && !deathLogDir.toFile().mkdir()) {
            raiseError("Failed to create directory");
            LOGGER.error("Failed to create DeathLog storage directory, further disk operations have been disabled");
        }
    }

    @Override
    public List<DeathInfo> getDeathInfoList(@Nullable UUID profile) {
        return deathInfos;
    }

    @Override
    public void delete(DeathInfo info, @Nullable UUID profile) {
        deathInfos.remove(info);
        save(MinecraftClient.getInstance().world.getRegistryManager(), deathLogFile, deathInfos);
    }

    @Override
    public void store(Text deathMessage, PlayerEntity player) {
        final DeathInfo deathInfo = new DeathInfo();
        final MinecraftClient client = MinecraftClient.getInstance();

        deathInfo.setProperty(DeathInfo.INVENTORY_KEY, new InventoryProperty(player.getInventory()));

        deathInfo.setProperty(DeathInfo.COORDINATES_KEY, new CoordinatesProperty(player.getBlockPos()));
        deathInfo.setProperty(DeathInfo.DIMENSION_KEY, new StringProperty("deathlog.deathinfoproperty.dimension", player.getWorld().getRegistryKey().getValue().toString()));

        if (client.isInSingleplayer()) {
            deathInfo.setProperty(DeathInfo.LOCATION_KEY, new LocationProperty(((MinecraftServerAccessor) client.getServer()).deathlog_getSession().getDirectoryName(), false));
        } else {
            deathInfo.setProperty(DeathInfo.LOCATION_KEY, new LocationProperty(client.getCurrentServerEntry().name, true));
        }

        deathInfo.setProperty(DeathInfo.SCORE_KEY, new ScoreProperty(player.getScore(), player.experienceLevel, player.experienceProgress, player.totalExperience));
        deathInfo.setProperty(DeathInfo.DEATH_MESSAGE_KEY, new StringProperty("deathlog.deathinfoproperty.death_message", deathMessage.getString()));
        deathInfo.setProperty(DeathInfo.TIME_OF_DEATH_KEY, new StringProperty("deathlog.deathinfoproperty.time_of_death", new Date().toString()));

        SpecialPropertyProvider.apply(deathInfo, player);
        DeathInfoCreatedCallback.EVENT.invoker().event(deathInfo);

        deathInfos.add(deathInfo);
        save(MinecraftClient.getInstance().world.getRegistryManager(), deathLogFile, deathInfos);
    }

    @Override
    public void restore(int index, @Nullable UUID profile) {
        DeathLogPackets.CHANNEL.clientHandle().send(new DeathLogPackets.RestoreRequest(
                MinecraftClient.getInstance().player.getUuid(),
                index
        ));
    }

    @Override
    protected void raiseError(String error) {
        super.raiseError(error);

        MinecraftClient.getInstance().getToastManager().add(new DeathLogToast(SystemToast.Type.PACK_LOAD_FAILURE, Text.of("DeathLog Database Error"), Text.of(error)));
        MinecraftClient.getInstance().getToastManager().add(new DeathLogToast(SystemToast.Type.PACK_LOAD_FAILURE, Text.of("DeathLog Problem"), Text.of("Check your log for details")));
    }
}
