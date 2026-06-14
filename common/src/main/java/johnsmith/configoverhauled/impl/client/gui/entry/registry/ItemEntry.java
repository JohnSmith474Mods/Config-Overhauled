package johnsmith.configoverhauled.impl.client.gui.entry.registry;

import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.client.gui.screen.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

public class ItemEntry extends RegistryEntry<Item> {
    public ItemEntry(Property<Item> property, ConfigScreen parentScreen, Minecraft minecraft, Runnable onValueChanged) {
        super(
                property,
                parentScreen,
                minecraft,
                onValueChanged,
                BuiltInRegistries.ITEM,
                item -> {
                    Item boundItem = BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).get().value();
                    return SafeIconHelper.getSafeIcon(boundItem);
                },
                item -> Component.translatable(item.getDescriptionId())
        );
    }
}