package com.glisco.deathlog.storage;

import com.glisco.deathlog.client.DeathInfo;
import com.google.common.collect.ImmutableList;
import io.wispforest.endec.SerializationContext;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.owo.serialization.format.nbt.NbtDeserializer;
import io.wispforest.owo.serialization.format.nbt.NbtSerializer;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// TODO format conversion?
public abstract class BaseDeathLogStorage implements DeathLogStorage {

    private static final int FORMAT_REVISION = 3;
    public static final Logger LOGGER = LogManager.getLogger();

    private boolean errored = false;

    private final DynamicRegistryManager registries;

    protected BaseDeathLogStorage(DynamicRegistryManager registries) {
        this.registries = registries;
    }

    protected CompletableFuture<List<DeathInfo>> load(DynamicRegistryManager registries, File file) {
        final var future = new CompletableFuture<List<DeathInfo>>();
        Util.getIoWorkerExecutor().service().submit(() -> {
            if (errored) {
                LOGGER.warn("Attempted to load DeathLog database even though disk operations are disabled");
                future.complete(null);
                return;
            }

            NbtCompound deathNbt;

            if (file.exists()) {
                try {
                    deathNbt = NbtIo.read(file.toPath());

                    if (deathNbt.getInt("FormatRevision") != FORMAT_REVISION) {
                        raiseError("Incompatible format");

                        LOGGER.error("Incompatible DeathLog database format detected. Database not loaded and further disk operations disabled");

                        future.complete(null);
                        return;
                    }
                } catch (IOException e) {
                    raiseError("Disk access failed");

                    e.printStackTrace();
                    LOGGER.error("Failed to load DeathLog database, further disk operations have been disabled");

                    future.completeExceptionally(e);
                    return;
                }
            } else {
                deathNbt = new NbtCompound();
            }

            final var list = new ArrayList<DeathInfo>();
            final NbtList infoList = deathNbt.getList("Deaths", NbtElement.COMPOUND_TYPE);
            try {
                for (int i = 0; i < infoList.size(); i++) {
                    list.add(DeathInfo.ENDEC.decodeFully(
                            SerializationContext.attributes(RegistriesAttribute.of(registries)),
                            NbtDeserializer::of,
                            infoList.getCompound(i)
                    ));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to decode death info", e);
            }

            future.complete(list);
        });

        return future;
    }

    protected void save(DynamicRegistryManager registries, File file, List<DeathInfo> listIn) {
        final var list = ImmutableList.copyOf(listIn);
        Util.getIoWorkerExecutor().service().submit(() -> {
            if (errored) {
                LOGGER.warn("Attempted to save DeathLog database even though disk operations are disabled");
                return;
            }

            final NbtCompound deathNbt = new NbtCompound();
            final NbtList infoList = new NbtList();

            list.forEach(deathInfo -> infoList.add(DeathInfo.ENDEC.encodeFully(
                    SerializationContext.attributes(RegistriesAttribute.of(registries)),
                    NbtSerializer::of,
                    deathInfo)
            ));

            deathNbt.put("Deaths", infoList);
            deathNbt.putInt("FormatRevision", FORMAT_REVISION);

            try {
                NbtIo.write(deathNbt, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error("Failed to save DeathLog database");
            }
        });
    }

    @Override
    public boolean isErrored() {
        return errored;
    }

    protected void raiseError(String error) {
        this.errored = true;
    }

    @Override
    public DynamicRegistryManager registries() {
        return this.registries;
    }
}
