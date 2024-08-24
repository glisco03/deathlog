package com.glisco.deathlog.death_info.properties;

import com.glisco.deathlog.death_info.DeathInfoProperty;
import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import io.wispforest.endec.*;
import io.wispforest.owo.serialization.format.nbt.NbtEndec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MissingDeathInfoProperty implements DeathInfoProperty {

    private final Type type;
    private final NbtCompound data;

    public MissingDeathInfoProperty(Type type, NbtCompound data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public DeathInfoPropertyType<?> getType() {
        return this.type;
    }

    @Override
    public Text formatted() {
        return Text.empty();
    }

    @Override
    public String toSearchableString() {
        return null;
    }

    public static class Type extends DeathInfoPropertyType<MissingDeathInfoProperty> {

        private final StructEndec<MissingDeathInfoProperty> endec = new StructEndec<>() {
            @Override
            public void encodeStruct(SerializationContext ctx, Serializer<?> serializer, Serializer.Struct struct, MissingDeathInfoProperty value) {
                if (serializer instanceof SelfDescribedSerializer<?>) {
                    for (String key : value.data.getKeys()) {
                        struct.field(key, ctx, NbtEndec.ELEMENT, value.data.get(key));
                    }
                } else {
                    NbtEndec.COMPOUND.encode(ctx, serializer, value.data);
                }
            }

            @Override
            public MissingDeathInfoProperty decodeStruct(SerializationContext ctx, Deserializer<?> deserializer, Deserializer.Struct struct) {
                if (deserializer instanceof SelfDescribedDeserializer<?>) {
                    var map = NbtEndec.ELEMENT.mapOf().decode(ctx, deserializer);

                    var compound = new NbtCompound();
                    map.forEach(compound::put);

                    return new MissingDeathInfoProperty(Type.this, compound);
                } else {
                    return new MissingDeathInfoProperty(Type.this, NbtEndec.COMPOUND.decode(ctx, deserializer));
                }
            }
        };

        public Type(Identifier id) {
            super(null, id);
        }

        @Override
        public boolean displayedInInfoView() {
            return false;
        }

        @Override
        public StructEndec<MissingDeathInfoProperty> endec() {
            return this.endec;
        }
    }

}
