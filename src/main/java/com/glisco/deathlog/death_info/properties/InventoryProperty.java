package com.glisco.deathlog.death_info.properties;

import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import com.glisco.deathlog.death_info.RestorableDeathInfoProperty;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

public class InventoryProperty implements RestorableDeathInfoProperty {

    private static final StructEndec<InventoryProperty> ENDEC = StructEndecBuilder.of(
            defaulted(MinecraftEndecs.ITEM_STACK.listOf()).fieldOf("items", s -> s.playerItems),
            defaulted(MinecraftEndecs.ITEM_STACK.listOf()).fieldOf("armor", s -> s.playerArmor),
            InventoryProperty::new
    );

    private final DefaultedList<ItemStack> playerItems;
    private final DefaultedList<ItemStack> playerArmor;

    public InventoryProperty(DefaultedList<ItemStack> playerItems, DefaultedList<ItemStack> playerArmor) {
        this.playerItems = playerItems;
        this.playerArmor = playerArmor;
    }

    public InventoryProperty(PlayerInventory playerInventory) {
        this.playerItems = DefaultedList.ofSize(37, ItemStack.EMPTY);
        this.playerArmor = DefaultedList.ofSize(4, ItemStack.EMPTY);

        copy(playerInventory.armor, playerArmor);
        copy(playerInventory.main, playerItems);

        playerItems.set(36, playerInventory.offHand.get(0).copy());
    }

    @Override
    public DeathInfoPropertyType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public Text formatted() {
        return null;
    }

    @Override
    public String toSearchableString() {
        StringBuilder builder = new StringBuilder();

        playerItems.forEach(stack -> builder.append(stack.getName().getString()));
        playerArmor.forEach(stack -> builder.append(stack.getName().getString()));

        return builder.toString();
    }

    @Override
    public void restore(ServerPlayerEntity player) {
        final var inventory = player.getInventory();
        inventory.clear();

        copy(playerArmor, inventory.armor);
        copy(playerItems, inventory.main, 36);

        inventory.offHand.set(0, playerItems.get(36));
    }

    public DefaultedList<ItemStack> getPlayerArmor() {
        return playerArmor;
    }

    public DefaultedList<ItemStack> getPlayerItems() {
        return playerItems;
    }

    private static void copy(DefaultedList<ItemStack> list, DefaultedList<ItemStack> other) {
        copy(list, other, list.size());
    }

    private static void copy(DefaultedList<ItemStack> list, DefaultedList<ItemStack> other, int maxItems) {
        for (int i = 0; i < maxItems; i++) other.set(i, list.get(i).copy());
    }

    private static <T> Endec<DefaultedList<T>> defaulted(Endec<List<T>> endec) {
        return endec.xmap(ts -> {
                    var defaulted = DefaultedList.<T>of();
                    defaulted.addAll(ts);
                    return defaulted;
                },
                defaulted -> defaulted
        );
    }

    public static class Type extends DeathInfoPropertyType<InventoryProperty> {

        public static final Type INSTANCE = new Type();

        private Type() {
            super("deathlog.deathinfoproperty.inventory", Identifier.of("deathlog", "inventory"));
        }

        @Override
        public boolean displayedInInfoView() {
            return false;
        }

        @Override
        public StructEndec<InventoryProperty> endec() {
            return InventoryProperty.ENDEC;
        }
    }
}
