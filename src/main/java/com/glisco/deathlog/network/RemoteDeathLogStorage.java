package com.glisco.deathlog.network;

import com.glisco.deathlog.client.DeathInfo;
import com.glisco.deathlog.storage.BaseDeathLogStorage;
import com.glisco.deathlog.storage.DirectDeathLogStorage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class RemoteDeathLogStorage extends BaseDeathLogStorage implements DirectDeathLogStorage {

    private final List<DeathInfo> deathInfoList;
    private final UUID profileId;

    public RemoteDeathLogStorage(List<DeathInfo> deathInfoList, UUID profileId) {
        super(MinecraftClient.getInstance().world.getRegistryManager());
        this.deathInfoList = deathInfoList;
        this.profileId = profileId;
    }

    @Override
    public List<DeathInfo> getDeathInfoList(@Nullable UUID profile) {
        return deathInfoList;
    }

    @Override
    public void delete(DeathInfo info, @Nullable UUID profile) {
        int index = deathInfoList.indexOf(info);
        DeathLogPackets.CHANNEL.clientHandle().send(new DeathLogPackets.DeletionRequest(profileId, index));
        deathInfoList.remove(info);
    }

    @Override
    public void store(Text deathMessage, PlayerEntity player) {
        //NO-OP
    }

    @Override
    public void restore(int index, @Nullable UUID profile) {
        DeathLogPackets.CHANNEL.clientHandle().send(new DeathLogPackets.RestoreRequest(profileId, index));
    }

    public void fetchCompleteInfo(DeathInfo info) {
        if (!info.isPartial()) return;

        var idx = this.getDeathInfoList().indexOf(info);
        DeathLogPackets.CHANNEL.clientHandle().send(new DeathLogPackets.InfoRequest(profileId, idx));
    }

}
