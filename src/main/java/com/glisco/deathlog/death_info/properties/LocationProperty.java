package com.glisco.deathlog.death_info.properties;

import com.glisco.deathlog.death_info.DeathInfoProperty;
import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class LocationProperty implements DeathInfoProperty {

    public static final StructEndec<LocationProperty> ENDEC = StructEndecBuilder.of(
            Endec.STRING.fieldOf("location", s -> s.location),
            Endec.BOOLEAN.fieldOf("multiplayer", s -> s.multiplayer),
            LocationProperty::new
    );

    private final String location;
    private final boolean multiplayer;

    public LocationProperty(String location, boolean multiplayer) {
        this.location = location;
        this.multiplayer = multiplayer;
    }

    @Override
    public DeathInfoPropertyType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public Text formatted() {
        return Text.translatable(
                "deathlog.deathinfoproperty.location.value", location,
                multiplayer
                        ? Text.translatable("deathlog.deathinfoproperty.location.multiplayer")
                        : Text.translatable("deathlog.deathinfoproperty.location.singleplayer")
        );
    }

    @Override
    public String toSearchableString() {
        return location;
    }

    public static class Type extends DeathInfoPropertyType<LocationProperty> {

        public static final Type INSTANCE = new Type();

        private Type() {
            super("deathlog.deathinfoproperty.location", Identifier.of("deathlog", "location"));
        }

        @Override
        public boolean displayedInInfoView() {
            return true;
        }

        @Override
        public StructEndec<LocationProperty> endec() {
            return LocationProperty.ENDEC;
        }
    }
}
