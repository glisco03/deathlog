package com.glisco.deathlog.death_info;

import io.wispforest.endec.Endec;
import net.minecraft.text.Text;

public interface DeathInfoProperty {

    Endec<DeathInfoProperty> ENDEC = Endec.dispatchedStruct(DeathInfoPropertyType::endec, DeathInfoProperty::getType, DeathInfoPropertyType.ENDEC);

    default Text getName() {
        return DeathInfoPropertyType.decorateName(getType().getName());
    }

    DeathInfoPropertyType<?> getType();

    Text formatted();

    String toSearchableString();
}
