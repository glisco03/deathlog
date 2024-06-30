package com.glisco.deathlog.death_info.properties;

import com.glisco.deathlog.death_info.DeathInfoProperty;
import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class CoordinatesProperty implements DeathInfoProperty {

    private static final StructEndec<CoordinatesProperty> ENDEC = StructEndecBuilder.of(
            MinecraftEndecs.BLOCK_POS.fieldOf("coordinates", s -> s.coordinates),
            CoordinatesProperty::new
    );

    private final BlockPos coordinates;

    public CoordinatesProperty(BlockPos coordinates) {
        this.coordinates = coordinates;
    }

    @Override
    public DeathInfoPropertyType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public Text formatted() {
        return Text.translatable("deathlog.deathinfoproperty.coordinates.value", coordinates.getX(), coordinates.getY(), coordinates.getZ());
    }

    @Override
    public String toSearchableString() {
        return coordinates.getX() + " " + coordinates.getY() + " " + coordinates.getZ();
    }

    public static class Type extends DeathInfoPropertyType<CoordinatesProperty> {

        public static final Type INSTANCE = new Type();

        private Type() {
            super("deathlog.deathinfoproperty.coordinates", Identifier.of("deathlog", "coordinates"));
        }

        @Override
        public boolean displayedInInfoView() {
            return true;
        }

        @Override
        public StructEndec<CoordinatesProperty> endec() {
            return CoordinatesProperty.ENDEC;
        }
    }
}
