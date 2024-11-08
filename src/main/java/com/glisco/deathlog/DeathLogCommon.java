package com.glisco.deathlog;

import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import com.glisco.deathlog.death_info.DeathInfoPropertyTypes;
import com.glisco.deathlog.network.DeathLogPackets;
import com.glisco.deathlog.storage.DeathLogStorage;
import io.wispforest.owo.registration.reflect.AutoRegistryContainer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

public class DeathLogCommon implements ModInitializer {

    public static final Registry<DeathInfoPropertyType<?>> PROPERTY_TYPES = FabricRegistryBuilder
        .createSimple(RegistryKey.<DeathInfoPropertyType<?>>ofRegistry(Identifier.of("deathlog", "property_type")))
        .buildAndRegister();

    private static DeathLogStorage currentStorage = null;
    private static boolean usePermissions;

    @Override
    public void onInitialize() {
        AutoRegistryContainer.register(DeathInfoPropertyTypes.class, "deathlog", false);

//        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
//            SpecialPropertyProvider.register(TrinketComponentProperty::apply);
//            Registry.register(PROPERTY_TYPES, Identifier.of("deathlog", "trinkets"), TrinketComponentProperty.Type.INSTANCE);
//        }

        usePermissions = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

        DeathLogPackets.init();
    }

    public static boolean usePermissions() {
        return usePermissions;
    }

    @ApiStatus.Internal
    public static void setStorage(DeathLogStorage storage) {
        DeathLogCommon.currentStorage = storage;
    }

    public static DeathLogStorage getStorage() {
        return currentStorage;
    }
}
