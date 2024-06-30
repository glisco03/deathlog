package com.glisco.deathlog.death_info.properties;

import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import com.glisco.deathlog.death_info.RestorableDeathInfoProperty;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ScoreProperty implements RestorableDeathInfoProperty {

    public static final StructEndec<ScoreProperty> ENDEC = StructEndecBuilder.of(
            Endec.INT.fieldOf("score", s -> s.score),
            Endec.INT.fieldOf("levels", s -> s.levels),
            Endec.FLOAT.fieldOf("progress", s -> s.progress),
            Endec.INT.fieldOf("xp", s -> s.xp),
            ScoreProperty::new
    );

    private final int score;
    private final int levels;
    private final float progress;

    private final int xp;

    public ScoreProperty(int score, int level, float progress, int xp) {
        this.score = score;
        this.levels = level;
        this.progress = progress;
        this.xp = xp;
    }

    @Override
    public DeathInfoPropertyType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public Text formatted() {
        return Text.translatable(
                "deathlog.deathinfoproperty.score.value",
                score, levels, xp
        );
    }

    @Override
    public String toSearchableString() {
        return xp + " " + levels;
    }

    @Override
    public void restore(ServerPlayerEntity player) {
        player.experienceProgress = progress;
        player.setExperienceLevel(levels);
    }

    public static class Type extends DeathInfoPropertyType<ScoreProperty> {

        public static final Type INSTANCE = new Type();

        private Type() {
            super("deathlog.deathinfoproperty.score", Identifier.of("deathlog", "score"));
        }

        @Override
        public boolean displayedInInfoView() {
            return true;
        }

        @Override
        public StructEndec<ScoreProperty> endec() {
            return ScoreProperty.ENDEC;
        }
    }
}
