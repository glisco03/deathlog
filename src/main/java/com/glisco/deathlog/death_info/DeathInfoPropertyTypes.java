package com.glisco.deathlog.death_info;

import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.death_info.properties.*;
import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.minecraft.registry.Registry;

public class DeathInfoPropertyTypes implements AutoRegistryContainer<DeathInfoPropertyType<?>> {

    public static final DeathInfoPropertyType<?> INVENTORY = InventoryProperty.Type.INSTANCE;
    public static final DeathInfoPropertyType<?> COORDINATES = CoordinatesProperty.Type.INSTANCE;
    public static final DeathInfoPropertyType<?> LOCATION = LocationProperty.Type.INSTANCE;
    public static final DeathInfoPropertyType<?> SCORE = ScoreProperty.Type.INSTANCE;
    public static final DeathInfoPropertyType<?> STRING = StringProperty.Type.INSTANCE;

    @Override
    public Registry<DeathInfoPropertyType<?>> getRegistry() {
        return DeathLogCommon.PROPERTY_TYPES;
    }

    @Override
    public Class<DeathInfoPropertyType<?>> getTargetFieldType() {
        return AutoRegistryContainer.conform(DeathInfoPropertyType.class);
    }
}
