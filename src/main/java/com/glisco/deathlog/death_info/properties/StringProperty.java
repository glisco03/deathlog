package com.glisco.deathlog.death_info.properties;

import com.glisco.deathlog.death_info.DeathInfoProperty;
import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class StringProperty implements DeathInfoProperty {

    private static final StructEndec<StringProperty> ENDEC = StructEndecBuilder.of(
            Endec.STRING.fieldOf("translation_key", s -> s.translationKey),
            Endec.STRING.fieldOf("data", s -> s.data),
            StringProperty::new
    );

    private final String translationKey;
    private final String data;

    public StringProperty(String translationKey, String data) {
        this.translationKey = translationKey;
        this.data = data;
    }

    @Override
    public DeathInfoPropertyType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public Text formatted() {
        return Text.literal(data);
    }

    @Override
    public String toSearchableString() {
        return data;
    }

    @Override
    public Text getName() {
        return DeathInfoPropertyType.decorateName(Text.translatable(translationKey));
    }

    public static class Type extends DeathInfoPropertyType<StringProperty> {

        public static final Type INSTANCE = new Type();

        private Type() {
            super("deathlog.deathinfoproperty.string", Identifier.of("deathlog", "string"));
        }

        @Override
        public boolean displayedInInfoView() {
            return true;
        }

        @Override
        public StructEndec<StringProperty> endec() {
            return StringProperty.ENDEC;
        }
    }
}
