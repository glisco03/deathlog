package com.glisco.deathlog.client;

import com.glisco.deathlog.death_info.DeathInfoProperty;
import com.glisco.deathlog.death_info.RestorableDeathInfoProperty;
import com.glisco.deathlog.death_info.properties.InventoryProperty;
import com.glisco.deathlog.death_info.properties.TrinketComponentProperty;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;

import java.util.*;
import java.util.function.Consumer;

public class DeathInfo {

    public static final Endec<DeathInfo> ENDEC = StructEndecBuilder.of(
            sequencedMapEndec(DeathInfoProperty.ENDEC).fieldOf("properties", info -> info.properties),
            DeathInfo::new
    );

    public static final Endec<DeathInfo> PARTIAL_ENDEC = StructEndecBuilder.of(
            sequencedMapEndec(DeathInfoProperty.ENDEC).xmap(properties -> {
                SequencedMap<String, DeathInfoProperty> copy = new LinkedHashMap<>();
                properties.forEach((key, property) -> {
                    if (property instanceof InventoryProperty || property instanceof TrinketComponentProperty) return;
                    copy.put(key, property);
                });
                return copy;
            }, map -> map).fieldOf("properties", info -> info.properties),
            DeathInfo::new
    );

    public static final String COORDINATES_KEY = "coordinates";
    public static final String DIMENSION_KEY = "dimension";
    public static final String LOCATION_KEY = "location";
    public static final String SCORE_KEY = "score";
    public static final String DEATH_MESSAGE_KEY = "death_message";
    public static final String TIME_OF_DEATH_KEY = "time_of_death";
    public static final String INVENTORY_KEY = "inventory";

    private final SequencedMap<String, DeathInfoProperty> properties;

    public DeathInfo() {
        this.properties = new LinkedHashMap<>();
    }

    public DeathInfo(SequencedMap<String, DeathInfoProperty> properties) {
        this.properties = properties;
    }

    public void restore(ServerPlayerEntity player) {
        properties.values().stream().filter(property -> property instanceof RestorableDeathInfoProperty).forEach(property -> ((RestorableDeathInfoProperty) property).restore(player));
    }

    public void setProperty(String property, DeathInfoProperty value) {
        this.properties.put(property, value);
    }

    public Optional<DeathInfoProperty> getProperty(String property) {
        return Optional.ofNullable(properties.get(property));
    }

    public boolean isPartial() {
        return getProperty(INVENTORY_KEY).isEmpty();
    }

    public Text getListName() {
        DeathInfoProperty property = getProperty(TIME_OF_DEATH_KEY).orElse(null);
        return property == null ? Text.translatable("text.deathlog.info.time_missing") : property.formatted();
    }

    public Text getTitle() {
        DeathInfoProperty property = getProperty(DEATH_MESSAGE_KEY).orElse(null);
        return property == null ? Text.translatable("text.deathlog.info.death_message_missing") : property.formatted();
    }

    public List<Text> getLeftColumnText() {
        final var texts = new ArrayList<Text>();
        iterateDisplayProperties(property -> texts.add(property.getName()));
        return texts;
    }

    public List<Text> getRightColumnText() {
        final var texts = new ArrayList<Text>();
        iterateDisplayProperties(property -> texts.add(property.formatted()));
        return texts;
    }

    public String createSearchString() {
        final StringBuilder builder = new StringBuilder();
        properties.forEach((s, property) -> builder.append(property.toSearchableString()));
        return builder.toString().toLowerCase();
    }

    private void iterateDisplayProperties(Consumer<DeathInfoProperty> callback) {
        properties.forEach((s, property) -> {
            if (!property.getType().displayedInInfoView()) return;

            callback.accept(property);
        });
    }

    public DefaultedList<ItemStack> getPlayerArmor() {
        var propertyOptional = getProperty(INVENTORY_KEY);
        if (propertyOptional.isEmpty()) return DefaultedList.of();
        return ((InventoryProperty) propertyOptional.get()).getPlayerArmor();
    }

    public DefaultedList<ItemStack> getPlayerItems() {
        var propertyOptional = getProperty(INVENTORY_KEY);
        if (propertyOptional.isEmpty()) return DefaultedList.of();
        return ((InventoryProperty) propertyOptional.get()).getPlayerItems();
    }

    private static <T> Endec<SequencedMap<String, T>> sequencedMapEndec(Endec<T> in) {
        return Endec.of((ctx, serializer, map) -> {
            try (var mapState = serializer.map(ctx, in, map.size())) {
                map.forEach(mapState::entry);
            }
        }, (ctx, deserializer) -> {
            var mapState = deserializer.map(ctx, in);

            var map = new LinkedHashMap<String, T>(mapState.estimatedSize());
            mapState.forEachRemaining(entry -> map.put(entry.getKey(), entry.getValue()));

            return map;
        });
    }
}
