package com.glisco.deathlog.storage;

import com.glisco.deathlog.client.DeathInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface DeathLogStorage {

    List<DeathInfo> getDeathInfoList(@Nullable UUID profile);

    void delete(DeathInfo info, @Nullable UUID profile);

    void store(Text deathMessage, PlayerEntity player);

    void restore(int index, @Nullable UUID profile);

    boolean isErrored();

    DynamicRegistryManager registries();
}
