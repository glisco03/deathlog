package com.glisco.deathlog.death_info;

import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.death_info.properties.MissingDeathInfoProperty;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public abstract class DeathInfoPropertyType<P extends DeathInfoProperty> {

    public static final Endec<DeathInfoPropertyType<?>> ENDEC = MinecraftEndecs.IDENTIFIER.xmap(
            identifier -> DeathLogCommon.PROPERTY_TYPES.getOptionalValue(identifier).orElse(new MissingDeathInfoProperty.Type(identifier)),
            DeathInfoPropertyType::getId
    );

    private final String translationKey;
    private final Identifier id;

    public DeathInfoPropertyType(String translationKey, Identifier id) {
        this.translationKey = translationKey;
        this.id = id;
    }

    public MutableText getName() {
        return Text.translatable(translationKey);
    }

    public static Text decorateName(MutableText name) {
        return name.formatted(Formatting.BLUE);
    }

    public Identifier getId() {
        return id;
    }

    public abstract boolean displayedInInfoView();

    public abstract StructEndec<P> endec();
}
