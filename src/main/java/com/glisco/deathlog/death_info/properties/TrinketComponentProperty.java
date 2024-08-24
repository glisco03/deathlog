package com.glisco.deathlog.death_info.properties;

import com.glisco.deathlog.client.DeathInfo;
import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import com.glisco.deathlog.death_info.RestorableDeathInfoProperty;
import dev.emi.trinkets.api.TrinketsApi;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.owo.serialization.format.nbt.NbtEndec;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;

public class TrinketComponentProperty implements RestorableDeathInfoProperty {

    public static final StructEndec<TrinketComponentProperty> ENDEC = StructEndecBuilder.of(
            NbtEndec.COMPOUND.xmapWithContext(
                    (ctx, nbt) -> new NbtWithRegistries(nbt, ctx.getAttributeValue(RegistriesAttribute.REGISTRIES).registryManager()),
                    (ctx, nbt) -> nbt.nbt
            ).fieldOf("component_nbt", s -> new NbtWithRegistries(s.componentNbt, null)),
            TrinketComponentProperty::new
    );

    private final NbtCompound componentNbt;
    private final DefaultedList<ItemStack> trinkets;

    private TrinketComponentProperty(NbtWithRegistries nbt) {
        this.componentNbt = nbt.nbt;

        this.trinkets = DefaultedList.ofSize(nbt.nbt.getList("Items", NbtElement.COMPOUND_TYPE).size(), ItemStack.EMPTY);
        Inventories.readNbt(nbt.nbt, this.trinkets, nbt.registries);
    }

    @Override
    public DeathInfoPropertyType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public Text formatted() {
        return Text.translatable("deathlog.deathinfoproperty.trinket_component.value", trinkets.size());
    }

    @Override
    public String toSearchableString() {
        StringBuilder builder = new StringBuilder();
        trinkets.forEach(stack -> builder.append(stack.getName().getString()));
        return builder.toString();
    }

    @Override
    public void restore(ServerPlayerEntity player) {
        TrinketsApi.getTrinketComponent(player).get().readFromNbt(this.componentNbt, player.getRegistryManager());
    }

    public static void apply(DeathInfo info, PlayerEntity player) {
        final var trinketComponent = TrinketsApi.getTrinketComponent(player).get();

        var nbt = new NbtCompound();
        trinketComponent.writeToNbt(nbt, player.getRegistryManager());

        info.setProperty("trinket_component", new TrinketComponentProperty(new NbtWithRegistries(nbt, player.getRegistryManager())));
    }

    private record NbtWithRegistries(NbtCompound nbt, @Nullable RegistryWrapper.WrapperLookup registries) {}

    public static class Type extends DeathInfoPropertyType<TrinketComponentProperty> {

        public static final Type INSTANCE = new Type();

        private Type() {
            super("deathlog.deathinfoproperty.trinket_component", Identifier.of("deathlog", "trinkets"));
        }

        @Override
        public boolean displayedInInfoView() {
            return true;
        }

        @Override
        public StructEndec<TrinketComponentProperty> endec() {
            return TrinketComponentProperty.ENDEC;
        }
    }
}
